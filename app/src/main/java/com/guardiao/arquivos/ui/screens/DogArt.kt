package com.guardiao.arquivos.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.guardiao.arquivos.R
import kotlin.math.abs

/**
 * Cão farejando, usado enquanto a varredura roda.
 *
 * A cabeça balança devagar como quem fareja e três marcas de cheiro sobem do chão à frente do
 * nariz, em sequência. O desenho fica sobre um círculo tingido porque o cão é creme e
 * desapareceria direto sobre uma superfície clara.
 */
@Composable
fun SniffingDog(modifier: Modifier = Modifier, size: Dp = 168.dp) {
  val transition = rememberInfiniteTransition(label = "farejando")

  val bob by
    transition.animateFloat(
      initialValue = -4.5f,
      targetValue = 4.5f,
      animationSpec =
        infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "balanco",
    )

  val phase by
    transition.animateFloat(
      initialValue = 0f,
      targetValue = 3f,
      animationSpec = infiniteRepeatable(tween(2100, easing = LinearEasing), RepeatMode.Restart),
      label = "cheiro",
    )

  val scentColor = MaterialTheme.colorScheme.secondary
  val groundColor = MaterialTheme.colorScheme.outline

  BoxWithConstraints(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer),
    contentAlignment = Alignment.TopStart,
  ) {
    val box = maxWidth

    // O desenho do cão ocupa 67,5% da caixa, deslocado para a esquerda e para baixo, de modo que
    // o nariz encoste na linha do chão. As frações vêm da composição validada em 160 unidades.
    Image(
      painter = painterResource(R.drawable.ic_dog_sniffing),
      contentDescription = "Cão farejando os arquivos do aparelho",
      modifier =
        Modifier
          .size(box * 0.675f)
          .offset(x = box * 0.175f, y = box * 0.275f)
          .graphicsLayer {
            rotationZ = bob
            // Pivô no cangote, para o focinho descrever o arco e não a cabeça toda girar.
            transformOrigin = TransformOrigin(0.74f, 0.46f)
          },
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = this.size.width

      drawLine(
        color = groundColor,
        start = Offset(w * 0.15f, w * 0.70f),
        end = Offset(w * 0.8375f, w * 0.70f),
        strokeWidth = w * 0.022f,
        cap = StrokeCap.Round,
        alpha = 0.45f,
      )

      // Três marcas de cheiro subindo à frente do nariz, acendendo em sequência.
      val marks =
        listOf(
          Triple(0.2625f, 0.6125f, 0.031f),
          Triple(0.1940f, 0.5440f, 0.0225f),
          Triple(0.1500f, 0.4690f, 0.0163f),
        )
      marks.forEachIndexed { index, (fx, fy, fr) ->
        // Cada marca acende quando a fase passa pelo seu índice.
        val distance = abs(phase - index)
        val intensity = (1f - distance).coerceIn(0f, 1f)
        if (intensity > 0f) {
          drawCircle(
            color = scentColor,
            radius = w * fr * (0.75f + 0.25f * intensity),
            center = Offset(w * fx, w * fy),
            alpha = 0.25f + 0.6f * intensity,
          )
        }
      }
    }
  }
}

/** Marca do app: o cão de frente, o mesmo desenho do ícone, dentro de um círculo. */
@Composable
fun DogMark(modifier: Modifier = Modifier, size: Dp = 44.dp) {
  Box(
    modifier = modifier.size(size).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(R.drawable.ic_launcher_foreground),
      contentDescription = null,
      modifier = Modifier.size(size * 1.32f),
    )
  }
}
