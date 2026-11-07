package com.example.ui.components

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
 * Donut chart visualizing proportional data usage between SIM 1 and SIM 2.
 */
@Composable
fun DualSimDonutCanvas(
  sim1Bytes: Long,
  sim2Bytes: Long,
  modifier: Modifier = Modifier,
  size: Dp = 120.dp,
  strokeWidth: Dp = 12.dp
) {
  var startAnimation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    delay(40)
    startAnimation = true
  }

  val total = (sim1Bytes + sim2Bytes).coerceAtLeast(1L)
  val sim1Ratio = (sim1Bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
  val sim2Ratio = (sim2Bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)

  val animatedProgress by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = NetTraceMotion.springSpec(),
    label = "donut_progress"
  )

  val raisedColor = NetTraceColors.Raised
  val mobileColor = NetTraceColors.Mobile
  val sim2Color = NetTraceColors.Sim2

  Box(
    modifier = modifier.size(size),
    contentAlignment = Alignment.Center
  ) {
    Canvas(modifier = Modifier.size(size)) {
      val sw = strokeWidth.toPx()
      val arcSize = this.size.width - sw
      val topLeft = Offset(sw / 2f, sw / 2f)

      if (sim1Bytes == 0L && sim2Bytes == 0L) {
        drawArc(
          color = raisedColor,
          startAngle = 0f,
          sweepAngle = 360f,
          useCenter = false,
          style = Stroke(width = sw, cap = StrokeCap.Round),
          size = Size(arcSize, arcSize),
          topLeft = topLeft
        )
        return@Canvas
      }

      val sim1Sweep = sim1Ratio * 360f * animatedProgress
      val sim2Sweep = sim2Ratio * 360f * animatedProgress

      // SIM 1 Arc
      if (sim1Sweep > 0f) {
        drawArc(
          color = mobileColor,
          startAngle = -90f,
          sweepAngle = sim1Sweep,
          useCenter = false,
          style = Stroke(width = sw, cap = StrokeCap.Butt),
          size = Size(arcSize, arcSize),
          topLeft = topLeft
        )
      }

      // SIM 2 Arc
      if (sim2Sweep > 0f) {
        drawArc(
          color = sim2Color,
          startAngle = -90f + sim1Sweep,
          sweepAngle = sim2Sweep,
          useCenter = false,
          style = Stroke(width = sw, cap = StrokeCap.Butt),
          size = Size(arcSize, arcSize),
          topLeft = topLeft
        )
      }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = "SIM",
        color = NetTraceColors.TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Text(
        text = "SPLIT",
        color = NetTraceColors.TextTertiary,
        fontSize = 9.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp
      )
    }
  }
}
