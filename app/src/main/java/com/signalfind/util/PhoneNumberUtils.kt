package com.signalfind.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Normalizes and compares phone numbers without leaking them into logs. */
object PhoneNumberUtils {

    /** Keeps a leading '+' and digits; drops spaces, dashes, parentheses. */
    fun normalize(raw: String): String {
        val trimmed = raw.trim()
        val sb = StringBuilder()
        trimmed.forEachIndexed { index, c ->
            when {
                c.isDigit() -> sb.append(c)
                c == '+' && index == 0 -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * True when two numbers identify the same subscriber. Exact normalized
     * match wins; otherwise the last 10 digits are compared so "+91 98..."
     * matches "098..." style variants.
     */
    fun matches(a: String, b: String): Boolean {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() || nb.isEmpty()) return false
        if (na == nb) return true
        val da = na.trimStart('+')
        val db = nb.trimStart('+')
        if (da.length >= 10 && db.length >= 10) {
            return da.takeLast(10) == db.takeLast(10)
        }
        return false
    }

    /** Masks a number for display/history: keeps only the last 4 digits. */
    fun mask(raw: String): String {
        val digits = normalize(raw).filter { it.isDigit() }
        return if (digits.length <= 4) "****" else "***" + digits.takeLast(4)
    }

    fun isPlausible(raw: String): Boolean {
        val digits = normalize(raw).filter { it.isDigit() }
        return digits.length in 7..15
    }
}

/** Time formatting helpers shared by SMS replies and the UI. */
object TimeFormat {
    // DateTimeFormatter is immutable and thread-safe — unlike the old
    // SimpleDateFormat, which corrupted output when the SMS receiver thread
    // and the UI thread formatted dates concurrently.
    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val dateTimeFmt = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)

    private fun zoned(ms: Long) = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())

    fun time(ms: Long): String = timeFmt.format(zoned(ms))
    fun dateTime(ms: Long): String = dateTimeFmt.format(zoned(ms))

    fun ageDescription(ms: Long): String {
        val minutes = (System.currentTimeMillis() - ms) / 60_000
        return when {
            minutes < 1 -> "just now"
            minutes == 1L -> "1 minute"
            minutes < 60 -> "$minutes minutes"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
    }
}
