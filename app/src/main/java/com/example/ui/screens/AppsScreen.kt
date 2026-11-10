package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.AppUsageItem
import com.example.data.model.UsageFormatters
import com.example.data.model.UsageRange
import com.example.ui.components.AppIconTile
import com.example.ui.components.AppUsageBarItem
import com.example.ui.components.SlidingPillSelector
import com.example.ui.theme.NetTraceColors

/**
 * NetTrace Apps Screen:
 * Lists the applications by data consumption with a sliding pill range selector.
 * Allows tapping on any app to set or clear per-app consumption ceilings with alert warnings.
 */
@Composable
fun AppsScreen(
  selectedRange: UsageRange,
  onRangeSelected: (UsageRange) -> Unit,
  appList: List<AppUsageItem>,
  appLimits: Map<String, Long> = emptyMap(),
  onSetAppLimit: (String, Long) -> Unit = { _, _ -> },
  onRemoveAppLimit: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val maxBytes = appList.firstOrNull()?.totalBytes ?: 1L
  var selectedAppForLimit by remember { mutableStateOf<AppUsageItem?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NetTraceColors.Background)
      .padding(horizontal = 16.dp, vertical = 20.dp)
  ) {
    // Header
    Text(
      text = "App Usage",
      color = NetTraceColors.TextPrimary,
      fontSize = 34.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = (-0.5).sp
    )
    Text(
      text = "Tap any app to set a data limit warning",
      color = NetTraceColors.TextTertiary,
      fontSize = 13.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Range Selector (Sliding Pill)
    SlidingPillSelector(
      selectedRange = selectedRange,
      onRangeSelected = onRangeSelected
    )

    Spacer(modifier = Modifier.height(16.dp))

    if (appList.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(28.dp))
          .background(NetTraceColors.Surface)
          .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(28.dp))
          .padding(36.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "No App Traffic Recorded",
            color = NetTraceColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Traffic will appear here as apps transmit Wi-Fi or cellular data.",
            color = NetTraceColors.TextTertiary,
            fontSize = 13.sp
          )
        }
      }
    } else {
      // Header info strip: App Count and interaction hint
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "${appList.size} Applications Ranked",
          color = NetTraceColors.TextSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(NetTraceColors.Raised)
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = "Tap card to set ceiling",
            color = NetTraceColors.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Spaced Cards Layout
      LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        itemsIndexed(appList) { index, app ->
          val limit = appLimits[app.packageName]
          AppUsageBarItem(
            item = app,
            maxBytes = maxBytes,
            index = index,
            limitBytes = limit,
            onClick = { selectedAppForLimit = app }
          )
        }
        item {
          Spacer(modifier = Modifier.height(88.dp)) // Clearance for floating bottom bar
        }
      }
    }
  }

  // Per-App Limit Modal Dialog
  selectedAppForLimit?.let { app ->
    val existingLimit = appLimits[app.packageName] ?: 0L
    val initialMb = if (existingLimit > 0L) (existingLimit / (1024L * 1024L)).toString() else "1024"
    var customMbText by remember(app) { mutableStateOf(initialMb) }

    Dialog(onDismissRequest = { selectedAppForLimit = null }) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(NetTraceColors.Surface)
          .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(24.dp))
          .padding(22.dp)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            AppIconTile(item = app)
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = app.appName,
                color = NetTraceColors.TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Used: ${UsageFormatters.formatBytes(app.totalBytes)}",
                color = NetTraceColors.TextSecondary,
                fontSize = 12.sp
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Assign Data Limit",
            color = NetTraceColors.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "NetTrace will post an alert notification if this application exceeds your assigned ceiling in the current cycle.",
            color = NetTraceColors.TextTertiary,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          // Preset limits row
          val presets = listOf(
            256L to "256 MB",
            512L to "512 MB",
            1024L to "1 GB",
            2048L to "2 GB",
            5120L to "5 GB"
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            presets.forEach { (mb, label) ->
              val isSelected = (customMbText.toLongOrNull() == mb)
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) NetTraceColors.Wifi else NetTraceColors.Raised)
                  .clickable { customMbText = mb.toString() }
                  .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = label,
                  color = if (isSelected) NetTraceColors.Background else NetTraceColors.TextPrimary,
                  fontSize = 10.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = customMbText,
            onValueChange = { customMbText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Limit in Megabytes (MB)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = NetTraceColors.Wifi,
              unfocusedBorderColor = NetTraceColors.Hairline,
              focusedTextColor = NetTraceColors.TextPrimary,
              unfocusedTextColor = NetTraceColors.TextPrimary,
              focusedLabelColor = NetTraceColors.Wifi,
              unfocusedLabelColor = NetTraceColors.TextTertiary
            ),
            shape = RoundedCornerShape(12.dp)
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Action buttons: Save, Remove, Cancel
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            if (existingLimit > 0L) {
              Button(
                onClick = {
                  onRemoveAppLimit(app.packageName)
                  selectedAppForLimit = null
                },
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = NetTraceColors.Raised,
                  contentColor = NetTraceColors.Warning
                ),
                shape = RoundedCornerShape(12.dp)
              ) {
                Text("Clear", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              }
            }

            Button(
              onClick = { selectedAppForLimit = null },
              modifier = Modifier.weight(1f).height(44.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = NetTraceColors.Raised,
                contentColor = NetTraceColors.TextSecondary
              ),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Cancel", fontSize = 13.sp)
            }

            Button(
              onClick = {
                val mb = customMbText.toLongOrNull() ?: 0L
                if (mb > 0L) {
                  onSetAppLimit(app.packageName, mb * 1024L * 1024L)
                } else {
                  onRemoveAppLimit(app.packageName)
                }
                selectedAppForLimit = null
              },
              modifier = Modifier.weight(1f).height(44.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = NetTraceColors.Wifi,
                contentColor = NetTraceColors.Background
              ),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Save", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }
  }
}
