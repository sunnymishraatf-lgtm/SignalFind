package com.signalfind.system

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.signalfind.MainActivity
import com.signalfind.R
import com.signalfind.alarm.RingService

object NotificationHelper {
    const val CHANNEL_RECOVERY = "signalfind_recovery"
    const val CHANNEL_ALARM = "signalfind_alarm"
    const val NOTIF_RECOVERY_ACTIVE = 1001
    const val NOTIF_ALARM = 1002

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_RECOVERY,
                "Recovery status",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows that SMS recovery mode is active." }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALARM,
                "Recovery alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Loud alarm played by the RING command." }
        )
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Persistent, honest indicator that recovery mode is on. Never hidden. */
    fun showRecoveryActive(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val notif = NotificationCompat.Builder(context, CHANNEL_RECOVERY)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SignalFind recovery active")
            .setContentText("This phone can answer authenticated SMS recovery commands.")
            .setOngoing(true)
            .setContentIntent(openAppIntent(context))
            .build()
        try {
            nm.notify(NOTIF_RECOVERY_ACTIVE, notif)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on Android 13+; status stays in-app.
        }
    }

    fun hideRecoveryActive(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(NOTIF_RECOVERY_ACTIVE)
    }

    fun buildAlarmNotification(context: Context): Notification {
        val stop = Intent(context, RingService::class.java).setAction(RingService.ACTION_STOP)
        val stopPi = PendingIntent.getService(
            context, 1, stop,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("SignalFind recovery alarm")
            .setContentText("Playing loud alarm. Tap Stop to silence.")
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_ALARM)
            .setContentIntent(openAppIntent(context))
            .addAction(
                android.R.drawable.ic_media_pause, "Stop",
                stopPi
            )
            .build()
    }
}
