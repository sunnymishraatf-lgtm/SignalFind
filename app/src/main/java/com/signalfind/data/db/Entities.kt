package com.signalfind.data.db

/** A phone number allowed to send recovery commands (normalized form). */
data class TrustedNumber(
    val id: Long = 0,
    val number: String,
    val label: String,
    val addedAt: Long = System.currentTimeMillis()
)

/**
 * One processed recovery command. Sender is stored masked (last 4 digits)
 * so the history never becomes a list of full phone numbers.
 */
data class RecoveryEvent(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val senderMasked: String,
    val command: String,
    /** LIVE, CACHED, NONE, or TEST. */
    val locationState: String,
    val accuracyM: Float?,
    val latitude: Double?,
    val longitude: Double?,
    /** SENT, FAILED, PREVIEW, or DENIED. */
    val replyStatus: String
)
