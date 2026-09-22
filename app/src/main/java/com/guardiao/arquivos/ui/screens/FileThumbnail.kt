package com.guardiao.arquivos.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guardiao.arquivos.quarantine.QuarantineRecord
import com.guardiao.arquivos.scanner.FileCategory
import com.guardiao.arquivos.scanner.ScannedFile
import com.guardiao.arquivos.ui.icon
import java.io.File

/**
 * Categorias que ganham pré-visualização. Planilhas e apresentações ficam de fora: um despejo de
 * células ou de tópicos não ajuda a reconhecer o arquivo em miniatura.
 */
private val previewCategories =
  setOf(FileCategory.IMAGE, FileCategory.VIDEO, FileCategory.PDF, FileCategory.DOCUMENT)

/** Miniatura de um arquivo encontrado na varredura. */
@Composable
fun FileThumbnail(file: ScannedFile, size: Dp, modifier: Modifier = Modifier) {
  Thumbnail(
    model = remember(file.path) { File(file.path) },
    category = file.category,
    cacheKey = file.path,
    size = size,
    modifier = modifier,
  )
}

/** Miniatura de um arquivo em quarentena, que pode estar sob um caminho ou uma URI do SAF. */
@Composable
fun QuarantineThumbnail(record: QuarantineRecord, size: Dp, modifier: Modifier = Modifier) {
  val path = record.quarantinePath
  Thumbnail(
    model = remember(path) { if (path.startsWith("content://")) Uri.parse(path) else File(path) },
    category = remember(record.category) { runCatching { FileCategory.valueOf(record.category) }.getOrNull() },
    cacheKey = path,
    size = size,
    modifier = modifier,
  )
}

/**
 * Mostra uma pré-visualização para fotos, vídeos, PDFs e documentos de texto, e o ícone da
 * categoria para o restante.
 *
 * As miniaturas são decodificadas no tamanho em que serão exibidas e mantidas apenas em memória
 * (ver [com.guardiao.arquivos.GuardiaoApp]). Se a decodificação falhar — formato não suportado pela
 * versão do Android, arquivo corrompido, vídeo sem quadro legível, PDF protegido por senha ou
 * documento sem texto — o ícone da categoria entra no lugar.
 */
@Composable
private fun Thumbnail(
  model: Any,
  category: FileCategory?,
  cacheKey: String,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  val showsPreview = category != null && category in previewCategories
  var failed by remember(cacheKey) { mutableStateOf(false) }

  Box(
    modifier =
      modifier
        .size(size)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant),
    contentAlignment = Alignment.Center,
  ) {
    if (showsPreview && !failed) {
      val context = LocalContext.current
      val pixels = with(LocalDensity.current) { size.roundToPx() }
      AsyncImage(
        model =
          remember(cacheKey, pixels) {
            ImageRequest.Builder(context).data(model).size(pixels).crossfade(true).build()
          },
        contentDescription = category?.label,
        contentScale = ContentScale.Crop,
        onError = { failed = true },
        modifier = Modifier.fillMaxSize(),
      )
      if (category == FileCategory.VIDEO) {
        Icon(
          Icons.Filled.PlayCircle,
          contentDescription = null,
          tint = Color.White.copy(alpha = 0.85f),
          modifier = Modifier.size(size / 2.5f),
        )
      }
    } else {
      Icon(
        imageVector = category?.icon ?: Icons.Filled.Description,
        contentDescription = category?.label,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.size(size * 0.55f),
      )
    }
  }
}
