package com.example.ui.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion

/**
 * Floating Apple-like bottom bar.
 * Layered raised surface, rounded 28dp, hairline border, spring color responses.
 */
@Composable
fun FloatingBottomNavBar(
  currentScreen: Screen,
  onScreenSelected: (Screen) -> Unit,
  modifier: Modifier = Modifier
) {
  val view = LocalView.current
  val navScreens = listOf(Screen.HOME, Screen.APPS, Screen.HISTORY, Screen.SETTINGS)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(28.dp))
        .background(NetTraceColors.Raised)
        .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(28.dp))
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .height(54.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically
    ) {
      navScreens.forEach { screen ->
        val isSelected = (screen == currentScreen)

        val tintColor by animateColorAsState(
          targetValue = if (isSelected) NetTraceColors.Wifi else NetTraceColors.TextSecondary,
          animationSpec = NetTraceMotion.springSpec(),
          label = "nav_item_tint"
        )

        Column(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              if (!isSelected) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onScreenSelected(screen)
              }
            },
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          screen.icon?.let { icon ->
            Icon(
              imageVector = icon,
              contentDescription = screen.title,
              tint = tintColor,
              modifier = Modifier.size(22.dp)
            )
          }
          Text(
            text = screen.title,
            color = tintColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
          )
        }
      }
    }
  }
}
