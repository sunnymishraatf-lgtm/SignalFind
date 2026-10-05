package com.signalfind.system

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

data class BatteryState(
    val levelPct: Int,
    val isCharging: Boolean,
    val chargeKind: String
)

object BatteryInfo {
    fun get(context: Context): BatteryState {
        val bm = context.getSystemService(BatteryManager::class.java)
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1

        val intent: Intent? = context.registerReceiver(
            null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val kind = when {
            status == BatteryManager.BATTERY_STATUS_FULL -> "Full"
            plugged == BatteryManager.BATTERY_PLUGGED_AC -> "Charging (AC)"
            plugged == BatteryManager.BATTERY_PLUGGED_USB -> "Charging (USB)"
            plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Charging (wireless)"
            isCharging -> "Charging"
            else -> "Not charging"
        }
        return BatteryState(levelPct = level.coerceIn(0, 100), isCharging = isCharging, chargeKind = kind)
    }
}
