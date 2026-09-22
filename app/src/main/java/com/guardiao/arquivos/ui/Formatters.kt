package com.guardiao.arquivos.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.guardiao.arquivos.scanner.FileCategory
import com.guardiao.arquivos.scanner.RiskLevel
import com.guardiao.arquivos.ui.theme.LocalRiskColors
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val ptBr: Locale = Locale.forLanguageTag("pt-BR")

fun formatSize(bytes: Long): String {
  if (bytes < 1024) return "$bytes B"
  val units = arrayOf("KB", "MB", "GB", "TB")
  var value = bytes.toDouble() / 1024
  var unit = 0
  while (value >= 1024 && unit < units.lastIndex) {
    value /= 1024
    unit++
  }
  return String.format(ptBr, if (value >= 100) "%.0f %s" else "%.1f %s", value, units[unit])
}

private val dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.SHORT, ptBr)

fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

fun formatDuration(millis: Long): String {
  val seconds = millis / 1000
  return when {
    seconds < 60 -> "${seconds}s"
    seconds < 3600 -> "${seconds / 60}min ${seconds % 60}s"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}min"
  }
}

/** Separador de milhar em português, para as contagens de arquivos. */
fun formatCount(value: Int): String = String.format(ptBr, "%,d", value)

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

/** Cor de destaque do nível de risco: usada em texto, ícones e no anel de pontuação. */
@Composable
@ReadOnlyComposable
fun RiskLevel.accent(): Color =
  with(LocalRiskColors.current) {
    when (this@accent) {
      RiskLevel.HIGH -> high
      RiskLevel.MEDIUM -> medium
      RiskLevel.LOW -> low
      RiskLevel.NONE -> none
    }
  }

/** Fundo da etiqueta do nível de risco. */
@Composable
@ReadOnlyComposable
fun RiskLevel.container(): Color =
  with(LocalRiskColors.current) {
    when (this@container) {
      RiskLevel.HIGH -> highContainer
      RiskLevel.MEDIUM -> mediumContainer
      RiskLevel.LOW -> lowContainer
      RiskLevel.NONE -> noneContainer
    }
  }

/** Texto sobre o fundo da etiqueta. */
@Composable
@ReadOnlyComposable
fun RiskLevel.onContainer(): Color =
  with(LocalRiskColors.current) {
    when (this@onContainer) {
      RiskLevel.HIGH -> onHighContainer
      RiskLevel.MEDIUM -> onMediumContainer
      RiskLevel.LOW -> onLowContainer
      RiskLevel.NONE -> onNoneContainer
    }
  }

/** Frase curta que explica o que o nível significa, usada no detalhe do arquivo. */
val RiskLevel.explanation: String
  get() =
    when (this) {
      RiskLevel.HIGH -> "Indícios fortes de dados pessoais. Vale conferir e considerar a quarentena."
      RiskLevel.MEDIUM -> "Vários indícios de dados pessoais. Confira antes de decidir."
      RiskLevel.LOW -> "Indícios fracos. Provavelmente não há dado sensível, mas confira."
      RiskLevel.NONE -> "Nenhum indício encontrado no nome, na pasta, no conteúdo ou nos metadados."
    }
