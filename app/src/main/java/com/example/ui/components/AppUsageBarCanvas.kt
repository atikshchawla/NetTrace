package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * High-polish App telemetry row with real launcher icons, stylized monograms
 * for system services/uninstalled UIDs, and proportional split telemetry bars.
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
    delay(30L * (index % 12).toLong())
    startAnimation = true
  }

  val fraction = if (maxBytes > 0) (item.totalBytes.toFloat() / maxBytes.toFloat()).coerceIn(0.02f, 1f) else 0.02f
  val animatedFraction by animateFloatAsState(
    targetValue = if (startAnimation) fraction else 0f,
    animationSpec = NetTraceMotion.springSpec(),
    label = "app_bar_fraction"
  )

  val wifiFraction = if (item.totalBytes > 0) (item.wifiBytes.toFloat() / item.totalBytes.toFloat()) else 0f
  val mobileFraction = if (item.totalBytes > 0) (item.mobileBytes.toFloat() / item.totalBytes.toFloat()) else 0f

  val raisedColor = NetTraceColors.Raised
  val wifiColor = NetTraceColors.Wifi
  val mobileColor = NetTraceColors.Mobile

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1. App Icon or Stylized Monogram
      AppIconTile(item = item)

      Spacer(modifier = Modifier.width(12.dp))

      // 2. App Name, System Tag, Package/UID, and Wi-Fi/Mobile breakdown
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = item.appName,
            color = NetTraceColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
          )

          if (item.isSystem || item.categoryLabel.isNotBlank()) {
            val tagText = item.categoryLabel.ifBlank { "System" }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                  if (item.categoryLabel == "Removed") NetTraceColors.Warning.copy(alpha = 0.15f)
                  else NetTraceColors.Wifi.copy(alpha = 0.15f)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                text = tagText.uppercase(),
                color = if (item.categoryLabel == "Removed") NetTraceColors.Warning else NetTraceColors.Wifi,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Secondary caption: Clean package name or UID
        val subtitle = when {
          item.packageName.startsWith("android.kernel") -> "OS Kernel • UID 0"
          item.packageName.startsWith("android.system") -> "Android System Services • UID ${item.uid}"
          item.packageName.startsWith("system.daemon") -> "Background Service • UID ${item.uid}"
          item.packageName.startsWith("app.removed") -> "Uninstalled Package • UID ${item.uid}"
          else -> item.packageName
        }

        Text(
          text = subtitle,
          color = NetTraceColors.TextTertiary,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Wi-Fi and Mobile indicators
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(NetTraceColors.Wifi)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Wi-Fi: ${UsageFormatters.formatBytes(item.wifiBytes)}",
              color = NetTraceColors.TextSecondary,
              fontSize = 11.sp
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(NetTraceColors.Mobile)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Mobile: ${UsageFormatters.formatBytes(item.mobileBytes)}",
              color = NetTraceColors.TextSecondary,
              fontSize = 11.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(10.dp))

      // 3. Total data consumption
      Column(horizontalAlignment = Alignment.End) {
        Text(
          text = UsageFormatters.formatBytes(item.totalBytes),
          color = NetTraceColors.TextPrimary,
          fontSize = 15.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 4. Proportional Dual-Color Scale Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(CircleShape)
        .background(raisedColor)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth(animatedFraction)
          .height(6.dp)
          .clip(CircleShape)
      ) {
        if (wifiFraction > 0f) {
          Box(
            modifier = Modifier
              .weight(wifiFraction.coerceAtLeast(0.01f))
              .height(6.dp)
              .background(wifiColor)
          )
        }
        if (mobileFraction > 0f) {
          Box(
            modifier = Modifier
              .weight(mobileFraction.coerceAtLeast(0.01f))
              .height(6.dp)
              .background(mobileColor)
          )
        }
      }
    }
  }
}

/**
 * Renders a real bitmap if available or a stylish Apple-like monogram/category tile.
 */
@Composable
private fun AppIconTile(item: AppUsageItem) {
  Box(
    modifier = Modifier
      .size(42.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(NetTraceColors.Raised)
      .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(12.dp)),
    contentAlignment = Alignment.Center
  ) {
    if (item.icon != null) {
      val bitmap = remember(item.icon) {
        try {
          item.icon.toBitmap(width = 84, height = 84).asImageBitmap()
        } catch (_: Exception) {
          null
        }
      }
      if (bitmap != null) {
        Image(
          bitmap = bitmap,
          contentDescription = item.appName,
          modifier = Modifier.size(42.dp)
        )
        return@Box
      }
    }

    // Fallback: Categorized Apple-like Monogram Glyph Tile
    when {
      item.categoryLabel == "Kernel" || item.categoryLabel == "System" -> {
        Box(
          modifier = Modifier
            .size(42.dp)
            .background(
              Brush.linearGradient(
                colors = listOf(Color(0xFF3A3A3C), Color(0xFF1C1C1E))
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = NetTraceIcons.Gear,
            contentDescription = null,
            tint = Color(0xFF0A84FF),
            modifier = Modifier.size(20.dp)
          )
        }
      }
      item.categoryLabel == "Cellular" -> {
        Box(
          modifier = Modifier
            .size(42.dp)
            .background(
              Brush.linearGradient(
                colors = listOf(Color(0xFF1E3A2B), Color(0xFF14241B))
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = NetTraceIcons.Cellular,
            contentDescription = null,
            tint = Color(0xFF30D158),
            modifier = Modifier.size(20.dp)
          )
        }
      }
      item.categoryLabel == "Hotspot" -> {
        Box(
          modifier = Modifier
            .size(42.dp)
            .background(
              Brush.linearGradient(
                colors = listOf(Color(0xFF14293D), Color(0xFF0E1A29))
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = NetTraceIcons.Wifi,
            contentDescription = null,
            tint = Color(0xFF0A84FF),
            modifier = Modifier.size(20.dp)
          )
        }
      }
      item.categoryLabel == "Removed" -> {
        Box(
          modifier = Modifier
            .size(42.dp)
            .background(Color(0xFF2C2C2E)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = NetTraceIcons.Warning,
            contentDescription = null,
            tint = Color(0xFFFF9F0A),
            modifier = Modifier.size(18.dp)
          )
        }
      }
      else -> {
        // Monogram for un-iconed apps
        val initial = item.appName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "A"
        val seed = item.uid.hashCode()
        val bgColors = listOf(
          listOf(Color(0xFF2C3E50), Color(0xFF1A252F)),
          listOf(Color(0xFF2B5876), Color(0xFF4E4376)),
          listOf(Color(0xFF134E5E), Color(0xFF24484E)),
          listOf(Color(0xFF4A148C), Color(0xFF2A0845)),
          listOf(Color(0xFF5A3F11), Color(0xFF2E2008))
        )
        val chosenGradient = bgColors[kotlin.math.abs(seed) % bgColors.size]

        Box(
          modifier = Modifier
            .size(42.dp)
            .background(Brush.linearGradient(chosenGradient)),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = initial,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
