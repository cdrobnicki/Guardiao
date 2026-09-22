package com.guardiao.arquivos.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.guardiao.arquivos.scanner.FileCategory
import com.guardiao.arquivos.scanner.RiskLevel
import java.text.DateFormat
import java.util.Date
import java.util.Locale

fun formatSize(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val units = arrayOf("KB", "MB", "GB", "TB")
  var value = bytes.toDouble() / 1024
  var unit = 0
  while (value >= 1024 && unit < units.lastIndex) {
    value /= 1024
    unit++
  }
  return String.format(Locale.getDefault(), if (value >= 100) "%.0f %s" else "%.1f %s", value, units[unit])
}

private val dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.SHORT, Locale("pt", "BR"))

fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

fun formatDuration(millis: Long): String {
  val seconds = millis / 1000
  return if (seconds < 60) "${seconds}s" else "${seconds / 60}min ${seconds % 60}s"
}

val FileCategory.icon: ImageVector
  get() =
    when (this) {
      FileCategory.IMAGE -> Icons.Filled.Image
      FileCategory.VIDEO -> Icons.Filled.Videocam
      FileCategory.AUDIO -> Icons.Filled.AudioFile
      FileCategory.PDF -> Icons.Filled.PictureAsPdf
      FileCategory.DOCUMENT -> Icons.Filled.Description
      FileCategory.SPREADSHEET -> Icons.Filled.TableChart
      FileCategory.PRESENTATION -> Icons.Filled.Slideshow
    }

@Composable
fun RiskLevel.color(): Color =
  when (this) {
    RiskLevel.HIGH -> Color(0xFFD32F2F)
    RiskLevel.MEDIUM -> Color(0xFFF57C00)
    RiskLevel.LOW -> Color(0xFFFBC02D)
    RiskLevel.NONE -> MaterialTheme.colorScheme.outline
  }
