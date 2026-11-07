package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.DailyUsage
import com.example.data.model.UsageFormatters
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion
import kotlinx.coroutines.delay

/**
 * 30-day scrubbable interactive bar chart.
 * Features:
 * - Scrub gestures with snap + haptic tick
 * - Dashed average plan-pace line
 * - Value bubble showing date and usage for the scrubbed day
 */
@Composable
fun HistoryBarChartCanvas(
  dailyList: List<DailyUsage>,
  dailyPaceBytes: Long,
  modifier: Modifier = Modifier,
  height: Dp = 200.dp,
  onDaySelected: ((DailyUsage?) -> Unit)? = null
) {
  val view = LocalView.current
  var selectedIndex by remember { mutableIntStateOf(-1) }
  var startAnimation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    delay(40)
    startAnimation = true
  }

  val animatedScale by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = NetTraceMotion.springSpec(),
    label = "history_bars_scale"
  )

  val warningColor = NetTraceColors.Warning
  val wifiColor = NetTraceColors.Wifi
  val mobileColor = NetTraceColors.Mobile
  val raisedColor = NetTraceColors.Raised
  val hairlineColor = NetTraceColors.Hairline

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
                val barWidth = size.width / count
                val idx = (offset.x / barWidth).toInt().coerceIn(0, count - 1)
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
                val barWidth = size.width / count
                val idx = (offset.x / barWidth).toInt().coerceIn(0, count - 1)
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
                val barWidth = size.width / count
                val idx = (change.position.x / barWidth).toInt().coerceIn(0, count - 1)
                if (idx != selectedIndex) {
                  selectedIndex = idx
                  view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                  onDaySelected?.invoke(dailyList[idx])
                }
              }
            },
            onDragEnd = {
              // Keep last selected or notify
            }
          )
        }
    ) {
      if (dailyList.isEmpty()) return@Canvas

      val w = size.width
      val h = size.height
      val count = dailyList.size
      val slotWidth = w / count
      val barWidth = (slotWidth * 0.65f).coerceAtLeast(2f)
      val barSpacing = slotWidth - barWidth

      val maxBytes = (dailyList.maxOfOrNull { it.totalBytes } ?: 1L)
        .coerceAtLeast(dailyPaceBytes)
        .coerceAtLeast(1024L * 1024L * 100L) // Min 100MB scale
        .toFloat()

      val chartTopPadding = 42.dp.toPx()
      val usableHeight = h - chartTopPadding - 16.dp.toPx()

      // Draw dashed daily pace reference line if within bounds
      if (dailyPaceBytes > 0) {
        val paceY = h - 16.dp.toPx() - ((dailyPaceBytes.toFloat() / maxBytes) * usableHeight)
        if (paceY >= chartTopPadding) {
          drawLine(
            color = warningColor.copy(alpha = 0.45f),
            start = Offset(0f, paceY),
            end = Offset(w, paceY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
          )
        }
      }

      // Draw daily bars
      dailyList.forEachIndexed { index, day ->
        val x = (index * slotWidth) + (barSpacing / 2f)
        val mobileFrac = (day.mobileBytes.toFloat() / maxBytes).coerceIn(0f, 1f)
        val wifiFrac = (day.wifiBytes.toFloat() / maxBytes).coerceIn(0f, 1f)

        val mobileBarH = (mobileFrac * usableHeight * animatedScale).coerceAtLeast(0f)
        val wifiBarH = (wifiFrac * usableHeight * animatedScale).coerceAtLeast(0f)
        val totalH = (mobileBarH + wifiBarH).coerceAtLeast(2.dp.toPx())

        val baselineY = h - 16.dp.toPx()
        val isSelected = (index == selectedIndex)

        // Draw Wi-Fi segment (bottom)
        val wifiTop = baselineY - wifiBarH
        if (wifiBarH > 0f) {
          drawRoundRect(
            color = if (isSelected) wifiColor else wifiColor.copy(alpha = 0.75f),
            topLeft = Offset(x, wifiTop),
            size = Size(barWidth, wifiBarH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
          )
        }

        // Draw Mobile segment (stacked on top of Wi-Fi)
        val mobileTop = baselineY - totalH
        if (mobileBarH > 0f) {
          drawRoundRect(
            color = if (isSelected) mobileColor else mobileColor.copy(alpha = 0.85f),
            topLeft = Offset(x, mobileTop),
            size = Size(barWidth, mobileBarH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
          )
        }

        // If scrubbed, draw highlight glow column
        if (isSelected) {
          drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset(index * slotWidth, 0f),
            size = Size(slotWidth, h),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
          )
        }
      }

      // Draw scrubbed value bubble
      if (selectedIndex in dailyList.indices) {
        val selectedDay = dailyList[selectedIndex]
        val xCenter = (selectedIndex * slotWidth) + (slotWidth / 2f)
        val bubbleWidth = 110.dp.toPx()
        val bubbleHeight = 32.dp.toPx()
        val bubbleX = (xCenter - (bubbleWidth / 2f)).coerceIn(8.dp.toPx(), w - bubbleWidth - 8.dp.toPx())
        val bubbleY = 4.dp.toPx()

        drawRoundRect(
          color = raisedColor,
          topLeft = Offset(bubbleX, bubbleY),
          size = Size(bubbleWidth, bubbleHeight),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
        // Hairline outline on bubble
        drawRoundRect(
          color = hairlineColor,
          topLeft = Offset(bubbleX, bubbleY),
          size = Size(bubbleWidth, bubbleHeight),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
          style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )

        // Native canvas text rendering inside bubble
        val paint = android.graphics.Paint().apply {
          color = android.graphics.Color.WHITE
          textSize = 11.dp.toPx()
          isAntiAlias = true
          typeface = android.graphics.Typeface.DEFAULT_BOLD
          textAlign = android.graphics.Paint.Align.CENTER
        }
        val textStr = "${selectedDay.dateLabel}: ${UsageFormatters.formatBytes(selectedDay.totalBytes)}"
        drawContext.canvas.nativeCanvas.drawText(
          textStr,
          bubbleX + (bubbleWidth / 2f),
          bubbleY + (bubbleHeight * 0.65f),
          paint
        )
      }
    }
  }
}
