package com.example.ui.theme

import android.content.Context
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/**
 * NetTrace Motion System:
 * Apple-like spring physics (dampingRatio 0.8, low stiffness),
 * press scale feedback (0.97) with haptics, and system animation scale checks.
 */
object NetTraceMotion {

  fun <T> springSpec(): AnimationSpec<T> {
    return spring(
      dampingRatio = 0.8f,
      stiffness = Spring.StiffnessLow
    )
  }

  fun <T> navigationSpec(): AnimationSpec<T> {
    return tween(
      durationMillis = 300,
      easing = FastOutSlowInEasing
    )
  }

  fun areAnimationsEnabled(context: Context): Boolean {
    return try {
      val scale = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1.0f
      )
      scale > 0f
    } catch (_: Exception) {
      true
    }
  }
}

/**
 * Modifier for Apple-like card press feedback:
 * Scales card to 0.97 on touch with light haptic tick.
 */
@Composable
fun Modifier.pressScaleEffect(
  onClick: (() -> Unit)? = null,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
): Modifier {
  val isPressed by interactionSource.collectIsPressedAsState()
  val context = LocalContext.current
  val view = LocalView.current
  val animEnabled = NetTraceMotion.areAnimationsEnabled(context)

  val scale by animateFloatAsState(
    targetValue = if (isPressed && animEnabled) 0.97f else 1.0f,
    animationSpec = NetTraceMotion.springSpec(),
    label = "card_press_scale"
  )

  val baseModifier = this
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
    }

  return if (onClick != null) {
    baseModifier.clickable(
      interactionSource = interactionSource,
      indication = null
    ) {
      view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
      onClick()
    }
  } else {
    baseModifier
  }
}
