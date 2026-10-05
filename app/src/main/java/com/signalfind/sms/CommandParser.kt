package com.signalfind.sms

/**
 * Secure command parser for recovery SMS.
 *
 * Accepted shapes (case-insensitive, extra whitespace tolerated):
 *   SIGNALFIND <PIN> LOCATION | BATTERY | RING | STATUS
 *   <EMERGENCY_KEYWORD> <PIN>            (alias for LOCATION, default FINDFONE)
 *
 * Anything else -> null (ignored). The PIN must be 4-8 digits; malformed
 * input never reaches authentication.
 */
object CommandParser {

    sealed interface Command {
        data object Location : Command
        data object Battery : Command
        data object Ring : Command
        data object Status : Command
    }

    data class Parsed(val pin: String, val command: Command)

    private val PIN_PATTERN = Regex("^\\d{4,8}$")

    fun parse(body: String, emergencyKeyword: String = "FINDFONE"): Parsed? {
        val tokens = body.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return null
        val head = tokens[0].uppercase()

        return when {
            head == "SIGNALFIND" && tokens.size == 3 -> {
                val pin = tokens[1]
                val cmd = when (tokens[2].uppercase()) {
                    "LOCATION" -> Command.Location
                    "BATTERY" -> Command.Battery
                    "RING" -> Command.Ring
                    "STATUS" -> Command.Status
                    else -> return null
                }
                if (!PIN_PATTERN.matches(pin)) return null
                Parsed(pin, cmd)
            }
            head == emergencyKeyword.uppercase() && tokens.size == 2 -> {
                val pin = tokens[1]
                if (!PIN_PATTERN.matches(pin)) return null
                Parsed(pin, Command.Location)
            }
            else -> null
        }
    }

    /** Cheap pre-filter so unrelated SMS are ignored before any auth work. */
    fun looksLikeCommand(body: String, emergencyKeyword: String = "FINDFONE"): Boolean {
        val head = body.trim().split("\\s+".toRegex(), limit = 2)
            .firstOrNull()?.uppercase() ?: return false
        return head == "SIGNALFIND" || head == emergencyKeyword.uppercase()
    }
}
