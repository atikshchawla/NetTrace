package com.example.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity

/**
 * Manages notification channels and posts threshold alert notifications.
 * Respects Android 13+ POST_NOTIFICATIONS permission.
 */
class AlertNotificationManager(private val context: Context) {

  companion object {
    const val CHANNEL_ID = "nettrace_data_alerts"
    const val CHANNEL_NAME = "Plan Threshold Alerts"
    const val NOTIFICATION_ID = 1001
  }

  init {
    createNotificationChannel()
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        CHANNEL_ID,
        CHANNEL_NAME,
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Notifies when cellular data usage crosses your plan alert threshold"
        enableLights(false)
        enableVibration(true)
      }
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      notificationManager?.createNotificationChannel(channel)
    }
  }

  /**
   * Returns true if notifications are permitted on the current device.
   */
  fun canPostNotifications(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  /**
   * Posts an Apple-like calm, informative plan threshold alert.
   */
  fun postThresholdAlert(
    simName: String,
    percentUsed: Int,
    usedFormatted: String,
    planFormatted: String
  ) {
    if (!canPostNotifications()) return

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_sys_warning)
      .setContentTitle("Data Plan Alert ($percentUsed%)")
      .setContentText("$simName used $usedFormatted of $planFormatted. Approaching limit.")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("$simName has reached $percentUsed% of its $planFormatted billing cycle plan ($usedFormatted used). Consider monitoring background usage.")
      )
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.notify(NOTIFICATION_ID, notification)
  }
}
