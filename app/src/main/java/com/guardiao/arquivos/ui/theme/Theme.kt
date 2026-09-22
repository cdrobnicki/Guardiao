package com.guardiao.arquivos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

private val LightColors =
  lightColorScheme(
    primary = Navy40,
    onPrimary = LightSurface,
    primaryContainer = Navy90,
    onPrimaryContainer = Navy10,
    inversePrimary = Navy80,
    secondary = Tan40,
    onSecondary = LightSurface,
    secondaryContainer = Tan90,
    onSecondaryContainer = Tan10,
    tertiary = Teal40,
    onTertiary = LightSurface,
    tertiaryContainer = Teal90,
    onTertiaryContainer = Teal10,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = ErrorLight,
    onError = LightSurface,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
  )

private val DarkColors =
  darkColorScheme(
    primary = Navy80,
    onPrimary = Navy10,
    primaryContainer = Navy60,
    onPrimaryContainer = Navy90,
    inversePrimary = Navy40,
    secondary = Tan80,
    onSecondary = Tan10,
    secondaryContainer = Tan30,
    onSecondaryContainer = Tan90,
    tertiary = Teal80,
    onTertiary = Teal10,
    tertiaryContainer = Teal30,
    onTertiaryContainer = Teal90,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkSurfaceVariant,
    error = ErrorDark,
    onError = Navy10,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
  )

/** Dá acesso às cores de risco de dentro de qualquer composable, como o MaterialTheme. */
val LocalRiskColors = staticCompositionLocalOf { LightRiskColors }

/**
 * Tema do app.
 *
 * A paleta é fixa, e não a cor dinâmica do sistema (Material You), de propósito: o azul e o
 * caramelo vêm do ícone e são a identidade do app — o mesmo verde-limão do papel de parede do
 * usuário tornaria as cores de risco ambíguas.
 */
@Composable
fun GuardiaoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
  val colorScheme = if (darkTheme) DarkColors else LightColors
  val riskColors = if (darkTheme) DarkRiskColors else LightRiskColors

  CompositionLocalProvider(LocalRiskColors provides riskColors) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
