package com.signalfind.sms

import android.content.Context
import android.os.Build
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.util.Log
import com.signalfind.data.SettingsRepository
import com.signalfind.system.SimInfo

/**
 * Sends SMS replies, honoring the user's preferred SIM on dual-SIM devices.
 * Long replies are split into multipart messages automatically.
 */
class SmsSender(
    private val context: Context,
    private val settings: SettingsRepository
) {
    companion object {
        private const val TAG = "SignalFindSms"
    }

    private fun managerFor(subId: Int?): SmsManager {
        val valid = subId != null && subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID &&
            subId != SettingsRepository.NO_SUB_ID
        if (!valid) return context.getSystemService(SmsManager::class.java)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+: the supported way to bind an SmsManager to one SIM.
                context.getSystemService(SmsManager::class.java)
                    .createForSubscriptionId(subId!!)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getSmsManagerForSubscriptionId(subId!!)
            }
        } catch (_: Exception) {
            context.getSystemService(SmsManager::class.java)
        }
    }

    /**
     * Resolves which subscription to send with: the user's preferred SIM when
     * it is still active, otherwise the system default.
     */
    fun resolveSubId(): Int? {
        val preferred = settings.getPreferredSubId()
        if (preferred == SettingsRepository.NO_SUB_ID) return null
        val stillActive = SimInfo.getSubscriptions(context)
            .any { it.subscriptionId == preferred }
        return if (stillActive) preferred else null
    }

    /** Returns true when every part was accepted by the telephony stack. */
    fun sendSms(destination: String, text: String, subId: Int? = null): Boolean {
        val mgr = try {
            managerFor(subId ?: resolveSubId())
        } catch (e: Exception) {
            Log.w(TAG, "SmsManager unavailable: ${e.javaClass.simpleName}")
            return false
        }
        return try {
            val parts = mgr.divideMessage(text)
            if (parts.size <= 1) {
                mgr.sendTextMessage(destination, null, text, null, null)
            } else {
                mgr.sendMultipartTextMessage(destination, null, parts, null, null)
            }
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "SMS send blocked: ${e.javaClass.simpleName}")
            false
        } catch (e: Exception) {
            Log.w(TAG, "SMS send failed: ${e.javaClass.simpleName}")
            false
        }
    }
}
