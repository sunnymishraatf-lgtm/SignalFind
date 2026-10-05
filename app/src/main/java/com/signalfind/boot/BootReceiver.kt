package com.signalfind.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.signalfind.SignalFindApp
import com.signalfind.system.NotificationHelper

/**
 * Restores the visible recovery state after a reboot when the user had
 * recovery mode enabled. Uses only the standard BOOT_COMPLETED broadcast —
 * no security restrictions are bypassed. The SMS receiver itself is
 * manifest-declared, so it is active again automatically.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        try {
            val app = context.applicationContext as? SignalFindApp ?: return
            val settings = app.container.settings
            if (settings.isSetupComplete() && settings.isRecoveryEnabled()) {
                NotificationHelper.showRecoveryActive(context)
            }
        } catch (_: Exception) {
            // Encrypted storage or notifications unavailable this early; the
            // manifest-declared SMS receiver works regardless.
        }
    }
}
