package com.signalfind.system

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat

/** Central permission / device-state checks used by UI, receiver and diagnostics. */
object PermissionHelper {

    fun hasSms(context: Context): Boolean =
        has(context, Manifest.permission.RECEIVE_SMS) &&
            has(context, Manifest.permission.SEND_SMS)

    fun hasFineLocation(context: Context): Boolean =
        has(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasBackgroundLocation(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 29) return true // implied by foreground grant pre-Q
        return has(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    fun hasNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return has(context, Manifest.permission.POST_NOTIFICATIONS)
    }

    fun hasPhoneState(context: Context): Boolean =
        has(context, Manifest.permission.READ_PHONE_STATE)

    private fun has(context: Context, perm: String): Boolean =
        ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(LocationManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= 28) {
            lm.isLocationEnabled
        } else {
            @Suppress("DEPRECATION")
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
}
