package com.example.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import com.example.data.model.SimInfo

/**
 * Handles SIM card detection, active dual-SIM status, and default data subscription ID.
 * Completely avoids subscriberId (blocked on Android 10+).
 * READ_PHONE_STATE is purely optional for carrier names; fallback names are used if missing.
 */
class SimManager(private val context: Context) {

  private val subscriptionManager: SubscriptionManager? by lazy {
    context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
  }

  /**
   * Checks if READ_PHONE_STATE is granted.
   */
  fun hasPhoneStatePermission(): Boolean {
    return ContextCompat.checkSelfPermission(
      context,
      android.Manifest.permission.READ_PHONE_STATE
    ) == PackageManager.PERMISSION_GRANTED
  }

  /**
   * Retrieves the current default mobile data subscription ID.
   * Defaults to SubscriptionManager.getDefaultDataSubscriptionId() on Android 7.0+.
   */
  fun getDefaultDataSubscriptionId(): Int {
    return try {
      SubscriptionManager.getDefaultDataSubscriptionId()
    } catch (_: Exception) {
      SubscriptionManager.INVALID_SUBSCRIPTION_ID
    }
  }

  /**
   * Retrieves active SIM subscriptions. Handles permission restrictions gracefully.
   */
  fun getActiveSims(): List<SimInfo> {
    val subManager = subscriptionManager ?: return emptyList()
    val defaultDataSubId = getDefaultDataSubscriptionId()

    try {
      val infoList: List<SubscriptionInfo>? = subManager.activeSubscriptionInfoList
      if (infoList.isNullOrEmpty()) {
        // Fallback for devices where active list is inaccessible without permission
        return listOf(
          SimInfo(
            subscriptionId = if (defaultDataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) defaultDataSubId else 1,
            slotIndex = 0,
            displayName = "SIM 1",
            carrierName = "Mobile Network",
            isDefaultData = true
          )
        )
      }

      return infoList.mapIndexed { index, info ->
        val subId = info.subscriptionId
        val slot = info.simSlotIndex.takeIf { it >= 0 } ?: index
        val carrier = info.carrierName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${slot + 1}"
        val display = info.displayName?.toString()?.takeIf { it.isNotBlank() } ?: "SIM ${slot + 1}"

        SimInfo(
          subscriptionId = subId,
          slotIndex = slot,
          displayName = display,
          carrierName = carrier,
          isDefaultData = (subId == defaultDataSubId) || (infoList.size == 1)
        )
      }
    } catch (_: SecurityException) {
      // READ_PHONE_STATE not granted; return safe slot-based default
      return listOf(
        SimInfo(
          subscriptionId = if (defaultDataSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) defaultDataSubId else 1,
          slotIndex = 0,
          displayName = "SIM 1",
          carrierName = "Mobile Network",
          isDefaultData = true
        )
      )
    } catch (_: Exception) {
      return emptyList()
    }
  }

  /**
   * Returns true if there are two active SIM cards detected.
   */
  fun isDualSimActive(): Boolean {
    return getActiveSims().size >= 2
  }
}
