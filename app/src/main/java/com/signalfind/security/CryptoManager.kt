package com.signalfind.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Keystore-backed secret storage.
 *
 * The recovery PIN is never stored in plaintext: a random 16-byte salt and a
 * PBKDF2-HMAC-SHA256 digest (120,000 iterations) live inside
 * EncryptedSharedPreferences, whose keys are held in the Android Keystore.
 * PIN comparison uses [MessageDigest.isEqual] (constant-time) so verification
 * time does not leak information about the PIN. A deliberately slow KDF is
 * used because a 4–8 digit PIN has low entropy and a fast hash would let an
 * attacker with the encrypted file brute-force it offline.
 */
class CryptoManager(context: Context) {

    companion object {
        const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_KEY_BITS = 256
    }

    private val masterKey: MasterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "signalfind_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun randomSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    /**
     * Slow, salted PIN digest. Legacy [hashPin] (fast SHA-256) is kept only to
     * verify — and transparently migrate — PINs stored by older app versions.
     */
    fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Legacy fast hash — verification + migration only, never for new PINs. */
    fun legacyHashPin(pin: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        return md.digest(pin.toByteArray(Charsets.UTF_8))
    }

    /** Constant-time comparison of a candidate PIN against the stored digest. */
    fun verifyPin(pin: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        return MessageDigest.isEqual(pbkdf2(pin, salt), expectedHash)
    }

    /** Constant-time comparison against a legacy (fast-hash) digest. */
    fun verifyLegacyPin(pin: String, salt: ByteArray, expectedHash: ByteArray): Boolean {
        return MessageDigest.isEqual(legacyHashPin(pin, salt), expectedHash)
    }

    fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    fun decode(s: String): ByteArray = Base64.decode(s, Base64.DEFAULT)
}
