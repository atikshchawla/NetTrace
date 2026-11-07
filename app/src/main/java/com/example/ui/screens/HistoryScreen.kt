package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyUsage
import com.example.data.model.UsageFormatters
import com.example.ui.components.HistoryBarChartCanvas
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.pressScaleEffect

/**
 * NetTrace History Screen:
 * 30-day interactive scrubbable daily bar chart with dashed plan pace reference,
 * value bubble, and daily breakdown cards.
 */
@Composable
fun HistoryScreen(
  dailyList: List<DailyUsage>,
  dailyPaceBytes: Long,
  modifier: Modifier = Modifier
) {
  var selectedDay by remember { mutableStateOf<DailyUsage?>(null) }
  val scrollState = rememberScrollState()

  val total30dMobile = dailyList.sumOf { it.mobileBytes }
  val total30dWifi = dailyList.sumOf { it.wifiBytes }
  val activeDaysCount = dailyList.size.coerceAtLeast(1)
  val averageDaily = (total30dMobile + total30dWifi) / activeDaysCount

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NetTraceColors.Background)
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 20.dp)
  ) {
    // Header
    Text(
      text = "30-Day History",
      color = NetTraceColors.TextPrimary,
      fontSize = 34.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = (-0.5).sp
    )
    Text(
      text = "Touch or drag across bars to scrub daily telemetry",
      color = NetTraceColors.TextTertiary,
      fontSize = 13.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Scrubbable Bar Chart Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(NetTraceColors.Surface)
        .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(28.dp))
        .pressScaleEffect()
        .padding(20.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Daily Consumption",
            color = NetTraceColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NetTraceColors.Warning)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Pace target",
              color = NetTraceColors.TextTertiary,
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        HistoryBarChartCanvas(
          dailyList = dailyList,
          dailyPaceBytes = dailyPaceBytes,
          onDaySelected = { selectedDay = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NetTraceColors.Mobile)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Mobile",
              color = NetTraceColors.TextSecondary,
              fontSize = 12.sp
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NetTraceColors.Wifi)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Wi-Fi",
              color = NetTraceColors.TextSecondary,
              fontSize = 12.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Scrubbed Day Inspection Card
    val dayToInspect = selectedDay ?: dailyList.lastOrNull()
    if (dayToInspect != null) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(NetTraceColors.Surface)
          .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(24.dp))
          .padding(18.dp)
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = dayToInspect.dateLabel,
              color = NetTraceColors.TextPrimary,
              fontSize = 17.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = UsageFormatters.formatBytes(dayToInspect.totalBytes),
              color = NetTraceColors.TextPrimary,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(
                text = "Cellular Data",
                color = NetTraceColors.TextTertiary,
                fontSize = 12.sp
              )
              Text(
                text = UsageFormatters.formatBytes(dayToInspect.mobileBytes),
                color = NetTraceColors.Mobile,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
              )
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = "Wi-Fi Data",
                color = NetTraceColors.TextTertiary,
                fontSize = 12.sp
              )
              Text(
                text = UsageFormatters.formatBytes(dayToInspect.wifiBytes),
                color = NetTraceColors.Wifi,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 30-Day Aggregation Summary Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(NetTraceColors.Surface)
        .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(24.dp))
        .padding(18.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "30-Day Cumulative",
          color = NetTraceColors.TextSecondary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text(
              text = UsageFormatters.formatBytes(total30dMobile),
              color = NetTraceColors.TextPrimary,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Total Mobile",
              color = NetTraceColors.TextTertiary,
              fontSize = 11.sp
            )
          }

          Column {
            Text(
              text = UsageFormatters.formatBytes(total30dWifi),
              color = NetTraceColors.TextPrimary,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Total Wi-Fi",
              color = NetTraceColors.TextTertiary,
              fontSize = 11.sp
            )
          }

          Column(horizontalAlignment = Alignment.End) {
            Text(
              text = UsageFormatters.formatBytes(averageDaily),
              color = NetTraceColors.TextPrimary,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Daily Average",
              color = NetTraceColors.TextTertiary,
              fontSize = 11.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(80.dp)) // Padding above bottom bar
  }
}
