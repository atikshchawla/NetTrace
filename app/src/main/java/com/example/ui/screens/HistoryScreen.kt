package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.example.ui.components.NetTraceIcons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.components.D3LineGraphCanvas
import com.example.ui.components.HistoryBarChartCanvas
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion
import com.example.ui.theme.pressScaleEffect

enum class ChartType(val label: String) {
  LINE("Line"),
  BAR("Bar")
}

/**
 * NetTrace History Screen:
 * Multi-month historical telemetry inspector allowing users to browse past months/cycles,
 * interactive scrubbable D3/Recharts-style smooth monotone line graph or bar chart,
 * pace target line for current cycle, aggregation totals, and top application breakdown.
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
  var chartType by remember { mutableStateOf(ChartType.LINE) }
  val verticalScrollState = rememberScrollState()
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

    // 1. Previous & Next Month Stepper Card (< Month Year >)
    val currentIndex = availablePeriods.indexOfFirst { it.id == selectedPeriod.id }.coerceAtLeast(0)
    val canGoBack = currentIndex < availablePeriods.lastIndex
    val canGoForward = currentIndex > 0

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(24.dp))
        .background(NetTraceColors.Surface)
        .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(24.dp))
        .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Previous Month Button (<)
        IconButton(
          onClick = {
            if (canGoBack) {
              view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
              onSelectPeriod(availablePeriods[currentIndex + 1])
            }
          },
          enabled = canGoBack,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (canGoBack) NetTraceColors.Raised else Color.Transparent)
        ) {
          Icon(
            imageVector = NetTraceIcons.ChevronLeft,
            contentDescription = "Previous Month",
            tint = if (canGoBack) NetTraceColors.TextPrimary else NetTraceColors.TextTertiary.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
          )
        }

        // Center Month Title & Subtitle
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.weight(1f)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            if (selectedPeriod.isCurrent) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(NetTraceColors.Mobile)
              )
              Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
              text = selectedPeriod.title,
              color = NetTraceColors.TextPrimary,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(2.dp))

          Text(
            text = if (selectedPeriod.isCurrent) "Current Active Cycle" else "Completed Billing Period",
            color = if (selectedPeriod.isCurrent) NetTraceColors.Mobile else NetTraceColors.TextTertiary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }

        // Next Month Button (>)
        IconButton(
          onClick = {
            if (canGoForward) {
              view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
              onSelectPeriod(availablePeriods[currentIndex - 1])
            }
          },
          enabled = canGoForward,
          modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (canGoForward) NetTraceColors.Raised else Color.Transparent)
        ) {
          Icon(
            imageVector = NetTraceIcons.ChevronRight,
            contentDescription = "Next Month",
            tint = if (canGoForward) NetTraceColors.TextPrimary else NetTraceColors.TextTertiary.copy(alpha = 0.35f),
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Interactive Chart Card (Line Graph or Bar Chart)
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
              text = if (selectedPeriod.isCurrent) "Daily Consumption" else "${selectedPeriod.shortLabel} Daily Trend",
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

          // Line / Bar Chart Toggle
          Row(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(NetTraceColors.Raised)
              .border(1.dp, NetTraceColors.Hairline, RoundedCornerShape(12.dp))
              .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            ChartType.values().forEach { type ->
              val isTypeSelected = (chartType == type)
              val tabBg by animateColorAsState(
                targetValue = if (isTypeSelected) NetTraceColors.Wifi else NetTraceColors.Raised,
                animationSpec = tween(200),
                label = "chart_tab_bg_${type.name}"
              )

              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(tabBg)
                  .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                  ) {
                    chartType = type
                  }
                  .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = type.label,
                  color = if (isTypeSelected) NetTraceColors.Background else NetTraceColors.TextSecondary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Chart presentation with smooth cross-fade
        AnimatedContent(
          targetState = chartType,
          transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
          label = "chart_animation"
        ) { targetChart ->
          when (targetChart) {
            ChartType.LINE -> {
              D3LineGraphCanvas(
                dailyList = dailyList,
                dailyPaceBytes = if (selectedPeriod.isCurrent) dailyPaceBytes else 0L,
                onDaySelected = { selectedDay = it }
              )
            }
            ChartType.BAR -> {
              HistoryBarChartCanvas(
                dailyList = dailyList,
                dailyPaceBytes = if (selectedPeriod.isCurrent) dailyPaceBytes else 0L,
                onDaySelected = { selectedDay = it }
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
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
                text = "Total (Wi-Fi + Mobile)",
                color = NetTraceColors.TextSecondary,
                fontSize = 12.sp
              )
            }
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
                fontSize = 11.sp
              )
            }
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
      Spacer(modifier = Modifier.height(20.dp))

      Column(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "Top Apps for ${selectedPeriod.shortLabel}",
          color = NetTraceColors.TextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.3).sp
        )
        Text(
          text = "Highest data consumers during this billing period",
          color = NetTraceColors.TextTertiary,
          fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        val maxAppBytes = topAppsForPeriod.firstOrNull()?.totalBytes ?: 1L
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
