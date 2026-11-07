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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyUsage
import com.example.data.model.PlanForecast
import com.example.data.model.SimInfo
import com.example.data.model.UsageFormatters
import com.example.data.model.UsageTotals
import com.example.ui.components.DualSimDonutCanvas
import com.example.ui.components.NetTraceIcons
import com.example.ui.components.PlanRingCanvas
import com.example.ui.components.SparklineCanvas
import com.example.ui.components.StackedUsageBarCanvas
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.pressScaleEffect

/**
 * NetTrace Home Screen:
 * Displays plan ring, forecast pacing, today's stacked Wi-Fi/mobile bar,
 * 7-day sparkline, and active SIM chips when dual-SIM is detected.
 */
@Composable
fun HomeScreen(
  todayTotals: UsageTotals,
  sevenDaysTotals: UsageTotals,
  cycleTotals: UsageTotals,
  planForecast: PlanForecast,
  activeSims: List<SimInfo>,
  sevenDaysHistory: List<DailyUsage>,
  onOpenSettings: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  var selectedSimIndex by remember { mutableIntStateOf(0) }
  val isDualSim = activeSims.size >= 2
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NetTraceColors.Background)
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 20.dp)
  ) {
    // Header with Large Title 34 bold and Offline Telemetry subtitle
    Column(
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "NetTrace",
        color = NetTraceColors.TextPrimary,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp
      )
      Text(
        text = "Offline Telemetry",
        color = NetTraceColors.TextTertiary,
        fontSize = 13.sp
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Dual-SIM Chips (when 2 SIMs are active)
    if (isDualSim) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        activeSims.forEachIndexed { index, sim ->
          val isSelected = (index == selectedSimIndex)
          Box(
            modifier = Modifier
              .clip(CircleShape)
              .background(if (isSelected) NetTraceColors.Raised else NetTraceColors.Surface)
              .border(
                1.dp,
                if (isSelected) NetTraceColors.Wifi else NetTraceColors.Hairline,
                CircleShape
              )
              .pressScaleEffect(onClick = { selectedSimIndex = index })
              .padding(horizontal = 14.dp, vertical = 8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(if (index == 0) NetTraceColors.Mobile else NetTraceColors.Sim2)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = sim.displayName,
                color = if (isSelected) NetTraceColors.TextPrimary else NetTraceColors.TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
              )
              if (sim.isDefaultData) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "DATA",
                  color = NetTraceColors.Wifi,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }

    // Main Card: Plan Ring & Billing Cycle Pace
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(NetTraceColors.Surface)
        .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(28.dp))
        .pressScaleEffect()
        .padding(20.dp)
    ) {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        PlanRingCanvas(
          percentUsed = planForecast.percentUsed,
          modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Remaining data and days to reset
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = UsageFormatters.formatBytes(planForecast.bytesRemaining),
              color = NetTraceColors.TextPrimary,
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Data Left",
              color = NetTraceColors.TextTertiary,
              fontSize = 12.sp
            )
          }

          Box(
            modifier = Modifier
              .height(36.dp)
              .width(1.dp)
              .background(NetTraceColors.Hairline)
          )

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "${planForecast.daysRemaining} days",
              color = NetTraceColors.TextPrimary,
              fontSize = 22.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "Until Reset",
              color = NetTraceColors.TextTertiary,
              fontSize = 12.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Forecast Line
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NetTraceColors.Raised)
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (planForecast.willExceedLimit) NetTraceIcons.Warning else NetTraceIcons.Calendar,
              contentDescription = null,
              tint = if (planForecast.willExceedLimit) NetTraceColors.Warning else NetTraceColors.Mobile,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = if (planForecast.willExceedLimit && planForecast.estimatedExceedDate != null) {
                "At this pace you'll hit the limit on ${planForecast.estimatedExceedDate}"
              } else {
                "On track: Pacing is within your cycle data limit"
              },
              color = if (planForecast.willExceedLimit) NetTraceColors.Warning else NetTraceColors.TextSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Card 2: Today's Consumption (Stacked Wi-Fi vs Mobile Bar)
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
            text = "Today's Usage",
            color = NetTraceColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = UsageFormatters.formatBytes(todayTotals.grandTotalBytes),
            color = NetTraceColors.TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        StackedUsageBarCanvas(
          wifiBytes = todayTotals.totalWifiBytes,
          mobileBytes = todayTotals.totalMobileBytes
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NetTraceColors.Wifi)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Wi-Fi: ${UsageFormatters.formatBytes(todayTotals.totalWifiBytes)}",
              color = NetTraceColors.TextSecondary,
              fontSize = 12.sp
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(NetTraceColors.Mobile)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Mobile: ${UsageFormatters.formatBytes(todayTotals.totalMobileBytes)}",
              color = NetTraceColors.TextSecondary,
              fontSize = 12.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Card 3: 7-Day Sparkline Trend
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
          Column {
            Text(
              text = "7-Day Mobile Trend",
              color = NetTraceColors.TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "${UsageFormatters.formatBytes(sevenDaysTotals.totalMobileBytes)} total",
              color = NetTraceColors.TextTertiary,
              fontSize = 12.sp
            )
          }

          Text(
            text = "Avg ${UsageFormatters.formatBytes(sevenDaysTotals.totalMobileBytes / 7L)}/d",
            color = NetTraceColors.Mobile,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val points = sevenDaysHistory.takeLast(7).map { it.mobileBytes }
        SparklineCanvas(dataPoints = points)
      }
    }

    // Card 4: Dual SIM Split Donut (when 2 SIMs are active)
    if (isDualSim) {
      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(28.dp))
          .background(NetTraceColors.Surface)
          .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(28.dp))
          .pressScaleEffect()
          .padding(20.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          DualSimDonutCanvas(
            sim1Bytes = cycleTotals.totalMobileBytes * 2 / 3,
            sim2Bytes = cycleTotals.totalMobileBytes / 3,
            modifier = Modifier.size(90.dp)
          )

          Spacer(modifier = Modifier.width(18.dp))

          Column {
            Text(
              text = "Active Dual-SIM Split",
              color = NetTraceColors.TextPrimary,
              fontSize = 16.sp,
              fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Recorded since installation via hourly mobile deltas",
              color = NetTraceColors.TextTertiary,
              fontSize = 12.sp,
              lineHeight = 16.sp
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(80.dp)) // Padding above floating bottom bar
  }
}
