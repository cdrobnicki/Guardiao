package com.guardiao.arquivos.ui.thumbnail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.pxOrElse
import com.guardiao.arquivos.scanner.FileCategory
import com.guardiao.arquivos.scanner.TextExtractor
import java.io.File
import java.io.IOException

/**
 * Miniatura de documentos de texto — Word (.doc/.docx), ODT, RTF, TXT e afins.
 *
 * O Android não tem renderizador para esses formatos, e nenhuma biblioteca capaz de diagramar um
 * .docx caberia aqui. Então a miniatura **não é a primeira página renderizada**: é uma página
 * desenhada na hora com as primeiras linhas do texto real do documento, extraídas pelo
 * [TextExtractor]. Isso não reproduz a formatação, mas mostra do que o arquivo trata, que é o que
 * serve para decidir se ele precisa de atenção.
 */
class DocumentPreviewDecoder(private val source: ImageSource, private val options: Options) : Decoder {

  override suspend fun decode(): DecodeResult {
    val path = source.fileOrNull() ?: throw IOException("Documento sem caminho local para ler.")
    val file = File(path.toString())
    val extension = file.name.substringAfterLast('.', "")

    val text =
      TextExtractor.extract(file, extension, maxChars = PREVIEW_CHARS)
        ?.replace(WHITESPACE, " ")
        ?.trim()
    if (text.isNullOrBlank()) throw IOException("Documento sem texto para a prévia.")

    val width = options.size.width.pxOrElse { FALLBACK_SIZE }
    val height = options.size.height.pxOrElse { FALLBACK_SIZE }
    val bitmap = drawPage(text, width, height)
    return DecodeResult(drawable = bitmap.toDrawable(options.context.resources), isSampled = true)
  }

  private fun drawPage(text: String, width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(Color.WHITE)

    val padding = width * 0.10f
    val textPaint =
      TextPaint().apply {
        isAntiAlias = true
        color = INK
        textSize = width * 0.095f
      }

    val lineHeight = textPaint.textSize * 1.35f
    val available = (width - 2 * padding).toInt().coerceAtLeast(1)
    val maxLines = ((height - 2 * padding) / lineHeight).toInt().coerceAtLeast(1)

    val layout =
      StaticLayout.Builder
        .obtain(text, 0, text.length, textPaint, available)
        .setEllipsize(TextUtils.TruncateAt.END)
        .setMaxLines(maxLines)
        .setLineSpacing(textPaint.textSize * 0.35f, 1f)
        .build()

    canvas.save()
    canvas.translate(padding, padding)
    layout.draw(canvas)
    canvas.restore()

    // Borda fina, para a página se destacar de um fundo claro.
    val border =
      Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = (width * 0.012f).coerceAtLeast(1f)
        color = BORDER
      }
    val inset = border.strokeWidth / 2f
    canvas.drawRect(inset, inset, width - inset, height - inset, border)

    return bitmap
  }

  class Factory : Decoder.Factory {
    override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
      val path = result.source.fileOrNull()?.toString() ?: return null
      val extension = path.substringAfterLast('.', "").lowercase()
      if (extension !in previewable) return null
      return DocumentPreviewDecoder(result.source, options)
    }
  }

  private companion object {
    const val FALLBACK_SIZE = 256

    /** O suficiente para preencher a miniatura; ler mais só desperdiçaria trabalho. */
    const val PREVIEW_CHARS = 600

    const val INK = 0xFF32383F.toInt()
    const val BORDER = 0x1F000000

    val WHITESPACE = Regex("\\s+")

    /**
     * Só os documentos de texto cujo conteúdo o extrator sabe ler. PDF fica de fora porque tem
     * renderizador próprio, e planilhas e apresentações também: uma lista solta de células não
     * diz muito como miniatura.
     */
    val previewable: Set<String> =
      FileCategory.DOCUMENT.extensions intersect TextExtractor.supportedExtensions
  }
}
