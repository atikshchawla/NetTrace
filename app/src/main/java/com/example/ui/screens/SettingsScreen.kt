package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.export.CsvExporter
import com.example.data.model.AppUsageItem
import com.example.data.model.DailyUsage
import com.example.data.model.SimInfo
import com.example.data.model.SimLogEntry
import com.example.ui.components.NetTraceIcons
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceMotion
import kotlinx.coroutines.launch

/**
 * NetTrace Settings Screen:
 * Configures per-SIM plan limits, billing cycle reset day, alert threshold %,
 * runtime notification permission handling, CSV telemetry export,
 * and appearance/theme options arranged horizontally as the final section.
 */
@Composable
fun SettingsScreen(
  themeMode: String,
  planSizeMbSim1: Long,
  planSizeMbSim2: Long,
  cycleResetDay: Int,
  alertThresholdPercent: Int,
  alertsEnabled: Boolean,
  dailyLimitMb: Long = 0L,
  dailyAlertsEnabled: Boolean = false,
  activeSims: List<SimInfo>,
  dailyList: List<DailyUsage>,
  simEntries: List<SimLogEntry>,
  appList: List<AppUsageItem>,
  onUpdateThemeMode: (String) -> Unit,
  onUpdatePlanSizeSim1: (Long) -> Unit,
  onUpdatePlanSizeSim2: (Long) -> Unit,
  onUpdateResetDay: (Int) -> Unit,
  onUpdateThreshold: (Int) -> Unit,
  onToggleAlerts: (Boolean) -> Unit,
  onUpdateDailyLimit: (Long) -> Unit = {},
  onToggleDailyAlerts: (Boolean) -> Unit = {},
  onNavigateBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val scrollState = rememberScrollState()

  // Track runtime notification permission status
  var hasNotificationPermission by remember {
    mutableStateOf(
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
      } else {
        true
      }
    )
  }

  val notificationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasNotificationPermission = isGranted
    if (isGranted) {
      Toast.makeText(context, "Notifications enabled", Toast.LENGTH_SHORT).show()
      onToggleAlerts(true)
    } else {
      Toast.makeText(context, "Notification permission is required for threshold alerts", Toast.LENGTH_LONG).show()
    }
  }

  var sim1InputGb by remember(planSizeMbSim1) {
    mutableStateOf(String.format("%.1f", planSizeMbSim1 / 1024.0))
  }
  var sim2InputGb by remember(planSizeMbSim2) {
    mutableStateOf(String.format("%.1f", planSizeMbSim2 / 1024.0))
  }
  var resetDayInput by remember(cycleResetDay) {
    mutableStateOf(cycleResetDay.toString())
  }
  var thresholdSlider by remember(alertThresholdPercent) {
    mutableFloatStateOf(alertThresholdPercent.toFloat())
  }

  // Document creation launcher for zero-permission CSV export
  val csvLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("text/csv")
  ) { uri: Uri? ->
    if (uri != null) {
      scope.launch {
        val exporter = CsvExporter(context)
        val content = exporter.generateCsvContent(dailyList, simEntries, appList)
        val success = exporter.writeToUri(uri, content)
        if (success) {
          Toast.makeText(context, "Exported successfully", Toast.LENGTH_SHORT).show()
        } else {
          Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(NetTraceColors.Background)
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 20.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(
        onClick = onNavigateBack,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(NetTraceColors.Surface)
          .border(1.dp, NetTraceColors.Hairline, CircleShape)
      ) {
        Icon(
          imageVector = NetTraceIcons.ArrowBack,
          contentDescription = "Back",
          tint = NetTraceColors.TextPrimary,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column {
        Text(
          text = "Settings",
          color = NetTraceColors.TextPrimary,
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Data plans, alerts & preferences",
          color = NetTraceColors.TextTertiary,
          fontSize = 12.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Section 1: Cellular Plan Sizes
    SettingsCard(title = "Data Plans") {
      Text(
        text = "SIM 1 Monthly Allowance (GB)",
        color = NetTraceColors.TextSecondary,
        fontSize = 13.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = sim1InputGb,
        onValueChange = { input ->
          sim1InputGb = input
          input.toDoubleOrNull()?.let { gb ->
            onUpdatePlanSizeSim1((gb * 1024.0).toLong())
          }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = NetTraceColors.TextPrimary,
          unfocusedTextColor = NetTraceColors.TextPrimary,
          focusedBorderColor = NetTraceColors.Wifi,
          unfocusedBorderColor = NetTraceColors.Hairline,
          focusedContainerColor = NetTraceColors.Raised,
          unfocusedContainerColor = NetTraceColors.Raised
        ),
        shape = RoundedCornerShape(14.dp)
      )

      if (activeSims.size >= 2) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "SIM 2 Monthly Allowance (GB)",
          color = NetTraceColors.TextSecondary,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
          value = sim2InputGb,
          onValueChange = { input ->
            sim2InputGb = input
            input.toDoubleOrNull()?.let { gb ->
              onUpdatePlanSizeSim2((gb * 1024.0).toLong())
            }
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = NetTraceColors.TextPrimary,
            unfocusedTextColor = NetTraceColors.TextPrimary,
            focusedBorderColor = NetTraceColors.Sim2,
            unfocusedBorderColor = NetTraceColors.Hairline,
            focusedContainerColor = NetTraceColors.Raised,
            unfocusedContainerColor = NetTraceColors.Raised
          ),
          shape = RoundedCornerShape(14.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Billing Cycle Reset Day (1-28)",
        color = NetTraceColors.TextSecondary,
        fontSize = 13.sp
      )
      Spacer(modifier = Modifier.height(6.dp))
      OutlinedTextField(
        value = resetDayInput,
        onValueChange = { input ->
          resetDayInput = input
          input.toIntOrNull()?.let { day ->
            if (day in 1..28) {
              onUpdateResetDay(day)
            }
          }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = NetTraceColors.TextPrimary,
          unfocusedTextColor = NetTraceColors.TextPrimary,
          focusedBorderColor = NetTraceColors.Wifi,
          unfocusedBorderColor = NetTraceColors.Hairline,
          focusedContainerColor = NetTraceColors.Raised,
          unfocusedContainerColor = NetTraceColors.Raised
        ),
        shape = RoundedCornerShape(14.dp)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Section 2: Threshold Alerts & Notification Permission
    SettingsCard(title = "Alerts & Notifications") {
      // Permission Banner if permission missing on Android 13+
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NetTraceColors.Warning.copy(alpha = 0.12f))
            .border(1.dp, NetTraceColors.Warning.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(14.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = NetTraceIcons.Warning,
                contentDescription = null,
                tint = NetTraceColors.Warning,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Notification Permission Needed",
                color = NetTraceColors.Warning,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = "Android requires notification permission to post alerts when your mobile data reaches your threshold.",
              color = NetTraceColors.TextSecondary,
              fontSize = 12.sp,
              lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = NetTraceColors.Warning,
                contentColor = NetTraceColors.Background
              ),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.height(36.dp)
            ) {
              Text(
                text = "Grant Notification Permission",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Threshold Notification",
            color = NetTraceColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "Post heads-up notification when cycle usage reaches threshold",
            color = NetTraceColors.TextTertiary,
            fontSize = 12.sp
          )
        }
        Switch(
          checked = alertsEnabled,
          onCheckedChange = { enable ->
            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
              notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              onToggleAlerts(enable)
            }
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = NetTraceColors.TextPrimary,
            checkedTrackColor = NetTraceColors.Wifi,
            uncheckedThumbColor = NetTraceColors.TextTertiary,
            uncheckedTrackColor = NetTraceColors.Raised
          )
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Alert Threshold",
          color = NetTraceColors.TextSecondary,
          fontSize = 13.sp
        )
        Text(
          text = "${thresholdSlider.toInt()}%",
          color = NetTraceColors.Warning,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Slider(
        value = thresholdSlider,
        onValueChange = {
          thresholdSlider = it
          onUpdateThreshold(it.toInt())
        },
        valueRange = 50f..100f,
        steps = 9,
        enabled = alertsEnabled,
        colors = SliderDefaults.colors(
          thumbColor = NetTraceColors.Warning,
          activeTrackColor = NetTraceColors.Warning,
          inactiveTrackColor = NetTraceColors.Raised
        )
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Hourly background worker samples mobile data. When your billing cycle usage crosses ${thresholdSlider.toInt()}%, NetTrace posts a notification so you avoid unexpected overage charges.",
        color = NetTraceColors.TextTertiary,
        fontSize = 11.sp,
        lineHeight = 15.sp
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Section 2B: Daily Cellular Budget & Overage Warnings
    var localDailyLimitMb by remember(dailyLimitMb) {
      mutableStateOf(if (dailyLimitMb > 0L) dailyLimitMb.toString() else "1024")
    }

    SettingsCard(title = "Daily Cellular Budget") {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Daily Overage Warning",
            color = NetTraceColors.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "Alert immediately when today's mobile usage exceeds quota",
            color = NetTraceColors.TextTertiary,
            fontSize = 12.sp
          )
        }
        Switch(
          checked = dailyAlertsEnabled,
          onCheckedChange = { enable ->
            if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
              notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              onToggleDailyAlerts(enable)
              if (enable && dailyLimitMb <= 0L) {
                onUpdateDailyLimit(1024L) // Default 1 GB daily
              }
            }
          },
          colors = SwitchDefaults.colors(
            checkedThumbColor = NetTraceColors.TextPrimary,
            checkedTrackColor = NetTraceColors.Mobile,
            uncheckedThumbColor = NetTraceColors.TextTertiary,
            uncheckedTrackColor = NetTraceColors.Raised
          )
        )
      }

      if (dailyAlertsEnabled) {
        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Daily Limit Allowance",
          color = NetTraceColors.TextSecondary,
          fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Presets Row
        val presets = listOf(500L, 1024L, 1536L, 2048L, 3072L)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          presets.forEach { presetMb ->
            val isSelected = (dailyLimitMb == presetMb)
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) NetTraceColors.Mobile else NetTraceColors.Raised)
                .border(
                  width = 1.dp,
                  color = if (isSelected) NetTraceColors.Mobile else NetTraceColors.Hairline,
                  shape = RoundedCornerShape(10.dp)
                )
                .clickable {
                  localDailyLimitMb = presetMb.toString()
                  onUpdateDailyLimit(presetMb)
                }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = if (presetMb >= 1024L) "${presetMb / 1024} GB" else "${presetMb} MB",
                color = if (isSelected) NetTraceColors.Background else NetTraceColors.TextPrimary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = localDailyLimitMb,
          onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }
            localDailyLimitMb = filtered
            filtered.toLongOrNull()?.let { onUpdateDailyLimit(it) }
          },
          label = { Text("Custom Daily Limit (MB)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NetTraceColors.Mobile,
            unfocusedBorderColor = NetTraceColors.Hairline,
            focusedTextColor = NetTraceColors.TextPrimary,
            unfocusedTextColor = NetTraceColors.TextPrimary,
            focusedLabelColor = NetTraceColors.Mobile,
            unfocusedLabelColor = NetTraceColors.TextTertiary
          ),
          shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "NetTrace checks hourly and sends an alert when your phone reaches this threshold in a single day.",
          color = NetTraceColors.TextTertiary,
          fontSize = 11.sp,
          lineHeight = 15.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Section 3: CSV Export
    SettingsCard(title = "Data Management") {
      Text(
        text = "Export Telemetry to CSV",
        color = NetTraceColors.TextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Export historical usage, dual-SIM breakdown, and top applications as a spreadsheet.",
        color = NetTraceColors.TextTertiary,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      Spacer(modifier = Modifier.height(14.dp))

      Button(
        onClick = {
          val filename = "NetTrace_Export_${System.currentTimeMillis()}.csv"
          csvLauncher.launch(filename)
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = NetTraceColors.Raised,
          contentColor = NetTraceColors.TextPrimary
        ),
        shape = RoundedCornerShape(14.dp)
      ) {
        Icon(
          imageVector = NetTraceIcons.Export,
          contentDescription = null,
          tint = NetTraceColors.Wifi,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Export CSV Document",
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Section 4: Appearance & Theme (Positioned LAST, with 3 Horizontal Option Cards)
    SettingsCard(title = "Appearance & Theme") {
      Text(
        text = "Select your preferred appearance mode:",
        color = NetTraceColors.TextSecondary,
        fontSize = 13.sp
      )

      Spacer(modifier = Modifier.height(14.dp))

      val themes = listOf(
        ThemeOption(
          id = "system",
          title = "System",
          subtitle = "Default",
          icon = NetTraceIcons.Cellular
        ),
        ThemeOption(
          id = "dark",
          title = "Dark",
          subtitle = "OLED Black",
          icon = NetTraceIcons.Moon
        ),
        ThemeOption(
          id = "light",
          title = "Light",
          subtitle = "Quiet Light",
          icon = NetTraceIcons.Sun
        )
      )

      // 3 Horizontal Option Cards in a single responsive Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        themes.forEach { option ->
          val isSelected = (themeMode.lowercase() == option.id)

          val itemBg by animateColorAsState(
            targetValue = if (isSelected) NetTraceColors.Raised else NetTraceColors.Surface,
            animationSpec = NetTraceMotion.springSpec(),
            label = "theme_card_bg_${option.id}"
          )
          val itemBorder by animateColorAsState(
            targetValue = if (isSelected) NetTraceColors.Wifi else NetTraceColors.Hairline,
            animationSpec = NetTraceMotion.springSpec(),
            label = "theme_card_border_${option.id}"
          )

          Column(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(16.dp))
              .background(itemBg)
              .border(if (isSelected) 1.5.dp else 1.dp, itemBorder, RoundedCornerShape(16.dp))
              .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
              ) {
                onUpdateThemeMode(option.id)
              }
              .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) NetTraceColors.Wifi.copy(alpha = 0.18f) else NetTraceColors.Raised),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = option.icon,
                contentDescription = option.title,
                tint = if (isSelected) NetTraceColors.Wifi else NetTraceColors.TextSecondary,
                modifier = Modifier.size(18.dp)
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = option.title,
              color = if (isSelected) NetTraceColors.TextPrimary else NetTraceColors.TextSecondary,
              fontSize = 13.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              textAlign = TextAlign.Center
            )

            Text(
              text = option.subtitle,
              color = if (isSelected) NetTraceColors.Wifi else NetTraceColors.TextTertiary,
              fontSize = 10.sp,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Radio dot indicator
            Box(
              modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(
                  width = 1.5.dp,
                  color = if (isSelected) NetTraceColors.Wifi else NetTraceColors.TextTertiary,
                  shape = CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              if (isSelected) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NetTraceColors.Wifi)
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(80.dp)) // Clearance for bottom navigation bar
  }
}

private data class ThemeOption(
  val id: String,
  val title: String,
  val subtitle: String,
  val icon: ImageVector
)

@Composable
private fun SettingsCard(
  title: String,
  content: @Composable () -> Unit
) {
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
        text = title,
        color = NetTraceColors.TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.height(14.dp))
      content()
    }
  }
}
