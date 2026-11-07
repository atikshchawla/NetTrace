package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.AppUsageItem
import com.example.data.model.UsageFormatters
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion
import kotlinx.coroutines.delay

/**
 * Item row displaying an app's icon, name, formatted usage, and a horizontal bar scaled to the top app.
 */
@Composable
fun AppUsageBarItem(
  item: AppUsageItem,
  maxBytes: Long,
  index: Int,
  modifier: Modifier = Modifier
) {
  var startAnimation by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    delay(40L * (index % 15).toLong())
    startAnimation = true
  }

  val fraction = if (maxBytes > 0) (item.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.02f, 1f) else 0.02f
  val animatedFraction by animateFloatAsState(
    targetValue = if (startAnimation) fraction else 0f,
    animationSpec = NetTraceMotion.springSpec(),
    label = "app_bar_fraction"
  )

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 6.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // App Icon
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(NetTraceColors.Raised),
        contentAlignment = Alignment.Center
      ) {
        if (item.icon != null) {
          val bitmap = remember(item.icon) {
            try {
              item.icon.toBitmap(width = 72, height = 72).asImageBitmap()
            } catch (_: Exception) {
              null
            }
          }
          if (bitmap != null) {
            Image(
              bitmap = bitmap,
              contentDescription = item.appName,
              modifier = Modifier.size(36.dp)
            )
          } else {
            Icon(
              imageVector = NetTraceIcons.Apps,
              contentDescription = item.appName,
              tint = NetTraceColors.TextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        } else {
          Icon(
            imageVector = NetTraceIcons.Apps,
            contentDescription = item.appName,
            tint = NetTraceColors.TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // App Name + UID
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.appName,
          color = NetTraceColors.TextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "W: ${UsageFormatters.formatBytes(item.wifiBytes)}",
            color = NetTraceColors.Wifi,
            fontSize = 11.sp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "M: ${UsageFormatters.formatBytes(item.mobileBytes)}",
            color = NetTraceColors.Mobile,
            fontSize = 11.sp
          )
        }
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Total data formatted
      Text(
        text = UsageFormatters.formatBytes(item.totalBytes),
        color = NetTraceColors.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Horizontal usage scale bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(CircleShape)
        .background(NetTraceColors.Raised)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(animatedFraction)
          .height(6.dp)
          .clip(CircleShape)
          .background(NetTraceColors.Mobile)
      )
    }
  }
}
