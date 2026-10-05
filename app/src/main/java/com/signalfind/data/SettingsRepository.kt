package com.signalfind.data

import android.content.Context
import com.signalfind.security.CryptoManager

/**
 * App settings backed by Keystore-encrypted storage. The PIN itself is never
 * persisted — only a salt and a SHA-256 digest.
 */
class SettingsRepository(
    private val context: Context,
    private val crypto: CryptoManager
) {
    companion object {
        private const val KEY_SETUP = "setup_complete"
        private const val KEY_RECOVERY_ENABLED = "recovery_enabled"
        private const val KEY_TRUSTED_ONLY = "trusted_only"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_KDF = "pin_kdf"
        private const val KDF_PBKDF2 = "pbkdf2"
        private const val KEY_PREF_SUB_ID = "preferred_sub_id"
        private const val KEY_EMERGENCY_KEYWORD = "emergency_keyword"

        const val DEFAULT_EMERGENCY_KEYWORD = "FINDFONE"
        const val NO_SUB_ID = -1

        private val PIN_PATTERN = Regex("^\\d{4,8}$")
    }

    private val prefs get() = crypto.securePrefs

    fun isSetupComplete(): Boolean = prefs.getBoolean(KEY_SETUP, false)
    fun setSetupComplete(done: Boolean) =
        prefs.edit().putBoolean(KEY_SETUP, done).apply()

    fun isRecoveryEnabled(): Boolean = prefs.getBoolean(KEY_RECOVERY_ENABLED, false)
    fun setRecoveryEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(KEY_RECOVERY_ENABLED, enabled).apply()

    /** "Only accept commands from trusted numbers". Enabled by default. */
    fun isTrustedOnly(): Boolean = prefs.getBoolean(KEY_TRUSTED_ONLY, true)
    fun setTrustedOnly(only: Boolean) =
        prefs.edit().putBoolean(KEY_TRUSTED_ONLY, only).apply()

    fun hasPin(): Boolean =
        prefs.contains(KEY_PIN_SALT) && prefs.contains(KEY_PIN_HASH)

    /** @throws IllegalArgumentException when the PIN is not 4-8 digits. */
    fun setPin(pin: String) {
        require(PIN_PATTERN.matches(pin)) { "PIN must be 4-8 digits" }
        val salt = crypto.randomSalt()
        val hash = crypto.pbkdf2(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_SALT, crypto.encode(salt))
            .putString(KEY_PIN_HASH, crypto.encode(hash))
            .putString(KEY_PIN_KDF, KDF_PBKDF2)
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        if (!PIN_PATTERN.matches(pin)) return false
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return try {
            val salt = crypto.decode(saltB64)
            val expected = crypto.decode(hashB64)
            val ok = if (prefs.getString(KEY_PIN_KDF, null) == KDF_PBKDF2) {
                crypto.verifyPin(pin, salt, expected)
            } else {
                crypto.verifyLegacyPin(pin, salt, expected)
            }
            // Transparent migration: a correct legacy PIN is re-hashed with
            // the slow KDF so the weak digest does not linger on disk.
            if (ok && prefs.getString(KEY_PIN_KDF, null) != KDF_PBKDF2) {
                setPin(pin)
            }
            ok
        } catch (_: Exception) {
            false
        }
    }

    fun getPreferredSubId(): Int = prefs.getInt(KEY_PREF_SUB_ID, NO_SUB_ID)
    fun setPreferredSubId(subId: Int) =
        prefs.edit().putInt(KEY_PREF_SUB_ID, subId).apply()

    fun getEmergencyKeyword(): String =
        prefs.getString(KEY_EMERGENCY_KEYWORD, DEFAULT_EMERGENCY_KEYWORD)
            ?: DEFAULT_EMERGENCY_KEYWORD

    fun setEmergencyKeyword(keyword: String) {
        val clean = keyword.trim().uppercase().replace("\\s+".toRegex(), "")
        require(clean.length in 3..16 && clean.matches(Regex("^[A-Z0-9]+$"))) {
            "Keyword must be 3-16 letters/digits"
        }
        prefs.edit().putString(KEY_EMERGENCY_KEYWORD, clean).apply()
    }
}
