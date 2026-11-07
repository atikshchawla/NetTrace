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
  }

  val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
    preferences[Keys.THEME_MODE] ?: "system" // "system", "dark", "light" - default is system mode
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
}
