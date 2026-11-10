package com.example.ui.components

import android.graphics.Paint
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyUsage
import com.example.data.model.UsageFormatters
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion
import kotlinx.coroutines.delay

/**
 * Native Jetpack Compose implementation of D3 monotone cubic spline / Recharts AreaChart.
 * Visualizes daily data consumption over the selected month with smooth Bezier curves,
 * gradient area glow fill, Cartesian reference grid, daily plan pace reference line,
 * and touch/drag scrubbing crosshair with an interactive floating tooltip.
 */
@Composable
fun D3LineGraphCanvas(
  dailyList: List<DailyUsage>,
  dailyPaceBytes: Long = 0L,
  onDaySelected: ((DailyUsage) -> Unit)? = null,
  modifier: Modifier = Modifier,
  height: Dp = 220.dp
) {
  val view = LocalView.current
  val density = LocalDensity.current
  var selectedIndex by remember(dailyList) { mutableIntStateOf(-1) }
  var startAnimation by remember { mutableStateOf(false) }

  LaunchedEffect(dailyList) {
    startAnimation = false
    delay(40)
    startAnimation = true
    selectedIndex = if (dailyList.isNotEmpty()) dailyList.lastIndex else -1
  }

  val animatedProgress by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = NetTraceMotion.springSpec(),
    label = "d3_line_progress"
  )

  // Resolve colors before Canvas to avoid Composable context issues inside DrawScope
  val wifiColor = NetTraceColors.Wifi
  val mobileColor = NetTraceColors.Mobile
  val warningColor = NetTraceColors.Warning
  val raisedColor = NetTraceColors.Raised
  val hairlineColor = NetTraceColors.Hairline
  val textPrimaryColor = NetTraceColors.TextPrimary

  Box(modifier = modifier) {
    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(height)
        .pointerInput(dailyList) {
          detectTapGestures(
            onPress = { offset ->
              val count = dailyList.size
              if (count > 0) {
                val stepX = size.width / (count.coerceAtLeast(2) - 1).toFloat()
                val idx = (offset.x / stepX).toInt().coerceIn(0, count - 1)
                if (idx != selectedIndex) {
                  selectedIndex = idx
                  view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                  onDaySelected?.invoke(dailyList[idx])
                }
              }
            }
          )
        }
        .pointerInput(dailyList) {
          detectDragGestures(
            onDragStart = { offset ->
              val count = dailyList.size
              if (count > 0) {
                val stepX = size.width / (count.coerceAtLeast(2) - 1).toFloat()
                val idx = (offset.x / stepX).toInt().coerceIn(0, count - 1)
                if (idx != selectedIndex) {
                  selectedIndex = idx
                  view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                  onDaySelected?.invoke(dailyList[idx])
                }
              }
            },
            onDrag = { change, _ ->
              change.consume()
              val count = dailyList.size
              if (count > 0) {
                val stepX = size.width / (count.coerceAtLeast(2) - 1).toFloat()
                val idx = (change.position.x / stepX).toInt().coerceIn(0, count - 1)
                if (idx != selectedIndex) {
                  selectedIndex = idx
                  view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                  onDaySelected?.invoke(dailyList[idx])
                }
              }
            }
          )
        }
    ) {
      if (dailyList.isEmpty()) return@Canvas

      val w = size.width
      val h = size.height
      val count = dailyList.size

      val topPadding = 44.dp.toPx()
      val bottomPadding = 24.dp.toPx()
      val chartHeight = h - topPadding - bottomPadding

      val maxBytes = (dailyList.maxOfOrNull { it.totalBytes } ?: 1L)
        .coerceAtLeast(dailyPaceBytes)
        .coerceAtLeast(1024L * 1024L * 50L) // 50MB floor for visual headroom
        .toFloat()

      // 1. Horizontal Cartesian Grid Lines (Recharts-style subtle guidelines)
      val gridLines = 3
      for (i in 0..gridLines) {
        val y = topPadding + (chartHeight * (i.toFloat() / gridLines))
        drawLine(
          color = hairlineColor.copy(alpha = 0.5f),
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1.dp.toPx()
        )
      }

      // 2. Daily Plan Pace Target Reference Line (if available)
      if (dailyPaceBytes > 0) {
        val paceY = h - bottomPadding - ((dailyPaceBytes.toFloat() / maxBytes) * chartHeight)
        if (paceY in topPadding..(h - bottomPadding)) {
          drawLine(
            color = warningColor.copy(alpha = 0.5f),
            start = Offset(0f, paceY),
            end = Offset(w, paceY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
          )
        }
      }

      // 3. Compute Coordinates for Daily Data Points
      val stepX = if (count > 1) w / (count - 1).toFloat() else w
      val points = mutableListOf<Offset>()
      val mobilePoints = mutableListOf<Offset>()

      dailyList.forEachIndexed { index, day ->
        val x = index * stepX
        val normTotal = (day.totalBytes.toFloat() / maxBytes).coerceIn(0f, 1f)
        val normMobile = (day.mobileBytes.toFloat() / maxBytes).coerceIn(0f, 1f)

        val targetY = (h - bottomPadding) - (normTotal * chartHeight)
        val animatedY = (h - bottomPadding) - ((normTotal * chartHeight) * animatedProgress)
        points.add(Offset(x, animatedY))

        val animatedMobileY = (h - bottomPadding) - ((normMobile * chartHeight) * animatedProgress)
        mobilePoints.add(Offset(x, animatedMobileY))
      }

      // 4. Construct D3 Monotone Spline (Cubic Bézier) Curves
      val totalLinePath = Path()
      val totalAreaPath = Path()

      if (points.isNotEmpty()) {
        totalLinePath.moveTo(points.first().x, points.first().y)
        totalAreaPath.moveTo(points.first().x, h - bottomPadding)
        totalAreaPath.lineTo(points.first().x, points.first().y)

        for (i in 0 until points.size - 1) {
          val p0 = if (i > 0) points[i - 1] else points[i]
          val p1 = points[i]
          val p2 = points[i + 1]
          val p3 = if (i + 2 < points.size) points[i + 2] else p2

          // Catmull-Rom to Cubic Bézier control points for D3-smooth interpolation
          val cp1x = p1.x + (p2.x - p0.x) / 6f
          val cp1y = p1.y + (p2.y - p0.y) / 6f
          val cp2x = p2.x - (p3.x - p1.x) / 6f
          val cp2y = p2.y - (p3.y - p1.y) / 6f

          totalLinePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
          totalAreaPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        }

        totalAreaPath.lineTo(points.last().x, h - bottomPadding)
        totalAreaPath.close()

        // Draw Soft Glow Gradient Area (Recharts Area style)
        drawPath(
          path = totalAreaPath,
          brush = Brush.verticalGradient(
            colors = listOf(
              wifiColor.copy(alpha = 0.28f * animatedProgress),
              wifiColor.copy(alpha = 0.08f * animatedProgress),
              Color.Transparent
            ),
            startY = topPadding,
            endY = h - bottomPadding
          )
        )

        // Draw Main Spline Line
        drawPath(
          path = totalLinePath,
          color = wifiColor,
          style = Stroke(
            width = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
          )
        )
      }

      // 5. Draw Monotone Spline for Cellular/Mobile Data
      if (mobilePoints.isNotEmpty()) {
        val mobileLinePath = Path()
        mobileLinePath.moveTo(mobilePoints.first().x, mobilePoints.first().y)

        for (i in 0 until mobilePoints.size - 1) {
          val p0 = if (i > 0) mobilePoints[i - 1] else mobilePoints[i]
          val p1 = mobilePoints[i]
          val p2 = mobilePoints[i + 1]
          val p3 = if (i + 2 < mobilePoints.size) mobilePoints[i + 2] else p2

          val cp1x = p1.x + (p2.x - p0.x) / 6f
          val cp1y = p1.y + (p2.y - p0.y) / 6f
          val cp2x = p2.x - (p3.x - p1.x) / 6f
          val cp2y = p2.y - (p3.y - p1.y) / 6f

          mobileLinePath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        }

        drawPath(
          path = mobileLinePath,
          color = mobileColor,
          style = Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
          )
        )
      }

      // 6. X-Axis Day Number Labels along baseline
      val labelPaint = Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 10.dp.toPx()
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
      }

      val interval = if (count > 20) 5 else 3
      for (i in 0 until count step interval) {
        val x = i * stepX
        val dayNum = (i + 1).toString()
        drawContext.canvas.nativeCanvas.drawText(
          dayNum,
          x,
          h - 6.dp.toPx(),
          labelPaint
        )
      }

      // 7. Interactive Scrubbing Crosshair & Floating Tooltip (Recharts-style tooltip)
      if (selectedIndex in points.indices) {
        val pt = points[selectedIndex]
        val selectedDay = dailyList[selectedIndex]

        // Vertical cursor guide
        drawLine(
          color = textPrimaryColor.copy(alpha = 0.35f),
          start = Offset(pt.x, topPadding),
          end = Offset(pt.x, h - bottomPadding),
          strokeWidth = 1.dp.toPx(),
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )

        // Point halo and center dot on Total line
        drawCircle(
          color = wifiColor.copy(alpha = 0.3f),
          radius = 7.dp.toPx(),
          center = pt
        )
        drawCircle(
          color = wifiColor,
          radius = 4.dp.toPx(),
          center = pt
        )

        // Point dot on Mobile line
        if (selectedIndex in mobilePoints.indices) {
          drawCircle(
            color = mobileColor,
            radius = 3.5.dp.toPx(),
            center = mobilePoints[selectedIndex]
          )
        }

        // Floating Inspection Tooltip Box (top clamped)
        val tooltipWidth = 120.dp.toPx()
        val tooltipHeight = 32.dp.toPx()
        val tooltipX = (pt.x - (tooltipWidth / 2f)).coerceIn(8.dp.toPx(), w - tooltipWidth - 8.dp.toPx())
        val tooltipY = 4.dp.toPx()

        drawRoundRect(
          color = raisedColor,
          topLeft = Offset(tooltipX, tooltipY),
          size = Size(tooltipWidth, tooltipHeight),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
        drawRoundRect(
          color = hairlineColor,
          topLeft = Offset(tooltipX, tooltipY),
          size = Size(tooltipWidth, tooltipHeight),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
          style = Stroke(width = 1.dp.toPx())
        )

        val tooltipPaint = Paint().apply {
          color = android.graphics.Color.WHITE
          textSize = 11.dp.toPx()
          isAntiAlias = true
          typeface = Typeface.DEFAULT_BOLD
          textAlign = Paint.Align.CENTER
        }

        val textStr = "${selectedDay.dateLabel}: ${UsageFormatters.formatBytes(selectedDay.totalBytes)}"
        drawContext.canvas.nativeCanvas.drawText(
          textStr,
          tooltipX + (tooltipWidth / 2f),
          tooltipY + 20.dp.toPx(),
          tooltipPaint
        )
      }
    }
  }
}
