package com.signalfind.security

import android.content.Context
import com.signalfind.util.PhoneNumberUtils
import java.security.MessageDigest

/**
 * Per-sender rate limiting and failed-command protection.
 *
 * Rules:
 * - At most one processed command per sender every 5 seconds.
 * - 5 failed PIN attempts inside a 10-minute window locks the sender out for
 *   15 minutes (silent — no reply while locked).
 * - A successful command clears the failure counter.
 *
 * Sender keys are SHA-256 hashes so raw numbers are not persisted here.
 */
class RateLimiter(context: Context) {

    private val prefs =
        context.getSharedPreferences("signalfind_ratelimit", Context.MODE_PRIVATE)

    companion object {
        private const val MIN_INTERVAL_MS = 5_000L
        private const val FAIL_WINDOW_MS = 10 * 60_000L
        private const val MAX_FAILS = 5
        private const val LOCKOUT_MS = 15 * 60_000L
    }

    sealed interface Decision {
        data object Allow : Decision
        data object TooFrequent : Decision
        data class Locked(val untilMs: Long) : Decision
    }

    private fun keyFor(sender: String, suffix: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(PhoneNumberUtils.normalize(sender).toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }.take(16)
        return "rl_${hex}_$suffix"
    }

    @Synchronized
    fun check(sender: String): Decision {
        val now = System.currentTimeMillis()
        val lockUntil = prefs.getLong(keyFor(sender, "lock"), 0L)
        if (now < lockUntil) return Decision.Locked(lockUntil)
        val last = prefs.getLong(keyFor(sender, "last"), 0L)
        if (now - last < MIN_INTERVAL_MS) return Decision.TooFrequent
        return Decision.Allow
    }

    @Synchronized
    fun recordFailure(sender: String) {
        val now = System.currentTimeMillis()
        val kFails = keyFor(sender, "fails")
        val kWindow = keyFor(sender, "window")
        var fails = prefs.getInt(kFails, 0)
        var windowStart = prefs.getLong(kWindow, 0L)
        if (now - windowStart > FAIL_WINDOW_MS) {
            fails = 0
            windowStart = now
        }
        fails += 1
        val edit = prefs.edit()
            .putInt(kFails, fails)
            .putLong(kWindow, windowStart)
            .putLong(keyFor(sender, "last"), now)
        if (fails >= MAX_FAILS) {
            edit.putLong(keyFor(sender, "lock"), now + LOCKOUT_MS)
        }
        edit.apply()
    }

    @Synchronized
    fun recordSuccess(sender: String) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putInt(keyFor(sender, "fails"), 0)
            .putLong(keyFor(sender, "window"), now)
            .putLong(keyFor(sender, "last"), now)
            .remove(keyFor(sender, "lock"))
            .apply()
    }
}
