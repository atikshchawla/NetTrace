package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.data.model.AppUsageItem
import com.example.data.model.DailyUsage
import com.example.data.model.MonthPeriod
import com.example.data.model.PlanForecast
import com.example.data.model.SimInfo
import com.example.data.model.SimLogEntry
import com.example.data.model.UsageRange
import com.example.data.model.UsageTotals
import com.example.data.repository.DataStoreRepository
import com.example.data.repository.NetworkStatsRepository
import com.example.data.repository.SimManager
import com.example.ui.navigation.FloatingBottomNavBar
import com.example.ui.navigation.Screen
import com.example.ui.screens.AppsScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.NetTraceColors
import com.example.ui.theme.NetTraceTheme
import kotlinx.coroutines.launch

/**
 * NetTrace MainActivity:
 * Single-activity architecture hosting Home, Apps, History, and Settings.
 * Manages theme collection, runtime notification permission requests, multi-month historical inspection, and usage access.
 */
class MainActivity : ComponentActivity() {

  private lateinit var networkStatsRepo: NetworkStatsRepository
  private lateinit var simManager: SimManager
  private lateinit var dataStoreRepo: DataStoreRepository

  private val hasPermissionState = mutableStateOf(false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    networkStatsRepo = NetworkStatsRepository(this)
    simManager = SimManager(this)
    dataStoreRepo = DataStoreRepository(this)

    setContent {
      val themeMode by dataStoreRepo.themeModeFlow.collectAsState(initial = "system")
      val systemIsDark = isSystemInDarkTheme()
      val isDark = when (themeMode.lowercase()) {
        "dark" -> true
        "light" -> false
        else -> systemIsDark
      }

      NetTraceTheme(darkTheme = isDark) {
        NetTraceRootApp(
          hasUsagePermission = hasPermissionState.value,
          onRecheckPermission = { checkPermissionStatus() },
          networkStatsRepo = networkStatsRepo,
          simManager = simManager,
          dataStoreRepo = dataStoreRepo,
          themeMode = themeMode
        )
      }
    }
  }

  override fun onResume() {
    super.onResume()
    checkPermissionStatus()
  }

  private fun checkPermissionStatus() {
    hasPermissionState.value = networkStatsRepo.hasUsageAccessPermission()
  }
}

@Composable
fun NetTraceRootApp(
  hasUsagePermission: Boolean,
  onRecheckPermission: () -> Unit,
  networkStatsRepo: NetworkStatsRepository,
  simManager: SimManager,
  dataStoreRepo: DataStoreRepository,
  themeMode: String
) {
  val scope = rememberCoroutineScope()

  // Android 13+ Notification permission request launcher
  val notificationLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { _ -> }

  val context = androidx.compose.ui.platform.LocalContext.current

  // Proactively request notification permission on Android 13+ once usage access is granted
  LaunchedEffect(hasUsagePermission) {
    if (hasUsagePermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  if (!hasUsagePermission) {
    OnboardingScreen(onCheckPermissionAgain = onRecheckPermission)
    return
  }

  var currentScreen by remember { mutableStateOf(Screen.HOME) }
  var appsRange by remember { mutableStateOf(UsageRange.TODAY) }

  // Settings State
  val planSizeMbSim1 by dataStoreRepo.planSizeMbSim1Flow.collectAsState(initial = 10240L)
  val planSizeMbSim2 by dataStoreRepo.planSizeMbSim2Flow.collectAsState(initial = 10240L)
  val cycleResetDay by dataStoreRepo.cycleResetDayFlow.collectAsState(initial = 1)
  val alertThresholdPercent by dataStoreRepo.alertThresholdPercentFlow.collectAsState(initial = 80)
  val alertsEnabled by dataStoreRepo.alertsEnabledFlow.collectAsState(initial = true)

  // Telemetry state
  var todayTotals by remember { mutableStateOf(UsageTotals()) }
  var sevenDaysTotals by remember { mutableStateOf(UsageTotals()) }
  var cycleTotals by remember { mutableStateOf(UsageTotals()) }
  var planForecast by remember {
    mutableStateOf(
      PlanForecast(
        daysRemaining = 30,
        totalDaysInCycle = 30,
        currentUsageBytes = 0L,
        planLimitBytes = 10240L * 1024L * 1024L,
        dailyPaceBytes = 0L,
        projectedTotalBytes = 0L,
        willExceedLimit = false,
        estimatedExceedDate = null
      )
    )
  }
  var activeSims by remember { mutableStateOf<List<SimInfo>>(emptyList()) }
  var thirtyDaysHistory by remember { mutableStateOf<List<DailyUsage>>(emptyList()) }
  var simEntries by remember { mutableStateOf<List<SimLogEntry>>(emptyList()) }
  var appUsageList by remember { mutableStateOf<List<AppUsageItem>>(emptyList()) }

  // Multi-Month Historical Telemetry State
  val availablePeriods = remember { networkStatsRepo.getHistoricalMonthPeriods(6) }
  var selectedPeriod by remember { mutableStateOf(availablePeriods.first()) }
  var historyDailyList by remember { mutableStateOf<List<DailyUsage>>(emptyList()) }
  var historyTopApps by remember { mutableStateOf<List<AppUsageItem>>(emptyList()) }

  // Load telemetry when parameters change
  LaunchedEffect(hasUsagePermission, cycleResetDay, planSizeMbSim1, appsRange) {
    try {
      activeSims = simManager.getActiveSims()

      val (cycleStart, cycleEnd) = networkStatsRepo.calculateBillingCycleRange(cycleResetDay)
      val (todayStart, todayEnd) = networkStatsRepo.getRangeTimestamps(UsageRange.TODAY, cycleResetDay)
      val (sevenDaysStart, sevenDaysEnd) = networkStatsRepo.getRangeTimestamps(UsageRange.SEVEN_DAYS, cycleResetDay)

      todayTotals = networkStatsRepo.queryDeviceTotals(todayStart, todayEnd)
      sevenDaysTotals = networkStatsRepo.queryDeviceTotals(sevenDaysStart, sevenDaysEnd)
      cycleTotals = networkStatsRepo.queryDeviceTotals(cycleStart, cycleEnd)

      val planLimitBytes = planSizeMbSim1 * 1024L * 1024L
      planForecast = networkStatsRepo.computePlanForecast(
        cycleStartMs = cycleStart,
        cycleEndMs = cycleEnd,
        mobileUsageBytes = cycleTotals.totalMobileBytes,
        planLimitBytes = planLimitBytes
      )

      thirtyDaysHistory = networkStatsRepo.queryDailyHistory(30)
      simEntries = dataStoreRepo.getAllSimEntries()

      val (rangeStart, rangeEnd) = networkStatsRepo.getRangeTimestamps(appsRange, cycleResetDay)
      appUsageList = networkStatsRepo.queryAppUsage(rangeStart, rangeEnd, limit = 30)
    } catch (_: Exception) {}
  }

  // Load historical month data when selectedPeriod changes
  LaunchedEffect(selectedPeriod, hasUsagePermission) {
    if (hasUsagePermission) {
      try {
        historyDailyList = networkStatsRepo.queryDailyHistoryForPeriod(selectedPeriod)
        historyTopApps = networkStatsRepo.queryAppUsage(selectedPeriod.startTimeMs, selectedPeriod.endTimeMs, limit = 5)
      } catch (_: Exception) {}
    }
  }

  // Back handler for settings screen
  BackHandler(enabled = currentScreen != Screen.HOME) {
    currentScreen = Screen.HOME
  }

  Scaffold(
    containerColor = NetTraceColors.Background,
    bottomBar = {
      FloatingBottomNavBar(
        currentScreen = currentScreen,
        onScreenSelected = { currentScreen = it }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(NetTraceColors.Background)
    ) {
      AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
          // 300ms FastOutSlowIn navigation with an 8% slide
          val slideOffset = { fullWidth: Int -> (fullWidth * 0.08f).toInt() }
          (slideInHorizontally(animationSpec = tween(300), initialOffsetX = slideOffset) +
            fadeIn(animationSpec = tween(300))) togetherWith
            (slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -slideOffset(it) }) +
              fadeOut(animationSpec = tween(300)))
        },
        label = "screen_transition"
      ) { screen ->
        when (screen) {
          Screen.HOME -> HomeScreen(
            todayTotals = todayTotals,
            sevenDaysTotals = sevenDaysTotals,
            cycleTotals = cycleTotals,
            planForecast = planForecast,
            activeSims = activeSims,
            sevenDaysHistory = thirtyDaysHistory,
            onOpenSettings = { currentScreen = Screen.SETTINGS }
          )
          Screen.APPS -> AppsScreen(
            selectedRange = appsRange,
            onRangeSelected = { appsRange = it },
            appList = appUsageList
          )
          Screen.HISTORY -> HistoryScreen(
            availablePeriods = availablePeriods,
            selectedPeriod = selectedPeriod,
            onSelectPeriod = { selectedPeriod = it },
            dailyList = if (historyDailyList.isNotEmpty()) historyDailyList else thirtyDaysHistory,
            dailyPaceBytes = planForecast.dailyPaceBytes,
            planLimitBytes = planSizeMbSim1 * 1024L * 1024L,
            topAppsForPeriod = historyTopApps
          )
          Screen.SETTINGS -> SettingsScreen(
            themeMode = themeMode,
            planSizeMbSim1 = planSizeMbSim1,
            planSizeMbSim2 = planSizeMbSim2,
            cycleResetDay = cycleResetDay,
            alertThresholdPercent = alertThresholdPercent,
            alertsEnabled = alertsEnabled,
            activeSims = activeSims,
            dailyList = thirtyDaysHistory,
            simEntries = simEntries,
            appList = appUsageList,
            onUpdateThemeMode = { scope.launch { dataStoreRepo.setThemeMode(it) } },
            onUpdatePlanSizeSim1 = { scope.launch { dataStoreRepo.updatePlanSizeSim1(it) } },
            onUpdatePlanSizeSim2 = { scope.launch { dataStoreRepo.updatePlanSizeSim2(it) } },
            onUpdateResetDay = { scope.launch { dataStoreRepo.updateCycleResetDay(it) } },
            onUpdateThreshold = { scope.launch { dataStoreRepo.updateAlertThreshold(it) } },
            onToggleAlerts = { scope.launch { dataStoreRepo.setAlertsEnabled(it) } },
            onNavigateBack = { currentScreen = Screen.HOME }
          )
        }
      }
    }
  }
}
