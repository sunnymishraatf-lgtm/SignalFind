package com.signalfind.sms

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.signalfind.alarm.RingService
import com.signalfind.data.SettingsRepository
import com.signalfind.data.db.RecoveryEvent
import com.signalfind.data.repo.HistoryRepository
import com.signalfind.location.LocationProvider
import com.signalfind.location.LocationProvider.LocationResult
import com.signalfind.system.BatteryInfo
import com.signalfind.system.PermissionHelper
import com.signalfind.system.SimInfo
import com.signalfind.util.PhoneNumberUtils
import com.signalfind.util.TimeFormat
import java.util.Locale

/**
 * Builds and sends authenticated recovery replies. Every reply includes raw
 * latitude/longitude so it stays useful on a receiving phone with no internet
 * (the Google Maps link is a convenience on top).
 */
class SmsResponder(
    private val context: Context,
    private val settings: SettingsRepository,
    private val location: LocationProvider,
    private val smsSender: SmsSender,
    private val history: HistoryRepository
) {

    data class ReplyDraft(
        val text: String,
        val commandName: String,
        val locationState: String,
        val accuracyM: Float?,
        val latitude: Double?,
        val longitude: Double?
    )

    /** Build the reply without sending — used by the in-app test screen. */
    suspend fun buildReply(command: CommandParser.Command): ReplyDraft {
        return when (command) {
            CommandParser.Command.Location -> locationReply()
            CommandParser.Command.Battery -> batteryReply()
            CommandParser.Command.Ring -> ringReply()
            CommandParser.Command.Status -> statusReply()
        }
    }

    /**
     * Full handling of an authenticated command: build, send via the preferred
     * SIM, and record the outcome in local history.
     */
    suspend fun handle(sender: String, command: CommandParser.Command): Boolean {
        val draft = buildReply(command)
        val sent = smsSender.sendSms(sender, draft.text)
        history.record(
            RecoveryEvent(
                senderMasked = PhoneNumberUtils.mask(sender),
                command = draft.commandName,
                locationState = draft.locationState,
                accuracyM = draft.accuracyM,
                latitude = draft.latitude,
                longitude = draft.longitude,
                replyStatus = if (sent) "SENT" else "FAILED"
            )
        )
        return sent
    }

    /** Short reply for a wrong PIN from a trusted sender (never sent to strangers). */
    fun sendInvalidPinNotice(sender: String): Boolean =
        smsSender.sendSms(sender, "SignalFind: incorrect PIN. Check your PIN and try again.")

    /**
     * Audit trail for rejected attempts (wrong PIN, malformed command) from
     * senders that passed the earlier gates. Lets the owner see probing in
     * the recovery history; the sender is stored masked, as everywhere else.
     */
    suspend fun recordRejection(sender: String, commandName: String, reason: String) {
        history.record(
            RecoveryEvent(
                senderMasked = PhoneNumberUtils.mask(sender),
                command = commandName,
                locationState = "NONE",
                accuracyM = null,
                latitude = null,
                longitude = null,
                replyStatus = reason
            )
        )
    }

    private suspend fun locationReply(): ReplyDraft {
        val battery = BatteryInfo.get(context)
        return when (val r = location.getBestLocation()) {
            is LocationResult.Live -> {
                val text = buildString {
                    appendLine("\uD83D\uDCCD SignalFind Location")
                    appendLine("Lat: ${fmt(r.latitude)}")
                    appendLine("Lng: ${fmt(r.longitude)}")
                    appendLine("Accuracy: ±${r.accuracyM.toInt()}m")
                    appendLine("Updated: ${TimeFormat.time(r.timeMs)}")
                    appendLine("Battery: ${battery.levelPct}%")
                    appendLine("Source: ${r.provider} (live)")
                    append("Map: https://maps.google.com/?q=${fmt(r.latitude)},${fmt(r.longitude)}")
                }
                ReplyDraft(text, "LOCATION", "LIVE", r.accuracyM, r.latitude, r.longitude)
            }
            is LocationResult.Cached -> {
                val text = buildString {
                    appendLine("⚠️ SignalFind — live location unavailable")
                    appendLine()
                    appendLine("Last known location:")
                    appendLine("Lat: ${fmt(r.latitude)}")
                    appendLine("Lng: ${fmt(r.longitude)}")
                    r.accuracyM?.let { appendLine("Accuracy: ±${it.toInt()}m") }
                    appendLine("Age: ${TimeFormat.ageDescription(r.timeMs)} (cached)")
                    appendLine("Battery: ${battery.levelPct}%")
                    append("Map: https://maps.google.com/?q=${fmt(r.latitude)},${fmt(r.longitude)}")
                }
                ReplyDraft(text, "LOCATION", "CACHED", r.accuracyM, r.latitude, r.longitude)
            }
            is LocationResult.Unavailable -> {
                val gpsState = if (PermissionHelper.isLocationEnabled(context)) "ON" else "OFF"
                val text = buildString {
                    appendLine("SignalFind could not obtain the phone's current location.")
                    appendLine()
                    appendLine("GPS/Location: $gpsState")
                    appendLine("Battery: ${battery.levelPct}%")
                    append("SMS: Available")
                }
                ReplyDraft(text, "LOCATION", "NONE", null, null, null)
            }
        }
    }

    private fun batteryReply(): ReplyDraft {
        val b = BatteryInfo.get(context)
        val text = "\uD83D\uDD0B SignalFind Battery\nLevel: ${b.levelPct}%\nState: ${b.chargeKind}"
        return ReplyDraft(text, "BATTERY", "NONE", null, null, null)
    }

    private fun ringReply(): ReplyDraft {
        val started = try {
            val intent = Intent(context, RingService::class.java)
                .setAction(RingService.ACTION_START)
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (_: Exception) {
            // Android 12+ blocks foreground-service starts from the background.
            false
        }
        val text = if (started) {
            "\uD83D\uDD14 SignalFind: playing the recovery alarm for 60 seconds."
        } else {
            "SignalFind: could not start the alarm — Android blocked a background " +
                "service start. Open the app to ring it manually."
        }
        return ReplyDraft(text, "RING", "NONE", null, null, null)
    }

    private suspend fun statusReply(): ReplyDraft {
        val battery = BatteryInfo.get(context)
        val locEnabled = PermissionHelper.isLocationEnabled(context)
        val simCount = SimInfo.getSubscriptions(context).size
        val simText = if (simCount == 0) {
            if (SimInfo.hasUsableSim(context)) "1 (limited info)" else "none detected"
        } else {
            "$simCount active"
        }
        val text = buildString {
            appendLine("ℹ️ SignalFind Status")
            appendLine("Battery: ${battery.levelPct}% (${battery.chargeKind})")
            appendLine("Location: ${if (locEnabled) "available" else "OFF"}")
            appendLine("SIM: $simText")
            appendLine("SMS: Available")
            append("Time: ${TimeFormat.time(System.currentTimeMillis())}")
        }
        return ReplyDraft(text, "STATUS", "NONE", null, null, null)
    }

    private fun fmt(v: Double): String = String.format(Locale.US, "%.6f", v)
}
