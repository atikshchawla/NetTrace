package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion
import kotlinx.coroutines.delay

/**
 * 7-day sparkline graph with gradient fill and smooth animated stroke drawing.
 */
@Composable
fun SparklineCanvas(
  dataPoints: List<Long>,
  modifier: Modifier = Modifier,
  lineColor: Color = Color.Unspecified,
  height: Dp = 64.dp
) {
  val resolvedLineColor = if (lineColor != Color.Unspecified) lineColor else NetTraceColors.Mobile
  var startAnimation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    delay(40)
    startAnimation = true
  }

  val animatedProgress by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = NetTraceMotion.springSpec(),
    label = "sparkline_progress"
  )

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
  ) {
    if (dataPoints.isEmpty()) return@Canvas

    val w = size.width
    val h = size.height
    val paddingVertical = 6f
    val effectiveHeight = h - (paddingVertical * 2)

    val maxVal = (dataPoints.maxOrNull() ?: 1L).coerceAtLeast(1L).toFloat()
    val stepX = if (dataPoints.size > 1) w / (dataPoints.size - 1) else w

    val strokePath = Path()
    val fillPath = Path()

    dataPoints.forEachIndexed { index, value ->
      val normalized = (value.toFloat() / maxVal).coerceIn(0f, 1f)
      val x = index * stepX
      val targetY = h - paddingVertical - (normalized * effectiveHeight)
      val baselineY = h - paddingVertical
      val animatedY = baselineY - ((baselineY - targetY) * animatedProgress)

      if (index == 0) {
        strokePath.moveTo(x, animatedY)
        fillPath.moveTo(x, h)
        fillPath.lineTo(x, animatedY)
      } else {
        strokePath.lineTo(x, animatedY)
        fillPath.lineTo(x, animatedY)
      }
    }

    fillPath.lineTo(w, h)
    fillPath.close()

    // Draw gradient fill under the curve
    drawPath(
      path = fillPath,
      brush = Brush.verticalGradient(
        colors = listOf(
          resolvedLineColor.copy(alpha = 0.28f * animatedProgress),
          Color.Transparent
        )
      )
    )

    // Draw telemetry line
    drawPath(
      path = strokePath,
      color = resolvedLineColor,
      style = Stroke(
        width = 2.5f.dp.toPx(),
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
      )
    )
  }
}
