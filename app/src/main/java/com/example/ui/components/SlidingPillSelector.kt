package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.model.UsageRange
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion

/**
 * Apple-style sliding pill segmented control.
 * Smoothly translates the active pill behind selected options with haptic tick.
 */
@Composable
fun SlidingPillSelector(
  selectedRange: UsageRange,
  onRangeSelected: (UsageRange) -> Unit,
  modifier: Modifier = Modifier
) {
  val view = LocalView.current
  val ranges = UsageRange.values()
  val selectedIndex = ranges.indexOf(selectedRange).coerceAtLeast(0)

  BoxWithConstraints(
    modifier = modifier
      .fillMaxWidth()
      .height(40.dp)
      .clip(RoundedCornerShape(20.dp))
      .background(NetTraceColors.Surface)
      .padding(3.dp)
  ) {
    val totalWidth = maxWidth
    val itemWidth = totalWidth / ranges.size

    val targetOffsetX = itemWidth * selectedIndex
    val animatedOffsetX by animateFloatAsState(
      targetValue = targetOffsetX.value,
      animationSpec = NetTraceMotion.springSpec(),
      label = "pill_offset_x"
    )

    // Animated sliding highlight pill
    Box(
      modifier = Modifier
        .offset(x = animatedOffsetX.dp)
        .width(itemWidth)
        .fillMaxHeight()
        .clip(RoundedCornerShape(17.dp))
        .background(NetTraceColors.Raised)
    )

    // Option labels
    Row(modifier = Modifier.fillMaxSize()) {
      ranges.forEach { range ->
        val isSelected = (range == selectedRange)
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              if (range != selectedRange) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onRangeSelected(range)
              }
            },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = range.label,
            color = if (isSelected) NetTraceColors.TextPrimary else NetTraceColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
          )
        }
      }
    }
  }
}
