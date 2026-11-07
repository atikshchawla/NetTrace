package com.example

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.worker.AlertNotificationManager
import com.example.worker.SimUsageSamplingWorker
import java.util.concurrent.TimeUnit

/**
 * NetTraceApplication initializes notification channels and enqueues the
 * hourly background WorkManager job for SIM usage tracking and threshold checks.
 */
class NetTraceApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    // Safe initialization to guarantee the application never crashes during boot
    try {
      AlertNotificationManager(this)
    } catch (_: Exception) {}

    try {
      scheduleSimSamplingWorker()
    } catch (_: Exception) {}
  }

  private fun scheduleSimSamplingWorker() {
    val constraints = Constraints.Builder()
      .setRequiresBatteryNotLow(true)
      .build()

    val workRequest = PeriodicWorkRequestBuilder<SimUsageSamplingWorker>(
      1, TimeUnit.HOURS
    )
      .setConstraints(constraints)
      .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
      SimUsageSamplingWorker.WORK_NAME,
      ExistingPeriodicWorkPolicy.KEEP,
      workRequest
    )
  }
}
