package com.example.worker

import android.content.Context
import android.net.TrafficStats
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.model.UsageFormatters
import com.example.data.repository.DataStoreRepository
import com.example.data.repository.NetworkStatsRepository
import com.example.data.repository.SimManager
import kotlinx.coroutines.flow.first

/**
 * Hourly periodic worker that:
 * 1. Reads device mobile bytes, diffs against the last reading, and credits the delta to the default data SIM.
 * 2. Prunes the per-SIM daily log to 60 days.
 * 3. Evaluates billing cycle usage against user-defined alert thresholds.
 * 4. Posts a notification at most once per cycle crossing.
 */
class SimUsageSamplingWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  companion object {
    const val WORK_NAME = "nettrace_sim_sampling_worker"
  }

  private val dataStoreRepo = DataStoreRepository(applicationContext)
  private val simManager = SimManager(applicationContext)
  private val networkStatsRepo = NetworkStatsRepository(applicationContext)
  private val notificationManager = AlertNotificationManager(applicationContext)

  override suspend fun doWork(): Result {
    try {
      sampleMobileBytesAndCreditActiveSim()
      checkBillingCycleAlertThresholds()
      return Result.success()
    } catch (_: Exception) {
      return Result.retry()
    }
  }

  /**
   * Reads total mobile bytes, computes delta since previous reading,
   * credits delta to the default data SIM, and updates last sample.
   * Treats negative deltas (device reboots) as zero.
   */
  private suspend fun sampleMobileBytesAndCreditActiveSim() {
    val currentMobileRx = TrafficStats.getMobileRxBytes()
    val currentMobileTx = TrafficStats.getMobileTxBytes()
    if (currentMobileRx == TrafficStats.UNSUPPORTED.toLong() || currentMobileTx == TrafficStats.UNSUPPORTED.toLong()) {
      return
    }

    val currentTotalMobileBytes = currentMobileRx + currentMobileTx
    val lastSample = dataStoreRepo.getLastMobileBytesSample()
    val now = System.currentTimeMillis()

    val deltaBytes = if (lastSample >= 0L && currentTotalMobileBytes >= lastSample) {
      currentTotalMobileBytes - lastSample
    } else {
      0L // First run or device reboot reset counter
    }

    dataStoreRepo.updateLastMobileBytesSample(currentTotalMobileBytes, now)

    if (deltaBytes > 0L) {
      val defaultSubId = simManager.getDefaultDataSubscriptionId()
      val targetSubId = if (defaultSubId != android.telephony.SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
        defaultSubId
      } else {
        1
      }
      dataStoreRepo.recordSimUsageDelta(targetSubId, deltaBytes)
    }
  }

  /**
   * Computes cycle usage and sends an alert if the threshold is reached.
   */
  private suspend fun checkBillingCycleAlertThresholds() {
    val alertsEnabled = dataStoreRepo.alertsEnabledFlow.first()
    if (!alertsEnabled) return

    val resetDay = dataStoreRepo.cycleResetDayFlow.first()
    val (cycleStartMs, cycleEndMs) = networkStatsRepo.calculateBillingCycleRange(resetDay)
    val cycleKey = "cycle_${cycleStartMs}"

    if (dataStoreRepo.hasAlertedForCycle(cycleKey)) {
      return // Already alerted for this billing cycle
    }

    val thresholdPercent = dataStoreRepo.alertThresholdPercentFlow.first()
    val thresholdFraction = thresholdPercent / 100.0

    val isDualSim = simManager.isDualSimActive()
    val activeSims = simManager.getActiveSims()
    val now = System.currentTimeMillis()

    if (!isDualSim || activeSims.isEmpty()) {
      // Single-SIM mode: use plain system totals
      val totals = networkStatsRepo.queryDeviceTotals(cycleStartMs, now)
      val cycleMobileBytes = totals.totalMobileBytes
      val planLimitMb = dataStoreRepo.planSizeMbSim1Flow.first()
      val planLimitBytes = planLimitMb * 1024L * 1024L

      if (planLimitBytes > 0L && cycleMobileBytes >= (planLimitBytes * thresholdFraction)) {
        val usedFormatted = UsageFormatters.formatBytes(cycleMobileBytes)
        val planFormatted = UsageFormatters.formatBytes(planLimitBytes)
        notificationManager.postThresholdAlert(
          simName = "Cellular Plan",
          percentUsed = thresholdPercent,
          usedFormatted = usedFormatted,
          planFormatted = planFormatted
        )
        dataStoreRepo.setAlertedForCycle(cycleKey)
      }
    } else {
      // Dual-SIM mode: evaluate primary/active data SIM
      for (sim in activeSims) {
        val planLimitMb = if (sim.slotIndex == 0) {
          dataStoreRepo.planSizeMbSim1Flow.first()
        } else {
          dataStoreRepo.planSizeMbSim2Flow.first()
        }
        val planLimitBytes = planLimitMb * 1024L * 1024L
        val simCycleBytes = dataStoreRepo.getSimUsageInWindow(sim.subscriptionId, cycleStartMs, now)

        if (planLimitBytes > 0L && simCycleBytes >= (planLimitBytes * thresholdFraction)) {
          val usedFormatted = UsageFormatters.formatBytes(simCycleBytes)
          val planFormatted = UsageFormatters.formatBytes(planLimitBytes)
          notificationManager.postThresholdAlert(
            simName = sim.displayName,
            percentUsed = thresholdPercent,
            usedFormatted = usedFormatted,
            planFormatted = planFormatted
          )
          dataStoreRepo.setAlertedForCycle(cycleKey)
          break // Alert once per cycle
        }
      }
    }
  }
}
