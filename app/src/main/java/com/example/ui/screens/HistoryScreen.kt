package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppUsageItem
import com.example.data.model.DailyUsage
import com.example.data.model.MonthPeriod
import com.example.data.model.UsageFormatters
import com.example.ui.components.AppUsageBarItem
import com.example.ui.components.HistoryBarChartCanvas
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion
import com.example.ui.theme.pressScaleEffect

/**
 * NetTrace History Screen:
 * Multi-month historical telemetry inspector allowing users to browse past months/cycles,
 * interactive scrubbable daily bar chart, pace target line for current cycle,
 * aggregation totals, and top application breakdown for the selected period.
 */
@Composable
fun HistoryScreen(
  availablePeriods: List<MonthPeriod>,
  selectedPeriod: MonthPeriod,
  onSelectPeriod: (MonthPeriod) -> Unit,
  dailyList: List<DailyUsage>,
  dailyPaceBytes: Long,
  planLimitBytes: Long = 0L,
  topAppsForPeriod: List<AppUsageItem> = emptyList(),
  modifier: Modifier = Modifier
) {
  var selectedDay by remember(selectedPeriod) { mutableStateOf<DailyUsage?>(null) }
  val verticalScrollState = rememberScrollState()
  val horizontalScrollState = rememberScrollState()
  val view = LocalView.current

  val totalMobile = dailyList.sumOf { it.mobileBytes }
  val totalWifi = dailyList.sumOf { it.wifiBytes }
  val activeDaysCount = dailyList.size.coerceAtLeast(1)
  val averageDaily = (totalMobile + totalWifi) / activeDaysCount

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NetTraceColors.Background)
      .verticalScroll(verticalScrollState)
      .padding(horizontal = 16.dp, vertical = 20.dp)
  ) {
    // Header
    Text(
      text = "Usage History",
      color = NetTraceColors.TextPrimary,
      fontSize = 34.sp,
      fontWeight = FontWeight.Bold,
      letterSpacing = (-0.5).sp
    )
    Text(
      text = "Inspect current and previous months' data consumption",
      color = NetTraceColors.TextTertiary,
      fontSize = 13.sp
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 1. Month / Billing Period Selector Pill Row
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(horizontalScrollState),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      availablePeriods.forEach { period ->
        val isSelected = (period.id == selectedPeriod.id)

        val pillBg by animateColorAsState(
          targetValue = if (isSelected) NetTraceColors.Raised else NetTraceColors.Surface,
          animationSpec = NetTraceMotion.springSpec(),
          label = "period_pill_bg_${period.id}"
        )
        val pillBorder by animateColorAsState(
          targetValue = if (isSelected) NetTraceColors.Wifi else NetTraceColors.Hairline,
          animationSpec = NetTraceMotion.springSpec(),
          label = "period_pill_border_${period.id}"
        )

        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pillBg)
            .border(if (isSelected) 1.5.dp else 1.dp, pillBorder, RoundedCornerShape(20.dp))
            .clickable(
              interactionSource = remember { MutableInteractionSource() },
              indication = null
            ) {
              if (!isSelected) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onSelectPeriod(period)
              }
            }
            .padding(horizontal = 16.dp, vertical = 9.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (period.isCurrent) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(NetTraceColors.Mobile)
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text(
            text = period.title,
            color = if (isSelected) NetTraceColors.TextPrimary else NetTraceColors.TextSecondary,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Scrubbable Bar Chart Card for the Selected Period
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
              text = if (selectedPeriod.isCurrent) "Daily Consumption" else "${selectedPeriod.shortLabel} Daily Breakdown",
              color = NetTraceColors.TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = "${dailyList.size} days recorded",
              color = NetTraceColors.TextTertiary,
              fontSize = 12.sp
            )
          }

          if (selectedPeriod.isCurrent && dailyPaceBytes > 0) {
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
          } else {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(NetTraceColors.Raised)
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "Historical",
                color = NetTraceColors.TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        HistoryBarChartCanvas(
          dailyList = dailyList,
          dailyPaceBytes = if (selectedPeriod.isCurrent) dailyPaceBytes else 0L,
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
              text = "Cellular",
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

    // 3. Scrubbed Day Inspection Card
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

    // 4. Period Aggregation Summary Card
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
          text = "${selectedPeriod.title} Cumulative",
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
              text = UsageFormatters.formatBytes(totalMobile),
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
              text = UsageFormatters.formatBytes(totalWifi),
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

        if (planLimitBytes > 0) {
          Spacer(modifier = Modifier.height(12.dp))
          val usagePercent = (totalMobile.toFloat() / planLimitBytes.toFloat()).coerceIn(0f, 2f)
          val percentInt = (usagePercent * 100f).toInt()

          Text(
            text = "Used $percentInt% of configured monthly allowance (${UsageFormatters.formatBytes(planLimitBytes)})",
            color = if (percentInt > 100) NetTraceColors.Critical else if (percentInt > 80) NetTraceColors.Warning else NetTraceColors.TextTertiary,
            fontSize = 11.sp
          )
        }
      }
    }

    // 5. Top Applications for the Selected Month (if available)
    if (topAppsForPeriod.isNotEmpty()) {
      Spacer(modifier = Modifier.height(16.dp))

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
            text = "Top Apps for ${selectedPeriod.shortLabel}",
            color = NetTraceColors.TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
          )
          Text(
            text = "Highest data consumers during this period",
            color = NetTraceColors.TextTertiary,
            fontSize = 11.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          val maxAppBytes = topAppsForPeriod.firstOrNull()?.totalBytes ?: 1L
          topAppsForPeriod.forEachIndexed { idx, app ->
            AppUsageBarItem(
              item = app,
              maxBytes = maxAppBytes,
              index = idx
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(80.dp)) // Clearance for bottom navigation bar
  }
}
