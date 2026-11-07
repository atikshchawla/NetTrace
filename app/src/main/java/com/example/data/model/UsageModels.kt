package com.example.data.model

import android.graphics.drawable.Drawable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Time range filters for data consumption analysis.
 */
enum class UsageRange(val label: String) {
  TODAY("Today"),
  SEVEN_DAYS("7 Days"),
  CYCLE("Cycle")
}

/**
 * Represents a historical monthly billing cycle or calendar month period for inspection.
 */
data class MonthPeriod(
  val id: String,
  val title: String,
  val shortLabel: String,
  val startTimeMs: Long,
  val endTimeMs: Long,
  val isCurrent: Boolean
)

/**
 * Holds aggregated network usage bytes for Wi-Fi and Mobile.
 */
data class UsageTotals(
  val wifiRxBytes: Long = 0L,
  val wifiTxBytes: Long = 0L,
  val mobileRxBytes: Long = 0L,
  val mobileTxBytes: Long = 0L
) {
  val totalWifiBytes: Long get() = wifiRxBytes + wifiTxBytes
  val totalMobileBytes: Long get() = mobileRxBytes + mobileTxBytes
  val grandTotalBytes: Long get() = totalWifiBytes + totalMobileBytes
}

/**
 * Aggregated daily usage point for the history scrubbable chart.
 */
data class DailyUsage(
  val epochDay: Long,
  val dateLabel: String,
  val wifiBytes: Long,
  val mobileBytes: Long,
  val sim1Bytes: Long = 0L,
  val sim2Bytes: Long = 0L
) {
  val totalBytes: Long get() = wifiBytes + mobileBytes
}

/**
 * Represents per-application usage with enhanced identification and system categorization.
 */
data class AppUsageItem(
  val uid: Int,
  val packageName: String,
  val appName: String,
  val icon: Drawable? = null,
  val wifiBytes: Long = 0L,
  val mobileBytes: Long = 0L,
  val isSystem: Boolean = false,
  val categoryLabel: String = ""
) {
  val totalBytes: Long get() = wifiBytes + mobileBytes
}

/**
 * Active SIM card details.
 */
data class SimInfo(
  val subscriptionId: Int,
  val slotIndex: Int,
  val displayName: String,
  val carrierName: String,
  val isDefaultData: Boolean = false
)

/**
 * Forecast calculation for the current billing cycle pace.
 */
data class PlanForecast(
  val daysRemaining: Int,
  val totalDaysInCycle: Int,
  val currentUsageBytes: Long,
  val planLimitBytes: Long,
  val dailyPaceBytes: Long,
  val projectedTotalBytes: Long,
  val willExceedLimit: Boolean,
  val estimatedExceedDate: String?
) {
  val bytesRemaining: Long get() = (planLimitBytes - currentUsageBytes).coerceAtLeast(0L)
  val percentUsed: Float get() = if (planLimitBytes > 0) {
    (currentUsageBytes.toFloat() / planLimitBytes.toFloat()).coerceIn(0f, 2f)
  } else 0f
}

/**
 * Formatting helpers for tabular display and Apple-like readability.
 */
object UsageFormatters {
  fun formatBytes(bytes: Long): String {
    val b = bytes.coerceAtLeast(0L)
    return when {
      b < 1024L * 1024L -> {
        val kb = b / 1024.0
        String.format(Locale.US, "%.1f KB", kb)
      }
      b < 1024L * 1024L * 1024L -> {
        val mb = b / (1024.0 * 1024.0)
        String.format(Locale.US, "%.1f MB", mb)
      }
      else -> {
        val gb = b / (1024.0 * 1024.0 * 1024.0)
        String.format(Locale.US, "%.2f GB", gb)
      }
    }
  }

  fun formatBytesNumberOnly(bytes: Long): String {
    val b = bytes.coerceAtLeast(0L)
    return when {
      b < 1024L * 1024L -> String.format(Locale.US, "%.0f", b / 1024.0)
      b < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f", b / (1024.0 * 1024.0))
      else -> String.format(Locale.US, "%.2f", b / (1024.0 * 1024.0 * 1024.0))
    }
  }

  fun formatBytesUnitOnly(bytes: Long): String {
    val b = bytes.coerceAtLeast(0L)
    return when {
      b < 1024L * 1024L -> "KB"
      b < 1024L * 1024L * 1024L -> "MB"
      else -> "GB"
    }
  }

  fun formatDate(timestampMs: Long): String {
    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
    return sdf.format(Date(timestampMs))
  }
}
