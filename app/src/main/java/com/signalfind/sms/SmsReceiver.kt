package com.signalfind.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.signalfind.SignalFindApp
import com.signalfind.security.RateLimiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Entry point for incoming SMS. Only messages shaped like SignalFind commands
 * are processed; everything else is ignored without being read further.
 *
 * Order of checks (fail fast, fail silent):
 * 1. Setup complete + recovery enabled?
 * 2. Message shaped like a command? (cheap pre-filter)
 * 3. Sender trusted? (when "trusted numbers only" is on — the default)
 * 4. Rate limit / lockout?
 * 5. Parses cleanly?
 * 6. PIN verifies (constant-time)?
 *
 * The PIN never appears in logs — only sender-masked, non-secret metadata.
 */
class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SignalFindSms"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val pending = goAsync()
        scope.launch {
            try {
                process(context.applicationContext, intent)
            } catch (e: Exception) {
                Log.w(TAG, "process failed: ${e.javaClass.simpleName}")
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun process(context: Context, intent: Intent) {
        val container = (context.applicationContext as SignalFindApp).container
        val settings = container.settings
        if (!settings.isSetupComplete() || !settings.isRecoveryEnabled()) return

        val messages = try {
            Telephony.Sms.Intents.getMessagesFromIntent(intent)
        } catch (_: Exception) {
            return
        }
        if (messages.isNullOrEmpty()) return

        val senderRaw = messages.firstOrNull()?.displayOriginatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        if (body.isBlank()) return

        val keyword = settings.getEmergencyKeyword()
        if (!CommandParser.looksLikeCommand(body, keyword)) return // not ours; ignore

        val sender = com.signalfind.util.PhoneNumberUtils.normalize(senderRaw)

        // Trusted-sender gate (default ON).
        if (settings.isTrustedOnly() && !container.trustedRepo.isTrusted(sender)) {
            Log.i(TAG, "ignored command from untrusted sender")
            return
        }

        when (container.rateLimiter.check(sender)) {
            RateLimiter.Decision.Allow -> Unit
            RateLimiter.Decision.TooFrequent -> {
                Log.i(TAG, "rate-limited sender")
                return
            }
            is RateLimiter.Decision.Locked -> {
                Log.i(TAG, "locked-out sender")
                return
            }
        }

        val parsed = CommandParser.parse(body, keyword)
        if (parsed == null) {
            container.rateLimiter.recordFailure(sender)
            Log.i(TAG, "malformed command rejected")
            // Audit trail for trusted senders only — stranger noise is ignored
            // silently, exactly like their messages.
            if (container.trustedRepo.isTrusted(sender)) {
                container.responder.recordRejection(sender, "UNKNOWN", "MALFORMED")
            }
            return
        }

        if (!settings.verifyPin(parsed.pin)) {
            container.rateLimiter.recordFailure(sender)
            Log.i(TAG, "PIN mismatch")
            // The sender reached the PIN gate, so a probing attempt is worth
            // recording (masked) for the owner to see.
            container.responder.recordRejection(
                sender, parsed.command.javaClass.simpleName.uppercase(), "PIN_WRONG"
            )
            // Helpful to the real owner, silent to strangers: the "incorrect
            // PIN" hint goes only to trusted senders, and never while locked
            // out (which would otherwise confirm the app's presence).
            if (container.trustedRepo.isTrusted(sender) &&
                container.rateLimiter.check(sender) is RateLimiter.Decision.Allow
            ) {
                container.responder.sendInvalidPinNotice(sender)
            }
            return
        }

        container.rateLimiter.recordSuccess(sender)
        val sent = container.responder.handle(sender, parsed.command)
        Log.i(TAG, "command ${parsed.command.javaClass.simpleName} handled, reply sent=$sent")
    }
}
