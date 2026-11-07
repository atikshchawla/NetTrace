package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
  primary = DarkPalette.wifi,
  onPrimary = DarkPalette.textPrimary,
  secondary = DarkPalette.mobile,
  onSecondary = DarkPalette.background,
  tertiary = DarkPalette.sim2,
  background = DarkPalette.background,
  onBackground = DarkPalette.textPrimary,
  surface = DarkPalette.surface,
  onSurface = DarkPalette.textPrimary,
  surfaceVariant = DarkPalette.raised,
  onSurfaceVariant = DarkPalette.textSecondary,
  outline = DarkPalette.hairline,
  error = DarkPalette.critical,
  onError = DarkPalette.textPrimary
)

private val LightColorScheme = lightColorScheme(
  primary = LightPalette.wifi,
  onPrimary = LightPalette.surface,
  secondary = LightPalette.mobile,
  onSecondary = LightPalette.surface,
  tertiary = LightPalette.sim2,
  background = LightPalette.background,
  onBackground = LightPalette.textPrimary,
  surface = LightPalette.surface,
  onSurface = LightPalette.textPrimary,
  surfaceVariant = LightPalette.raised,
  onSurfaceVariant = LightPalette.textSecondary,
  outline = LightPalette.hairline,
  error = LightPalette.critical,
  onError = LightPalette.surface
)

@Composable
fun NetTraceTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val palette = if (darkTheme) DarkPalette else LightPalette
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        window.statusBarColor = palette.background.toArgb()
        window.navigationBarColor = palette.background.toArgb()
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !darkTheme
        insetsController.isAppearanceLightNavigationBars = !darkTheme
      }
    }
  }

  CompositionLocalProvider(LocalNetTraceColors provides palette) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = NetTraceTypography,
      content = content
    )
  }
}
