package com.example.data.repository

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import com.example.data.model.AppUsageItem
import com.example.data.model.DailyUsage
import com.example.data.model.MonthPeriod
import com.example.data.model.PlanForecast
import com.example.data.model.UsageRange
import com.example.data.model.UsageTotals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Handles NetworkStatsManager queries for Wi-Fi and Mobile device totals,
 * per-UID application breakdown with icon/label resolution,
 * billing cycle date math, and 30-day historical daily buckets.
 */
class NetworkStatsRepository(private val context: Context) {

  private val networkStatsManager: NetworkStatsManager? by lazy {
    context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
  }

  private val packageManager: PackageManager by lazy {
    context.packageManager
  }

  /**
   * Verifies if the user has granted Usage Access (PACKAGE_USAGE_STATS).
   */
  fun hasUsageAccessPermission(): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      appOps.unsafeCheckOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
      )
    } else {
      @Suppress("DEPRECATION")
      appOps.checkOpNoThrow(
        AppOpsManager.OPSTR_GET_USAGE_STATS,
        Process.myUid(),
        context.packageName
      )
    }
    return mode == AppOpsManager.MODE_ALLOWED
  }

  /**
   * Queries total mobile and Wi-Fi data for a time range via querySummaryForDevice.
   */
  suspend fun queryDeviceTotals(startTime: Long, endTime: Long): UsageTotals = withContext(Dispatchers.IO) {
    val nsm = networkStatsManager ?: return@withContext UsageTotals()
    if (!hasUsageAccessPermission()) return@withContext UsageTotals()

    var wifiRx = 0L
    var wifiTx = 0L
    var mobileRx = 0L
    var mobileTx = 0L

    try {
      val wifiBucket = nsm.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
      wifiRx = wifiBucket.rxBytes
      wifiTx = wifiBucket.txBytes
    } catch (_: Exception) {}

    try {
      val mobileBucket = nsm.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime)
      mobileRx = mobileBucket.rxBytes
      mobileTx = mobileBucket.txBytes
    } catch (_: Exception) {}

    UsageTotals(
      wifiRxBytes = wifiRx,
      wifiTxBytes = wifiTx,
      mobileRxBytes = mobileRx,
      mobileTxBytes = mobileTx
    )
  }

  /**
   * Queries top apps by usage for the specified time range.
   * Groups shared UIDs and resolves app icons and labels.
   */
  suspend fun queryAppUsage(startTime: Long, endTime: Long, limit: Int = 20): List<AppUsageItem> = withContext(Dispatchers.IO) {
    val nsm = networkStatsManager ?: return@withContext emptyList()
    if (!hasUsageAccessPermission()) return@withContext emptyList()

    val uidWifiMap = mutableMapOf<Int, Long>()
    val uidMobileMap = mutableMapOf<Int, Long>()

    // Wi-Fi per UID
    try {
      val wifiStats: NetworkStats = nsm.querySummary(ConnectivityManager.TYPE_WIFI, null, startTime, endTime)
      val bucket = NetworkStats.Bucket()
      while (wifiStats.hasNextBucket()) {
        wifiStats.getNextBucket(bucket)
        val uid = bucket.uid
        val bytes = bucket.rxBytes + bucket.txBytes
        if (bytes > 0L) {
          uidWifiMap[uid] = (uidWifiMap[uid] ?: 0L) + bytes
        }
      }
      wifiStats.close()
    } catch (_: Exception) {}

    // Mobile per UID
    try {
      val mobileStats: NetworkStats = nsm.querySummary(ConnectivityManager.TYPE_MOBILE, null, startTime, endTime)
      val bucket = NetworkStats.Bucket()
      while (mobileStats.hasNextBucket()) {
        mobileStats.getNextBucket(bucket)
        val uid = bucket.uid
        val bytes = bucket.rxBytes + bucket.txBytes
        if (bytes > 0L) {
          uidMobileMap[uid] = (uidMobileMap[uid] ?: 0L) + bytes
        }
      }
      mobileStats.close()
    } catch (_: Exception) {}

    val allUids = (uidWifiMap.keys + uidMobileMap.keys)
    val appItems = mutableListOf<AppUsageItem>()

    for (uid in allUids) {
      val wifiBytes = uidWifiMap[uid] ?: 0L
      val mobileBytes = uidMobileMap[uid] ?: 0L
      val totalBytes = wifiBytes + mobileBytes
      if (totalBytes <= 0L) continue

      val details = resolveUidAppDetails(uid)
      appItems.add(
        AppUsageItem(
          uid = uid,
          packageName = details.packageName,
          appName = details.appName,
          icon = details.icon,
          wifiBytes = wifiBytes,
          mobileBytes = mobileBytes,
          isSystem = details.isSystem,
          categoryLabel = details.categoryLabel
        )
      )
    }

    // Sort descending by total usage and take top N
    appItems.sortedByDescending { it.totalBytes }.take(limit)
  }

  private data class ResolvedApp(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable? = null,
    val isSystem: Boolean = false,
    val categoryLabel: String = ""
  )

  /**
   * Maps a Linux UID to app name, package name, launcher icon, and system category.
   * Handles Android system UIDs, tethering, uninstalled apps, and third-party packages.
   */
  private fun resolveUidAppDetails(uid: Int): ResolvedApp {
    when (uid) {
      0 -> return ResolvedApp("android.kernel", "Android Kernel & OS", null, isSystem = true, categoryLabel = "Kernel")
      1000 -> return ResolvedApp("android.system", "System Framework", null, isSystem = true, categoryLabel = "System")
      1001 -> return ResolvedApp("com.android.phone", "Phone & Cellular Radio", null, isSystem = true, categoryLabel = "Cellular")
      1002 -> return ResolvedApp("com.android.bluetooth", "Bluetooth Stack", null, isSystem = true, categoryLabel = "Bluetooth")
      1013 -> return ResolvedApp("android.media", "Media & Audio Services", null, isSystem = true, categoryLabel = "Audio")
      1020 -> return ResolvedApp("com.android.nfc", "NFC Services", null, isSystem = true, categoryLabel = "NFC")
      1052, 1073 -> return ResolvedApp("com.android.networkstack", "Network Stack & Hotspot", null, isSystem = true, categoryLabel = "Network")
      -4, 10044 -> return ResolvedApp("android.uid.removed", "Uninstalled Applications", null, isSystem = false, categoryLabel = "Removed")
      -5 -> return ResolvedApp("android.uid.tethering", "Personal Hotspot & Tethering", null, isSystem = true, categoryLabel = "Hotspot")
    }

    val packages = packageManager.getPackagesForUid(uid)
    if (packages.isNullOrEmpty()) {
      return if (uid < 10000) {
        ResolvedApp("system.daemon.$uid", "System Service ($uid)", null, isSystem = true, categoryLabel = "System")
      } else {
        ResolvedApp("app.removed.$uid", "Removed App ($uid)", null, isSystem = false, categoryLabel = "Removed")
      }
    }

    val primaryPkg = packages[0]
    return try {
      val appInfo = packageManager.getApplicationInfo(primaryPkg, 0)
      val label = packageManager.getApplicationLabel(appInfo).toString()
      val icon = packageManager.getApplicationIcon(appInfo)
      val isSys = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
      ResolvedApp(
        packageName = primaryPkg,
        appName = if (label.isNotBlank()) label else primaryPkg,
        icon = icon,
        isSystem = isSys,
        categoryLabel = if (isSys) "System" else "App"
      )
    } catch (_: Exception) {
      val isSys = uid < 10000
      ResolvedApp(primaryPkg, primaryPkg, null, isSystem = isSys, categoryLabel = if (isSys) "System" else "App")
    }
  }

  /**
   * Generates a list of the last [monthsCount] calendar months for historical review.
   */
  fun getHistoricalMonthPeriods(monthsCount: Int = 6): List<MonthPeriod> {
    val list = mutableListOf<MonthPeriod>()
    val cal = Calendar.getInstance()
    val now = System.currentTimeMillis()

    for (i in 0 until monthsCount) {
      val monthCal = (cal.clone() as Calendar).apply {
        add(Calendar.MONTH, -i)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val startMs = monthCal.timeInMillis
      val maxDays = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
      val endCal = (monthCal.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, maxDays)
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
      }
      val endMs = if (i == 0) now else endCal.timeInMillis

      val monthTitleFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
      val shortFormat = SimpleDateFormat("MMM", Locale.getDefault())
      val idFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

      val title = if (i == 0) "This Month (${shortFormat.format(monthCal.time)})" else monthTitleFormat.format(monthCal.time)
      val shortLabel = shortFormat.format(monthCal.time)
      val id = idFormat.format(monthCal.time)

      list.add(
        MonthPeriod(
          id = id,
          title = title,
          shortLabel = shortLabel,
          startTimeMs = startMs,
          endTimeMs = endMs,
          isCurrent = (i == 0)
        )
      )
    }
    return list
  }

  /**
   * Queries day-by-day usage for a specific historical month or time period.
   */
  suspend fun queryDailyHistoryForPeriod(period: MonthPeriod): List<DailyUsage> = withContext(Dispatchers.IO) {
    if (!hasUsageAccessPermission()) return@withContext emptyList()
    val result = mutableListOf<DailyUsage>()
    val cur = Calendar.getInstance().apply {
      timeInMillis = period.startTimeMs
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val endCal = Calendar.getInstance().apply {
      timeInMillis = period.endTimeMs
    }

    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    while (!cur.after(endCal)) {
      val startOfDay = cur.timeInMillis
      val endOfDay = (startOfDay + TimeUnit.DAYS.toMillis(1) - 1L).coerceAtMost(period.endTimeMs)

      val totals = queryDeviceTotals(startOfDay, endOfDay)
      val epochDay = TimeUnit.MILLISECONDS.toDays(startOfDay)
      val label = dateFormat.format(Date(startOfDay))

      result.add(
        DailyUsage(
          epochDay = epochDay,
          dateLabel = label,
          wifiBytes = totals.totalWifiBytes,
          mobileBytes = totals.totalMobileBytes
        )
      )
      cur.add(Calendar.DAY_OF_MONTH, 1)
    }

    result
  }

  /**
   * Computes billing cycle start and end timestamps from a configured reset day (1-28).
   * Handles cross-month transitions safely.
   */
  fun calculateBillingCycleRange(resetDay: Int): Pair<Long, Long> {
    val day = resetDay.coerceIn(1, 28)
    val now = Calendar.getInstance()

    val cycleStart = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    val currentDayOfMonth = now.get(Calendar.DAY_OF_MONTH)
    if (currentDayOfMonth >= day) {
      // Current cycle started this month on resetDay
      cycleStart.set(Calendar.DAY_OF_MONTH, day)
    } else {
      // Current cycle started last month on resetDay
      cycleStart.add(Calendar.MONTH, -1)
      cycleStart.set(Calendar.DAY_OF_MONTH, day)
    }

    val cycleEnd = (cycleStart.clone() as Calendar).apply {
      add(Calendar.MONTH, 1)
      add(Calendar.MILLISECOND, -1)
    }

    return Pair(cycleStart.timeInMillis, cycleEnd.timeInMillis)
  }

  /**
   * Generates timestamp bounds for UsageRange.
   */
  fun getRangeTimestamps(range: UsageRange, resetDay: Int): Pair<Long, Long> {
    val now = System.currentTimeMillis()
    return when (range) {
      UsageRange.TODAY -> {
        val cal = Calendar.getInstance().apply {
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        Pair(cal.timeInMillis, now)
      }
      UsageRange.SEVEN_DAYS -> {
        val cal = Calendar.getInstance().apply {
          add(Calendar.DAY_OF_YEAR, -6)
          set(Calendar.HOUR_OF_DAY, 0)
          set(Calendar.MINUTE, 0)
          set(Calendar.SECOND, 0)
          set(Calendar.MILLISECOND, 0)
        }
        Pair(cal.timeInMillis, now)
      }
      UsageRange.CYCLE -> {
        val (start, _) = calculateBillingCycleRange(resetDay)
        Pair(start, now)
      }
    }
  }

  /**
   * Queries 30-day daily usage breakdown for the History screen.
   */
  suspend fun queryDailyHistory(days: Int = 30): List<DailyUsage> = withContext(Dispatchers.IO) {
    if (!hasUsageAccessPermission()) return@withContext emptyList()
    val result = mutableListOf<DailyUsage>()
    val cal = Calendar.getInstance()

    val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    for (i in (days - 1) downTo 0) {
      val dayCal = (cal.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -i)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      val startOfDay = dayCal.timeInMillis
      val endOfDay = startOfDay + TimeUnit.DAYS.toMillis(1) - 1L

      val totals = queryDeviceTotals(startOfDay, endOfDay)
      val epochDay = TimeUnit.MILLISECONDS.toDays(startOfDay)
      val label = dateFormat.format(Date(startOfDay))

      result.add(
        DailyUsage(
          epochDay = epochDay,
          dateLabel = label,
          wifiBytes = totals.totalWifiBytes,
          mobileBytes = totals.totalMobileBytes
        )
      )
    }

    result
  }

  /**
   * Computes the data plan pacing forecast.
   */
  fun computePlanForecast(
    cycleStartMs: Long,
    cycleEndMs: Long,
    mobileUsageBytes: Long,
    planLimitBytes: Long
  ): PlanForecast {
    val now = System.currentTimeMillis()
    val elapsedMs = (now - cycleStartMs).coerceAtLeast(1L)
    val totalCycleMs = (cycleEndMs - cycleStartMs).coerceAtLeast(1L)
    val remainingMs = (cycleEndMs - now).coerceAtLeast(0L)

    val elapsedDays = (elapsedMs.toDouble() / TimeUnit.DAYS.toMillis(1).toDouble()).coerceAtLeast(0.1)
    val totalDaysInCycle = TimeUnit.MILLISECONDS.toDays(totalCycleMs).toInt().coerceAtLeast(28)
    val daysRemaining = TimeUnit.MILLISECONDS.toDays(remainingMs).toInt().coerceAtLeast(0)

    val dailyPace = (mobileUsageBytes.toDouble() / elapsedDays).toLong()
    val projectedTotal = mobileUsageBytes + (dailyPace * daysRemaining)
    val willExceed = (planLimitBytes > 0) && (projectedTotal > planLimitBytes)

    var estimatedDateStr: String? = null
    if (willExceed && dailyPace > 0) {
      val bytesLeft = (planLimitBytes - mobileUsageBytes).coerceAtLeast(0L)
      val daysUntilLimit = (bytesLeft.toDouble() / dailyPace.toDouble()).toLong()
      val exceedCal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysUntilLimit.toInt())
      }
      val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
      estimatedDateStr = sdf.format(exceedCal.time)
    }

    return PlanForecast(
      daysRemaining = daysRemaining,
      totalDaysInCycle = totalDaysInCycle,
      currentUsageBytes = mobileUsageBytes,
      planLimitBytes = planLimitBytes,
      dailyPaceBytes = dailyPace,
      projectedTotalBytes = projectedTotal,
      willExceedLimit = willExceed,
      estimatedExceedDate = estimatedDateStr
    )
  }
}
