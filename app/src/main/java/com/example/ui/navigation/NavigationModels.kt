package com.example.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.components.NetTraceIcons

/**
 * Screen destinations in NetTrace.
 */
enum class Screen(val title: String, val icon: ImageVector?) {
  HOME("Home", NetTraceIcons.Home),
  APPS("Apps", NetTraceIcons.Apps),
  HISTORY("History", NetTraceIcons.History),
  SETTINGS("Settings", NetTraceIcons.Gear)
}
