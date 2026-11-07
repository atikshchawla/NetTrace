package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * NetTrace Color Palette contract supporting True Dark and Clean Light themes.
 */
data class NetTraceColorPalette(
  val background: Color,
  val surface: Color,
  val raised: Color,
  val hairline: Color,
  val textPrimary: Color,
  val textSecondary: Color,
  val textTertiary: Color,
  val wifi: Color,
  val mobile: Color,
  val warning: Color,
  val critical: Color,
  val sim2: Color,
  val isDark: Boolean
)

val DarkPalette = NetTraceColorPalette(
  background = Color(0xFF000000),
  surface = Color(0xFF0F0F12),
  raised = Color(0xFF1A1A1F),
  hairline = Color(0x14FFFFFF), // 8% white for clean surface borders
  textPrimary = Color(0xFFFFFFFF),
  textSecondary = Color(0xFFA1A1AA),
  textTertiary = Color(0xFF636366),
  wifi = Color(0xFF0A84FF),
  mobile = Color(0xFF30D158),
  warning = Color(0xFFFF9F0A),
  critical = Color(0xFFFF453A),
  sim2 = Color(0xFFBF5AF2),
  isDark = true
)

val LightPalette = NetTraceColorPalette(
  background = Color(0xFFF2F2F7), // iOS grouped background
  surface = Color(0xFFFFFFFF),    // Clean white card
  raised = Color(0xFFE5E5EA),     // Soft neutral raised surface
  hairline = Color(0x1F000000),   // 12% black border
  textPrimary = Color(0xFF000000),
  textSecondary = Color(0xFF6C6C70),
  textTertiary = Color(0xFF8E8E93),
  wifi = Color(0xFF007AFF),
  mobile = Color(0xFF34C759),
  warning = Color(0xFFFF9500),
  critical = Color(0xFFFF3B30),
  sim2 = Color(0xFFAF52DE),
  isDark = false
)

val LocalNetTraceColors = staticCompositionLocalOf { DarkPalette }

/**
 * Dynamic NetTraceColors resolving from LocalNetTraceColors.
 */
object NetTraceColors {
  val Background: Color
    @Composable get() = LocalNetTraceColors.current.background

  val Surface: Color
    @Composable get() = LocalNetTraceColors.current.surface

  val Raised: Color
    @Composable get() = LocalNetTraceColors.current.raised

  val Hairline: Color
    @Composable get() = LocalNetTraceColors.current.hairline

  val TextPrimary: Color
    @Composable get() = LocalNetTraceColors.current.textPrimary

  val TextSecondary: Color
    @Composable get() = LocalNetTraceColors.current.textSecondary

  val TextTertiary: Color
    @Composable get() = LocalNetTraceColors.current.textTertiary

  val Wifi: Color
    @Composable get() = LocalNetTraceColors.current.wifi

  val Mobile: Color
    @Composable get() = LocalNetTraceColors.current.mobile

  val Warning: Color
    @Composable get() = LocalNetTraceColors.current.warning

  val Critical: Color
    @Composable get() = LocalNetTraceColors.current.critical

  val Sim2: Color
    @Composable get() = LocalNetTraceColors.current.sim2

  @Composable
  fun colorForUsage(percent: Float): Color {
    val current = LocalNetTraceColors.current
    return when {
      percent >= 1.0f -> current.critical
      percent >= 0.8f -> current.warning
      else -> current.wifi
    }
  }
}
