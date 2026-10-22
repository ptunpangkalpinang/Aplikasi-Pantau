package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
  primary = PantauPrimaryBlue,
  onPrimary = PantauSurfaceLight,
  primaryContainer = PantauNavy,
  onPrimaryContainer = PantauSurfaceLight,
  secondary = PantauSkyBlue,
  onSecondary = PantauBgDark,
  tertiary = PantauSuccessGreen,
  background = PantauBgDark,
  onBackground = PantauTextPrimaryDark,
  surface = PantauSurfaceDark,
  onSurface = PantauTextPrimaryDark,
  surfaceVariant = PantauSurfaceDarkCard,
  onSurfaceVariant = PantauTextSecondaryDark,
  outline = PantauCardBorderDark,
  error = PantauDangerRed
)

private val LightColorScheme = lightColorScheme(
  primary = PantauPrimaryBlue,
  onPrimary = PantauSurfaceLight,
  primaryContainer = PantauNavy,
  onPrimaryContainer = PantauSurfaceLight,
  secondary = PantauLightBlue,
  onSecondary = PantauSurfaceLight,
  tertiary = PantauSuccessGreen,
  background = PantauBgLight,
  onBackground = PantauTextPrimary,
  surface = PantauSurfaceLight,
  onSurface = PantauTextPrimary,
  surfaceVariant = PantauBgLight,
  onSurfaceVariant = PantauTextSecondary,
  outline = PantauCardBorderLight,
  error = PantauDangerRed
)

@Composable
fun PantauTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = colorScheme.background.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
