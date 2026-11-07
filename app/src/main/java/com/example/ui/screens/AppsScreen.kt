package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsageItem
import com.example.data.model.UsageRange
import com.example.ui.components.AppUsageBarItem
import com.example.ui.components.SlidingPillSelector
import com.example.ui.theme.NetTraceColors

/**
 * NetTrace Apps Screen:
 * Lists the top 20 applications by data consumption with a sliding pill range selector.
 */
@Composable
fun AppsScreen(
  selectedRange: UsageRange,
  onRangeSelected: (UsageRange) -> Unit,
  appList: List<AppUsageItem>,
  modifier: Modifier = Modifier
) {
  val maxBytes = appList.firstOrNull()?.totalBytes ?: 1L

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
      text = "Top consumers ranked by cumulative traffic",
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
      // Main Card containing the App List
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(28.dp))
          .background(NetTraceColors.Surface)
          .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(28.dp))
          .padding(horizontal = 16.dp, vertical = 14.dp)
      ) {
        LazyColumn(
          modifier = Modifier.fillMaxWidth()
        ) {
          itemsIndexed(appList) { index, app ->
            AppUsageBarItem(
              item = app,
              maxBytes = maxBytes,
              index = index
            )
            if (index < appList.lastIndex) {
              Spacer(
                modifier = Modifier
                  .fillMaxWidth()
                  .height(1.dp)
                  .background(NetTraceColors.Hairline)
              )
            }
          }
          item {
            Spacer(modifier = Modifier.height(80.dp)) // Padding above bottom bar
          }
        }
      }
    }
  }
}
