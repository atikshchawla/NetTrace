package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UsageFormatters
import com.example.data.telemetry.ThroughputSample
import com.example.ui.theme.NetTraceColors

/**
 * Real-time instantaneous download & upload speed monitor with an activity pulse indicator.
 */
@Composable
fun LiveBandwidthMeter(
  sample: ThroughputSample,
  modifier: Modifier = Modifier
) {
  val hasActivity = sample.totalBytesPerSec > 2048L // More than 2 KB/s

  val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.9f,
    targetValue = if (hasActivity) 1.35f else 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_scale"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = if (hasActivity) 0.9f else 0.2f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(NetTraceColors.Surface)
      .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(24.dp))
      .padding(horizontal = 18.dp, vertical = 14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Left: Live Pulse Dot & Label
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier.size(24.dp),
          contentAlignment = Alignment.Center
        ) {
          // Radar pulse ring
          Box(
            modifier = Modifier
              .size(18.dp)
              .scale(pulseScale)
              .clip(CircleShape)
              .background((if (hasActivity) NetTraceColors.Mobile else NetTraceColors.TextTertiary).copy(alpha = pulseAlpha * 0.35f))
          )
          // Solid center core
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(if (hasActivity) NetTraceColors.Mobile else NetTraceColors.TextTertiary)
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "Live Throughput",
              color = NetTraceColors.TextPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          Text(
            text = if (hasActivity) "Active Data Stream" else "Idle Network",
            color = if (hasActivity) NetTraceColors.Mobile else NetTraceColors.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      // Right: Download (↓) and Upload (↑) speeds
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Download
        Column(horizontalAlignment = Alignment.End) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "↓",
              color = NetTraceColors.Wifi,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${UsageFormatters.formatBytes(sample.rxBytesPerSec)}/s",
              color = NetTraceColors.TextPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Text(
            text = "Download",
            color = NetTraceColors.TextTertiary,
            fontSize = 10.sp
          )
        }

        // Upload
        Column(horizontalAlignment = Alignment.End) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "↑",
              color = NetTraceColors.Mobile,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = "${UsageFormatters.formatBytes(sample.txBytesPerSec)}/s",
              color = NetTraceColors.TextPrimary,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Text(
            text = "Upload",
            color = NetTraceColors.TextTertiary,
            fontSize = 10.sp
          )
        }
      }
    }
  }
}
