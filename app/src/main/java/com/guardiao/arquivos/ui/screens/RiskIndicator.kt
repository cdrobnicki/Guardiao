package com.guardiao.arquivos.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardiao.arquivos.scanner.RiskLevel
import com.guardiao.arquivos.ui.accent
import com.guardiao.arquivos.ui.container
import com.guardiao.arquivos.ui.onContainer

/**
 * Pontuação de risco (0 a 100) como anel preenchido proporcionalmente, com o número no centro.
 *
 * O anel dá a leitura imediata — quanto mais cheio, mais indícios — e o número permite comparar
 * dois arquivos do mesmo nível.
 */
@Composable
fun RiskScoreRing(score: Int, level: RiskLevel, modifier: Modifier = Modifier, diameter: Dp = 46.dp) {
  val accent = level.accent()
  val track = MaterialTheme.colorScheme.outlineVariant
  val fraction = (score.coerceIn(0, 100)) / 100f

  Box(modifier = modifier.size(diameter), contentAlignment = Alignment.Center) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val stroke = size.minDimension * 0.11f
      val inset = stroke / 2f
      val arcSize = Size(size.width - stroke, size.height - stroke)
      drawArc(
        color = track,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(inset, inset),
        size = arcSize,
        style = Stroke(width = stroke, cap = StrokeCap.Round),
      )
      if (fraction > 0f) {
        drawArc(
          color = accent,
          startAngle = -90f,
          sweepAngle = 360f * fraction,
          useCenter = false,
          topLeft = Offset(inset, inset),
          size = arcSize,
          style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
      }
    }
    Text(
      text = score.toString(),
      color = accent,
      fontWeight = FontWeight.Bold,
      fontSize = (diameter.value * 0.33f).sp,
      style = MaterialTheme.typography.labelLarge,
    )
  }
}

/** Etiqueta com o nome do nível de risco. */
@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
  Box(
    modifier =
      modifier
        .background(level.container(), RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = level.label,
      style = MaterialTheme.typography.labelSmall,
      color = level.onContainer(),
      fontWeight = FontWeight.SemiBold,
    )
  }
}
