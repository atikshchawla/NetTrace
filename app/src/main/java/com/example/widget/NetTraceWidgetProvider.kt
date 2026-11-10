package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.model.UsageFormatters
import com.example.data.model.UsageRange
import com.example.data.repository.DataStoreRepository
import com.example.data.repository.NetworkStatsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * NetTrace Glance Home Screen Widget:
 * Displays remaining cellular data, days until reset, today's total/cellular breakdown,
 * and monthly pacing status at a glance without opening the app.
 */
class NetTraceWidgetProvider : AppWidgetProvider() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    updateWidgets(context, appWidgetManager, appWidgetIds)
  }

  companion object {
    fun updateAllWidgets(context: Context) {
      val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
      val componentName = ComponentName(context, NetTraceWidgetProvider::class.java)
      val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
      if (appWidgetIds.isNotEmpty()) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
      }
    }

    private fun updateWidgets(
      context: Context,
      appWidgetManager: AppWidgetManager,
      appWidgetIds: IntArray
    ) {
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val networkStatsRepo = NetworkStatsRepository(context)
          val dataStoreRepo = DataStoreRepository(context)

          if (!networkStatsRepo.hasUsageAccessPermission()) {
            // Render ungranted permission state
            for (widgetId in appWidgetIds) {
              val views = RemoteViews(context.packageName, R.layout.widget_nettrace_glance)
              views.setTextViewText(R.id.widget_title, "NetTrace")
              views.setTextViewText(R.id.widget_data_remaining, "Access Needed")
              views.setTextViewText(R.id.widget_data_remaining_label, "Tap to Grant")
              views.setTextViewText(R.id.widget_today_total, "Offline")
              views.setTextViewText(R.id.widget_today_breakdown, "Permission required")
              views.setTextViewText(R.id.widget_days_left, "--")
              views.setTextViewText(R.id.widget_footer_status, "Tap to open NetTrace and grant usage access")

              val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
              }
              val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
              )
              views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
              appWidgetManager.updateAppWidget(widgetId, views)
            }
            return@launch
          }

          val resetDay = dataStoreRepo.cycleResetDayFlow.first()
          val planLimitMb = dataStoreRepo.planSizeMbSim1Flow.first()
          val planLimitBytes = planLimitMb * 1024L * 1024L

          val (cycleStart, cycleEnd) = networkStatsRepo.calculateBillingCycleRange(resetDay)
          val (todayStart, todayEnd) = networkStatsRepo.getRangeTimestamps(UsageRange.TODAY, resetDay)

          val cycleTotals = networkStatsRepo.queryDeviceTotals(cycleStart, cycleEnd)
          val todayTotals = networkStatsRepo.queryDeviceTotals(todayStart, todayEnd)

          val planForecast = networkStatsRepo.computePlanForecast(
            cycleStartMs = cycleStart,
            cycleEndMs = cycleEnd,
            mobileUsageBytes = cycleTotals.totalMobileBytes,
            planLimitBytes = planLimitBytes
          )

          val remainingText = UsageFormatters.formatBytes(planForecast.bytesRemaining)
          val daysLeftText = "${planForecast.daysRemaining}d left"
          val todayTotalText = "Today: ${UsageFormatters.formatBytes(todayTotals.grandTotalBytes)}"
          val todayBreakdownText = "Cell: ${UsageFormatters.formatBytes(todayTotals.totalMobileBytes)} • Wi-Fi: ${UsageFormatters.formatBytes(todayTotals.totalWifiBytes)}"

          val footerText = if (planForecast.willExceedLimit && planForecast.estimatedExceedDate != null) {
            "Pacing alert: Limit hit by ${planForecast.estimatedExceedDate}"
          } else {
            "Pacing on track: ${UsageFormatters.formatBytes(planForecast.dailyPaceBytes)}/day"
          }

          val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
          }
          val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
          )

          for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_nettrace_glance)
            views.setTextViewText(R.id.widget_title, "NetTrace")
            views.setTextViewText(R.id.widget_days_left, daysLeftText)
            views.setTextViewText(R.id.widget_data_remaining, remainingText)
            views.setTextViewText(R.id.widget_data_remaining_label, "Data Remaining")
            views.setTextViewText(R.id.widget_today_total, todayTotalText)
            views.setTextViewText(R.id.widget_today_breakdown, todayBreakdownText)
            views.setTextViewText(R.id.widget_footer_status, footerText)

            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            appWidgetManager.updateAppWidget(widgetId, views)
          }
        } catch (_: Exception) {}
      }
    }
  }
}
