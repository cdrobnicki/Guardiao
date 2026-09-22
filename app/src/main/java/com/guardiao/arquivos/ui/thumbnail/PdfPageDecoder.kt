package com.guardiao.arquivos.ui.thumbnail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.pxOrElse
import java.io.File
import java.io.IOException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Miniatura de PDF: renderiza a primeira página de verdade, com o [PdfRenderer] do próprio
 * Android. Não depende de nenhuma biblioteca externa e não envia nada para fora do aparelho.
 *
 * O recorte é do topo da página, e não do centro, porque é onde ficam título, cabeçalho e
 * logotipo — o que de fato identifica o documento.
 */
class PdfPageDecoder(private val source: ImageSource, private val options: Options) : Decoder {

  override suspend fun decode(): DecodeResult =
    // O PdfRenderer não é seguro para uso concorrente; uma lista rolando dispara várias
    // decodificações ao mesmo tempo, então elas são serializadas aqui.
    renderLock.withLock {
      val path = source.fileOrNull() ?: throw IOException("PDF sem caminho local para renderizar.")
      val width = options.size.width.pxOrElse { FALLBACK_SIZE }
      val height = options.size.height.pxOrElse { FALLBACK_SIZE }
      val bitmap =
        renderFirstPage(File(path.toString()), width, height)
          ?: throw IOException("PDF sem páginas para renderizar.")
      DecodeResult(drawable = bitmap.toDrawable(options.context.resources), isSampled = true)
    }

  private fun renderFirstPage(file: File, targetWidth: Int, targetHeight: Int): Bitmap? {
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
      PdfRenderer(descriptor).use { renderer ->
        if (renderer.pageCount <= 0) return null
        renderer.openPage(0).use { page ->
          val scale = targetWidth.toFloat() / page.width.coerceAtLeast(1)
          val fullHeight = (page.height * scale).toInt().coerceIn(1, targetWidth * MAX_ASPECT)

          val full = Bitmap.createBitmap(targetWidth, fullHeight, Bitmap.Config.ARGB_8888)
          // Páginas de PDF são renderizadas com fundo transparente; sem isto, o texto preto
          // sairia sobre nada.
          Canvas(full).drawColor(Color.WHITE)
          page.render(full, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

          if (fullHeight <= targetHeight) return full
          val top = Bitmap.createBitmap(full, 0, 0, targetWidth, targetHeight)
          full.recycle()
          return top
        }
      }
    }
  }

  class Factory : Decoder.Factory {
    override fun create(result: SourceResult, options: Options, imageLoader: ImageLoader): Decoder? {
      if (!isPdf(result)) return null
      return PdfPageDecoder(result.source, options)
    }

    /**
     * Identifica o PDF pelo tipo MIME ou pela extensão do caminho, sem tocar no fluxo de bytes:
     * consumir o fluxo aqui atrapalharia a decodificação seguinte.
     */
    private fun isPdf(result: SourceResult): Boolean {
      if (result.mimeType == "application/pdf") return true
      val path = result.source.fileOrNull()?.toString() ?: return false
      return path.substringAfterLast('.', "").equals("pdf", ignoreCase = true)
    }
  }

  private companion object {
    const val FALLBACK_SIZE = 256

    /** Teto de proporção, para uma página muito longa não gerar um bitmap gigante. */
    const val MAX_ASPECT = 4

    val renderLock = Mutex()
  }
}
