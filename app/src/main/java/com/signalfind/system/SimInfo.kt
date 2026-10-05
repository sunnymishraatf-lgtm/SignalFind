package com.signalfind.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat

data class SimSubscription(
    val subscriptionId: Int,
    val displayName: String,
    val carrierName: String
)

object SimInfo {

    /** Active subscriptions. Needs READ_PHONE_STATE; returns empty without it. */
    fun getSubscriptions(context: Context): List<SimSubscription> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) !=
            PackageManager.PERMISSION_GRANTED
        ) return emptyList()
        return try {
            val sm = context.getSystemService(SubscriptionManager::class.java) ?: return emptyList()
            val list = sm.activeSubscriptionInfoList
            (list ?: emptyList()).map {
                SimSubscription(
                    subscriptionId = it.subscriptionId,
                    displayName = it.displayName?.toString().ifNullOrBlank("SIM ${it.simSlotIndex + 1}"),
                    carrierName = it.carrierName?.toString().orEmpty()
                )
            }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun hasUsableSim(context: Context): Boolean {
        val tm = context.getSystemService(TelephonyManager::class.java) ?: return false
        if (tm.simState != TelephonyManager.SIM_STATE_READY) {
            // Fall back to subscription list when telephony state is unavailable.
            return getSubscriptions(context).isNotEmpty()
        }
        return true
    }

    private fun String?.ifNullOrBlank(default: String): String =
        if (this.isNullOrBlank()) default else this
}
