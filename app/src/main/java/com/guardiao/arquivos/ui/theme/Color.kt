package com.guardiao.arquivos.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta da marca, derivada do ícone: o azul-marinho profundo do fundo e o caramelo da mancha
// do cão. O azul carrega a ideia de segurança; o caramelo é o acento.

internal val Navy10 = Color(0xFF08243B)
internal val Navy20 = Color(0xFF0C2840)
internal val Navy40 = Color(0xFF1A4E78)
internal val Navy60 = Color(0xFF2A5B85)
internal val Navy80 = Color(0xFF9DC8EF)
internal val Navy90 = Color(0xFFD3E4F5)

internal val Tan10 = Color(0xFF2E1A06)
internal val Tan30 = Color(0xFF6B4520)
internal val Tan40 = Color(0xFF8A5A28)
internal val Tan80 = Color(0xFFE8BE8C)
internal val Tan90 = Color(0xFFF7E2C6)

internal val Teal10 = Color(0xFF052019)
internal val Teal30 = Color(0xFF1F544B)
internal val Teal40 = Color(0xFF2A6B60)
internal val Teal80 = Color(0xFF9FD4C8)
internal val Teal90 = Color(0xFFC9EBE3)

internal val LightBackground = Color(0xFFF7F9FB)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceVariant = Color(0xFFE3E9EF)
internal val LightOnSurface = Color(0xFF1A1D21)
internal val LightOnSurfaceVariant = Color(0xFF48525C)
internal val LightOutline = Color(0xFF78828C)
internal val LightOutlineVariant = Color(0xFFC8D0D8)

internal val DarkBackground = Color(0xFF0F1317)
internal val DarkSurface = Color(0xFF151A1F)
internal val DarkSurfaceVariant = Color(0xFF3B444D)
internal val DarkOnSurface = Color(0xFFE3E7EB)
internal val DarkOnSurfaceVariant = Color(0xFFC2CBD4)
internal val DarkOutline = Color(0xFF8C959E)

internal val ErrorLight = Color(0xFFB3261E)
internal val ErrorContainerLight = Color(0xFFF9DEDC)
internal val OnErrorContainerLight = Color(0xFF410E0B)
internal val ErrorDark = Color(0xFFF2B8B5)
internal val ErrorContainerDark = Color(0xFF8C1D18)
internal val OnErrorContainerDark = Color(0xFFF9DEDC)

/** Cores do desenho do cão, iguais às do ícone. Não variam com o tema: são a marca. */
object DogColors {
  val Cream = Color(0xFFF8F5EF)
  val Tan = Color(0xFFC98A4B)
  val TanDark = Color(0xFFA96C33)
  val Ink = Color(0xFF17181A)
}

/**
 * Cores de cada nível de risco.
 *
 * Cada nível tem uma cor de destaque (texto e ícone) e um par container/onContainer para as
 * etiquetas, com variantes clara e escura para o contraste ficar legível nos dois temas.
 */
data class RiskColors(
  val high: Color,
  val highContainer: Color,
  val onHighContainer: Color,
  val medium: Color,
  val mediumContainer: Color,
  val onMediumContainer: Color,
  val low: Color,
  val lowContainer: Color,
  val onLowContainer: Color,
  val none: Color,
  val noneContainer: Color,
  val onNoneContainer: Color,
)

internal val LightRiskColors =
  RiskColors(
    high = Color(0xFFC0261C),
    highContainer = Color(0xFFFBE1DE),
    onHighContainer = Color(0xFF5C0F0A),
    medium = Color(0xFFB05400),
    mediumContainer = Color(0xFFFDE8D0),
    onMediumContainer = Color(0xFF4A2300),
    low = Color(0xFF7D6000),
    lowContainer = Color(0xFFF8EFCB),
    onLowContainer = Color(0xFF3A2C00),
    none = Color(0xFF3F7A5E),
    noneContainer = Color(0xFFDDEDE4),
    onNoneContainer = Color(0xFF14382A),
  )

internal val DarkRiskColors =
  RiskColors(
    high = Color(0xFFFF9C92),
    highContainer = Color(0xFF6E1A14),
    onHighContainer = Color(0xFFFFE0DC),
    medium = Color(0xFFFFB877),
    mediumContainer = Color(0xFF63350A),
    onMediumContainer = Color(0xFFFFE6CC),
    low = Color(0xFFE8CE72),
    lowContainer = Color(0xFF4C3C00),
    onLowContainer = Color(0xFFF7EFD0),
    none = Color(0xFF8FCBAE),
    noneContainer = Color(0xFF214535),
    onNoneContainer = Color(0xFFDDEDE4),
  )
