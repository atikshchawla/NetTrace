package com.example.data.model

/**
 * Compact record for per-SIM daily usage tracking.
 * Serialized in DataStore as a newline-delimited or comma-delimited compact string:
 * "epochDay,subId,bytes"
 */
data class SimLogEntry(
  val epochDay: Long,
  val subscriptionId: Int,
  val bytes: Long
) {
  fun serialize(): String = "$epochDay,$subscriptionId,$bytes"

  companion object {
    fun deserialize(line: String): SimLogEntry? {
      val parts = line.split(",")
      if (parts.size != 3) return null
      val day = parts[0].toLongOrNull() ?: return null
      val subId = parts[1].toIntOrNull() ?: return null
      val b = parts[2].toLongOrNull() ?: return null
      return SimLogEntry(epochDay = day, subscriptionId = subId, bytes = b)
    }
  }
}
