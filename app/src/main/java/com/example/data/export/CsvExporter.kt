package com.example.data.export

import android.content.Context
import android.net.Uri
import com.example.data.model.AppUsageItem
import com.example.data.model.DailyUsage
import com.example.data.model.SimLogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates and exports comprehensive CSV telemetry reports without requiring storage permissions.
 * Compatible with Intent.ACTION_CREATE_DOCUMENT.
 */
class CsvExporter(private val context: Context) {

  /**
   * Generates CSV formatted text containing daily usage history and app breakdown.
   */
  fun generateCsvContent(
    dailyList: List<DailyUsage>,
    simEntries: List<SimLogEntry>,
    appList: List<AppUsageItem>
  ): String {
    val sb = StringBuilder()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    val exportDate = sdf.format(Date())

    // Header info
    sb.append("# NetTrace Data Usage Export\n")
    sb.append("# Generated: $exportDate\n")
    sb.append("# Units: Megabytes (MB)\n\n")

    // Daily breakdown table
    sb.append("--- Daily Usage (Last 30 Days) ---\n")
    sb.append("Date,EpochDay,WiFi_MB,Mobile_MB,Total_MB,SIM1_MB,SIM2_MB\n")

    val simByDay = simEntries.groupBy { it.epochDay }

    for (day in dailyList) {
      val daySimEntries = simByDay[day.epochDay] ?: emptyList()
      val sim1Mb = (daySimEntries.find { it.subscriptionId == 1 }?.bytes ?: 0L) / (1024.0 * 1024.0)
      val sim2Mb = (daySimEntries.find { it.subscriptionId == 2 }?.bytes ?: 0L) / (1024.0 * 1024.0)

      val wifiMb = day.wifiBytes / (1024.0 * 1024.0)
      val mobileMb = day.mobileBytes / (1024.0 * 1024.0)
      val totalMb = day.totalBytes / (1024.0 * 1024.0)

      sb.append(
        String.format(
          Locale.US,
          "%s,%d,%.2f,%.2f,%.2f,%.2f,%.2f\n",
          day.dateLabel,
          day.epochDay,
          wifiMb,
          mobileMb,
          totalMb,
          sim1Mb,
          sim2Mb
        )
      )
    }

    sb.append("\n--- Top Apps Usage (Billing Cycle) ---\n")
    sb.append("Rank,AppName,PackageOrUID,WiFi_MB,Mobile_MB,Total_MB\n")

    appList.forEachIndexed { index, app ->
      val wifiMb = app.wifiBytes / (1024.0 * 1024.0)
      val mobileMb = app.mobileBytes / (1024.0 * 1024.0)
      val totalMb = app.totalBytes / (1024.0 * 1024.0)
      val escapedName = "\"${app.appName.replace("\"", "\"\"")}\""

      sb.append(
        String.format(
          Locale.US,
          "%d,%s,%s,%.2f,%.2f,%.2f\n",
          index + 1,
          escapedName,
          app.packageName,
          wifiMb,
          mobileMb,
          totalMb
        )
      )
    }

    return sb.toString()
  }

  /**
   * Writes CSV string to the target Uri selected via ACTION_CREATE_DOCUMENT.
   */
  suspend fun writeToUri(targetUri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
    try {
      context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
        BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
          writer.write(content)
          writer.flush()
        }
      }
      true
    } catch (_: Exception) {
      false
    }
  }
}
