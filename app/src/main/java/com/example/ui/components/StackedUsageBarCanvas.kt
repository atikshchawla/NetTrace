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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion
import kotlinx.coroutines.delay

/**
 * Stacked horizontal ratio bar comparing Wi-Fi (blue) vs Mobile (green) data for today.
 */
@Composable
fun StackedUsageBarCanvas(
  wifiBytes: Long,
  mobileBytes: Long,
  modifier: Modifier = Modifier,
  height: Dp = 12.dp
) {
  var startAnimation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    delay(40)
    startAnimation = true
  }

  val totalBytes = (wifiBytes + mobileBytes).coerceAtLeast(1L)
  val rawWifiRatio = wifiBytes.toFloat() / totalBytes.toFloat()
  val rawMobileRatio = mobileBytes.toFloat() / totalBytes.toFloat()

  val animatedScale by animateFloatAsState(
    targetValue = if (startAnimation) 1f else 0f,
    animationSpec = NetTraceMotion.springSpec(),
    label = "stacked_bar_scale"
  )

  val raisedColor = NetTraceColors.Raised
  val wifiColor = NetTraceColors.Wifi
  val mobileColor = NetTraceColors.Mobile

  Canvas(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
  ) {
    val w = size.width
    val h = size.height
    val r = CornerRadius(h / 2f, h / 2f)

    if (wifiBytes == 0L && mobileBytes == 0L) {
      // Empty placeholder track
      drawRoundRect(
        color = raisedColor,
        size = Size(w, h),
        cornerRadius = r
      )
      return@Canvas
    }

    val wifiWidth = (w * rawWifiRatio * animatedScale).coerceAtLeast(0f)
    val mobileWidth = (w * rawMobileRatio * animatedScale).coerceAtLeast(0f)

    // Base background
    drawRoundRect(
      color = raisedColor,
      size = Size(w, h),
      cornerRadius = r
    )

    if (rawWifiRatio > 0f) {
      // Wi-Fi segment
      drawRoundRect(
        color = wifiColor,
        topLeft = Offset(0f, 0f),
        size = Size(wifiWidth, h),
        cornerRadius = if (mobileWidth <= 0f) r else CornerRadius(h / 2f, h / 2f)
      )
    }

    if (rawMobileRatio > 0f) {
      // Mobile segment
      val startX = (w - mobileWidth).coerceAtLeast(wifiWidth)
      drawRoundRect(
        color = mobileColor,
        topLeft = Offset(startX, 0f),
        size = Size(mobileWidth, h),
        cornerRadius = if (wifiWidth <= 0f) r else CornerRadius(h / 2f, h / 2f)
      )
    }
  }
}
