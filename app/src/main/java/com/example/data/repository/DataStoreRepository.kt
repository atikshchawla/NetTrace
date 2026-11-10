package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.SimLogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nettrace_settings")

/**
 * DataStore repository managing plan limits, reset day, threshold alerts,
 * and the compact 60-day per-SIM delta usage log.
 */
class DataStoreRepository(private val context: Context) {

  private object Keys {
    val PLAN_SIZE_MB_SIM1 = longPreferencesKey("plan_size_mb_sim1")
    val PLAN_SIZE_MB_SIM2 = longPreferencesKey("plan_size_mb_sim2")
    val CYCLE_RESET_DAY = intPreferencesKey("cycle_reset_day")
    val ALERT_THRESHOLD_PERCENT = intPreferencesKey("alert_threshold_percent")
    val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
    val LAST_MOBILE_BYTES_SAMPLE = longPreferencesKey("last_mobile_bytes_sample")
    val LAST_SAMPLE_TIMESTAMP = longPreferencesKey("last_sample_timestamp")
    val LAST_ALERTED_CYCLE_KEY = stringPreferencesKey("last_alerted_cycle_key")
    val SIM_DAILY_LOG = stringPreferencesKey("sim_daily_log")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DAILY_LIMIT_MB = longPreferencesKey("daily_limit_mb")
    val DAILY_ALERTS_ENABLED = booleanPreferencesKey("daily_alerts_enabled")
    val LAST_ALERTED_DAILY_DAY = longPreferencesKey("last_alerted_daily_day")
    val APP_LIMITS = stringPreferencesKey("app_limits")
    val LAST_ALERTED_APP_LIMITS = stringPreferencesKey("last_alerted_app_limits")
  }

  val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[Keys.THEME_MODE] ?: "system" // "system", "dark", "light" - default is system mode
  }

  val dailyLimitMbFlow: Flow<Long> = context.dataStore.data.map { preferences ->
    preferences[Keys.DAILY_LIMIT_MB] ?: 0L // 0L means disabled / no limit
  }

  val dailyAlertsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[Keys.DAILY_ALERTS_ENABLED] ?: false
  }

  val appLimitsFlow: Flow<Map<String, Long>> = context.dataStore.data.map { preferences ->
    val raw = preferences[Keys.APP_LIMITS] ?: ""
    parseAppLimits(raw)
  }

  val planSizeMbSim1Flow: Flow<Long> = context.dataStore.data.map { preferences ->
    preferences[Keys.PLAN_SIZE_MB_SIM1] ?: 10240L // Default 10 GB
  }

  val planSizeMbSim2Flow: Flow<Long> = context.dataStore.data.map { preferences ->
    preferences[Keys.PLAN_SIZE_MB_SIM2] ?: 10240L // Default 10 GB
  }

  val cycleResetDayFlow: Flow<Int> = context.dataStore.data.map { preferences ->
    preferences[Keys.CYCLE_RESET_DAY] ?: 1 // Default 1st of month
  }

  val alertThresholdPercentFlow: Flow<Int> = context.dataStore.data.map { preferences ->
    preferences[Keys.ALERT_THRESHOLD_PERCENT] ?: 80 // Default 80%
  }

  val alertsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[Keys.ALERTS_ENABLED] ?: true
  }

  suspend fun setThemeMode(mode: String) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode }
  }

  suspend fun updatePlanSizeSim1(mb: Long) {
    context.dataStore.edit { it[Keys.PLAN_SIZE_MB_SIM1] = mb }
  }

  suspend fun updatePlanSizeSim2(mb: Long) {
    context.dataStore.edit { it[Keys.PLAN_SIZE_MB_SIM2] = mb }
  }

  suspend fun updateCycleResetDay(day: Int) {
    val clamped = day.coerceIn(1, 28)
    context.dataStore.edit { it[Keys.CYCLE_RESET_DAY] = clamped }
  }

  suspend fun updateAlertThreshold(percent: Int) {
    val clamped = percent.coerceIn(10, 100)
    context.dataStore.edit { it[Keys.ALERT_THRESHOLD_PERCENT] = clamped }
  }

  suspend fun setAlertsEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.ALERTS_ENABLED] = enabled }
  }

  suspend fun getLastMobileBytesSample(): Long {
    val prefs = context.dataStore.data.first()
    return prefs[Keys.LAST_MOBILE_BYTES_SAMPLE] ?: -1L
  }

  suspend fun updateLastMobileBytesSample(bytes: Long, timestamp: Long) {
    context.dataStore.edit {
      it[Keys.LAST_MOBILE_BYTES_SAMPLE] = bytes
      it[Keys.LAST_SAMPLE_TIMESTAMP] = timestamp
    }
  }

  suspend fun hasAlertedForCycle(cycleKey: String): Boolean {
    val prefs = context.dataStore.data.first()
    return prefs[Keys.LAST_ALERTED_CYCLE_KEY] == cycleKey
  }

  suspend fun setAlertedForCycle(cycleKey: String) {
    context.dataStore.edit { it[Keys.LAST_ALERTED_CYCLE_KEY] = cycleKey }
  }

  /**
   * Credits delta mobile bytes to the active data subscription ID.
   * Maintains a compact per-SIM daily log, pruned to ~60 days.
   */
  suspend fun recordSimUsageDelta(subId: Int, deltaBytes: Long) {
    if (deltaBytes <= 0L) return
    val currentEpochDay = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())
    val pruneBeforeEpochDay = currentEpochDay - 60L

    context.dataStore.edit { preferences ->
      val raw = preferences[Keys.SIM_DAILY_LOG] ?: ""
      val entries = raw.lines()
        .mapNotNull { SimLogEntry.deserialize(it) }
        .filter { it.epochDay >= pruneBeforeEpochDay }
        .toMutableList()

      // Find if entry exists for today and this subId
      val index = entries.indexOfFirst { it.epochDay == currentEpochDay && it.subscriptionId == subId }
      if (index >= 0) {
        val existing = entries[index]
        entries[index] = existing.copy(bytes = existing.bytes + deltaBytes)
      } else {
        entries.add(SimLogEntry(epochDay = currentEpochDay, subscriptionId = subId, bytes = deltaBytes))
      }

      preferences[Keys.SIM_DAILY_LOG] = entries.joinToString("\n") { it.serialize() }
    }
  }

  /**
   * Retrieves total bytes credited to a SIM between two epoch timestamps.
   */
  suspend fun getSimUsageInWindow(subId: Int, startEpochMs: Long, endEpochMs: Long): Long {
    val startDay = TimeUnit.MILLISECONDS.toDays(startEpochMs)
    val endDay = TimeUnit.MILLISECONDS.toDays(endEpochMs)
    val prefs = context.dataStore.data.first()
    val raw = prefs[Keys.SIM_DAILY_LOG] ?: ""
    return raw.lines()
      .mapNotNull { SimLogEntry.deserialize(it) }
      .filter { it.subscriptionId == subId && it.epochDay in startDay..endDay }
      .sumOf { it.bytes }
  }

  /**
   * Retrieves all daily SIM entries for the past 60 days.
   */
  suspend fun getAllSimEntries(): List<SimLogEntry> {
    val prefs = context.dataStore.data.first()
    val raw = prefs[Keys.SIM_DAILY_LOG] ?: ""
    return raw.lines().mapNotNull { SimLogEntry.deserialize(it) }
  }

  suspend fun updateDailyLimit(mb: Long) {
    context.dataStore.edit { it[Keys.DAILY_LIMIT_MB] = mb.coerceAtLeast(0L) }
  }

  suspend fun setDailyAlertsEnabled(enabled: Boolean) {
    context.dataStore.edit { it[Keys.DAILY_ALERTS_ENABLED] = enabled }
  }

  suspend fun hasAlertedDailyToday(epochDay: Long): Boolean {
    val prefs = context.dataStore.data.first()
    return (prefs[Keys.LAST_ALERTED_DAILY_DAY] ?: -1L) == epochDay
  }

  suspend fun setAlertedDailyToday(epochDay: Long) {
    context.dataStore.edit { it[Keys.LAST_ALERTED_DAILY_DAY] = epochDay }
  }

  suspend fun setAppLimit(packageName: String, limitBytes: Long) {
    if (packageName.isBlank()) return
    context.dataStore.edit { prefs ->
      val current = parseAppLimits(prefs[Keys.APP_LIMITS] ?: "").toMutableMap()
      if (limitBytes <= 0L) {
        current.remove(packageName)
      } else {
        current[packageName] = limitBytes
      }
      prefs[Keys.APP_LIMITS] = current.entries.joinToString("\n") { "${it.key}:${it.value}" }
    }
  }

  suspend fun removeAppLimit(packageName: String) {
    setAppLimit(packageName, 0L)
  }

  suspend fun getAppLimits(): Map<String, Long> {
    val prefs = context.dataStore.data.first()
    return parseAppLimits(prefs[Keys.APP_LIMITS] ?: "")
  }

  suspend fun hasAlertedAppToday(packageName: String, epochDay: Long): Boolean {
    val prefs = context.dataStore.data.first()
    val raw = prefs[Keys.LAST_ALERTED_APP_LIMITS] ?: ""
    val key = "$packageName:$epochDay"
    return raw.lines().contains(key)
  }

  suspend fun setAlertedAppToday(packageName: String, epochDay: Long) {
    context.dataStore.edit { prefs ->
      val raw = prefs[Keys.LAST_ALERTED_APP_LIMITS] ?: ""
      val key = "$packageName:$epochDay"
      val lines = raw.lines().filter { it.isNotBlank() }.toMutableList()
      if (!lines.contains(key)) {
        lines.add(key)
        // Keep at most 100 entries
        val pruned = if (lines.size > 100) lines.takeLast(100) else lines
        prefs[Keys.LAST_ALERTED_APP_LIMITS] = pruned.joinToString("\n")
      }
    }
  }

  companion object {
    fun parseAppLimits(raw: String): Map<String, Long> {
      if (raw.isBlank()) return emptyMap()
      return raw.lines()
        .filter { it.isNotBlank() && it.contains(":") }
        .mapNotNull { line ->
          val parts = line.split(":", limit = 2)
          if (parts.size == 2) {
            val limit = parts[1].toLongOrNull()
            if (limit != null && limit > 0L) parts[0] to limit else null
          } else null
        }.toMap()
    }
  }
}
