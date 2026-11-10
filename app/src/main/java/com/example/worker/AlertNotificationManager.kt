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
    const val CHANNEL_NAME = "Data Alerts & Limits"
    const val NOTIFICATION_ID_CYCLE = 1001
    const val NOTIFICATION_ID_DAILY = 1002
    const val NOTIFICATION_ID_APP_BASE = 2000
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
        description = "Notifies when cellular data usage crosses cycle thresholds, daily limits, or app limits"
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

  private fun getPendingIntent(): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
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

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_sys_warning)
      .setContentTitle("Data Plan Alert ($percentUsed%)")
      .setContentText("$simName used $usedFormatted of $planFormatted. Approaching limit.")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("$simName has reached $percentUsed% of its $planFormatted billing cycle plan ($usedFormatted used). Consider monitoring background usage.")
      )
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(getPendingIntent())
      .setAutoCancel(true)
      .build()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.notify(NOTIFICATION_ID_CYCLE, notification)
  }

  /**
   * Posts a daily mobile data ceiling alert.
   */
  fun postDailyLimitAlert(
    usedFormatted: String,
    limitFormatted: String
  ) {
    if (!canPostNotifications()) return

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_sys_warning)
      .setContentTitle("Daily Mobile Limit Reached")
      .setContentText("Today's mobile usage reached $usedFormatted (Limit: $limitFormatted).")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("You have reached your daily mobile data budget of $limitFormatted today ($usedFormatted recorded). Switch to Wi-Fi to avoid cellular overages.")
      )
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(getPendingIntent())
      .setAutoCancel(true)
      .build()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.notify(NOTIFICATION_ID_DAILY, notification)
  }

  /**
   * Posts an alert when an individual app exceeds its assigned consumption ceiling.
   */
  fun postAppLimitAlert(
    appName: String,
    usedFormatted: String,
    limitFormatted: String,
    appUid: Int = 0
  ) {
    if (!canPostNotifications()) return

    val notificationId = NOTIFICATION_ID_APP_BASE + (Math.abs(appUid) % 1000)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
      .setSmallIcon(android.R.drawable.stat_sys_warning)
      .setContentTitle("App Data Limit: $appName")
      .setContentText("$appName consumed $usedFormatted, exceeding its $limitFormatted ceiling.")
      .setStyle(
        NotificationCompat.BigTextStyle()
          .bigText("$appName has exceeded your assigned limit of $limitFormatted (used $usedFormatted).")
      )
      .setPriority(NotificationCompat.PRIORITY_DEFAULT)
      .setContentIntent(getPendingIntent())
      .setAutoCancel(true)
      .build()

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    notificationManager?.notify(notificationId, notification)
  }
}
