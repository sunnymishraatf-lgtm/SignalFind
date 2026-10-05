package com.signalfind.alarm

import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.signalfind.system.NotificationHelper

/**
 * Plays a loud looping alarm for the RING command (max 60 seconds).
 *
 * Runs as a foreground service with a visible notification and a Stop action —
 * there is no silent/stealth behavior. Volume is raised on the alarm stream
 * for the duration and restored afterwards.
 */
class RingService : Service() {

    companion object {
        const val ACTION_START = "com.signalfind.action.RING_START"
        const val ACTION_STOP = "com.signalfind.action.RING_STOP"
        private const val TAG = "SignalFindRing"
        private const val RING_MS = 60_000L
    }

    private var player: MediaPlayer? = null
    private var previousVolume: Int = -1
    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable {
        stopRing()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopRing()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> startRing()
        }
        return START_NOT_STICKY
    }

    private fun startRing() {
        if (player != null) return // already ringing
        startForeground(
            NotificationHelper.NOTIF_ALARM,
            NotificationHelper.buildAlarmNotification(this)
        )
        try {
            val audio = getSystemService(AudioManager::class.java)
            if (audio != null) {
                previousVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
                audio.setStreamVolume(
                    AudioManager.STREAM_ALARM,
                    audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                    0
                )
            }
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            if (uri == null) {
                // No tone on this device: never leave a stuck foreground
                // service — the auto-stop was only scheduled after this point.
                Log.w(TAG, "no alarm tone available")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            player = MediaPlayer().apply {
                setDataSource(this@RingService, uri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            handler.postDelayed(stopRunnable, RING_MS)
        } catch (e: Exception) {
            Log.w(TAG, "alarm failed: ${e.javaClass.simpleName}")
            stopRing()
            stopSelf()
        }
    }

    private fun stopRing() {
        handler.removeCallbacks(stopRunnable)
        try {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        }
        player = null
        try {
            val audio = getSystemService(AudioManager::class.java)
            if (audio != null && previousVolume >= 0) {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0)
            }
        } catch (_: Exception) {
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopRing()
        super.onDestroy()
    }
}
