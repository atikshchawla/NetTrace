package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion
import kotlinx.coroutines.delay

/**
 * Apple-style animated circular plan usage ring.
 * Features:
 * - Spring growth animation with 40ms stagger
 * - Smooth color transitions across threshold states (blue -> amber -> red)
 * - Tabular % display in center
 */
@Composable
fun PlanRingCanvas(
  percentUsed: Float,
  modifier: Modifier = Modifier,
  size: Dp = 190.dp,
  strokeWidth: Dp = 18.dp
) {
  var startAnimation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    delay(40)
    startAnimation = true
  }

  val targetPercent = if (startAnimation) percentUsed.coerceIn(0f, 1.5f) else 0f
  val animatedProgress by animateFloatAsState(
    targetValue = targetPercent,
    animationSpec = NetTraceMotion.springSpec(),
    label = "ring_progress"
  )

  val targetColor = NetTraceColors.colorForUsage(animatedProgress)
  val animatedColor by animateColorAsState(
    targetValue = targetColor,
    animationSpec = NetTraceMotion.springSpec(),
    label = "ring_color"
  )

  val trackColor = NetTraceColors.Raised

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val sw = strokeWidth.toPx()
      val arcSize = this.size.width - sw
      val topLeftOffset = sw / 2f

      // Inactive track ring
      drawArc(
        color = trackColor,
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        style = Stroke(width = sw, cap = StrokeCap.Round),
        size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
        topLeft = androidx.compose.ui.geometry.Offset(topLeftOffset, topLeftOffset)
      )

      // Active progress arc
      val sweep = (animatedProgress * 360f).coerceIn(0f, 360f)
      if (sweep > 0f) {
        drawArc(
          color = animatedColor,
          startAngle = -90f,
          sweepAngle = sweep,
          useCenter = false,
          style = Stroke(width = sw, cap = StrokeCap.Round),
          size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
          topLeft = androidx.compose.ui.geometry.Offset(topLeftOffset, topLeftOffset)
        )
      }
    }

    // Centered percentage readout
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      val displayPercent = (animatedProgress * 100).toInt()
      Text(
        text = "$displayPercent%",
        color = NetTraceColors.TextPrimary,
        fontSize = 38.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )
      Text(
        text = "PLAN USED",
        color = NetTraceColors.TextTertiary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp
      )
    }
  }
}
