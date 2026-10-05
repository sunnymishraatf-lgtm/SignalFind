package com.signalfind.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.signalfind.util.PhoneNumberUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Local storage for trusted numbers and recovery history, built on
 * [SQLiteOpenHelper] directly. No annotation processing is required, which
 * keeps the build reproducible with a plain kotlinc + aapt2 pipeline as well
 * as with Gradle.
 */
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "signalfind.db"
        private const val DB_VERSION = 1

        private const val T_TRUSTED = "trusted_numbers"
        private const val T_EVENTS = "recovery_events"
    }

    /** Bumped after every write so [Flow] observers re-query. */
    private val version = MutableStateFlow(0L)

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $T_TRUSTED (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                number TEXT NOT NULL,
                label TEXT NOT NULL,
                addedAt INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE $T_EVENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp INTEGER NOT NULL,
                senderMasked TEXT NOT NULL,
                command TEXT NOT NULL,
                locationState TEXT NOT NULL,
                accuracyM REAL,
                latitude REAL,
                longitude REAL,
                replyStatus TEXT NOT NULL
            )"""
        )
        db.execSQL("CREATE INDEX idx_events_ts ON $T_EVENTS(timestamp DESC)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // v1: nothing to migrate.
    }

    private fun bump() {
        version.value = version.value + 1
    }

    // ---- Trusted numbers ----

    fun observeTrustedNumbers(): Flow<List<TrustedNumber>> =
        version.map { queryTrustedNumbers() }.flowOn(Dispatchers.IO)

    suspend fun addTrustedNumber(number: String, label: String): Long =
        withContext(Dispatchers.IO) {
            val normalized = PhoneNumberUtils.normalize(number)
            require(PhoneNumberUtils.isPlausible(normalized)) { "Not a plausible phone number" }
            // Never store the same number twice (country-code variants count).
            queryTrustedNumbers()
                .firstOrNull { PhoneNumberUtils.matches(it.number, normalized) }
                ?.let { return@withContext it.id }
            val values = ContentValues().apply {
                put("number", normalized)
                put("label", label.ifBlank { "Recovery phone" })
                put("addedAt", System.currentTimeMillis())
            }
            val id = writableDatabase.insertOrThrow(T_TRUSTED, null, values)
            bump()
            id
        }

    suspend fun deleteTrustedNumber(entry: TrustedNumber): Unit =
        withContext(Dispatchers.IO) {
            writableDatabase.delete(T_TRUSTED, "id = ?", arrayOf(entry.id.toString()))
            bump()
        }

    suspend fun trustedNumberCount(): Int = withContext(Dispatchers.IO) {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_TRUSTED", null).use { c ->
            if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    /** True when [sender] matches any trusted entry (see [PhoneNumberUtils.matches]). */
    suspend fun isTrustedNumber(sender: String): Boolean = withContext(Dispatchers.IO) {
        queryTrustedNumbers().any { PhoneNumberUtils.matches(it.number, sender) }
    }

    private fun queryTrustedNumbers(): List<TrustedNumber> {
        val out = ArrayList<TrustedNumber>()
        readableDatabase.rawQuery(
            "SELECT id, number, label, addedAt FROM $T_TRUSTED ORDER BY addedAt ASC", null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    TrustedNumber(
                        id = c.getLong(0),
                        number = c.getString(1),
                        label = c.getString(2),
                        addedAt = c.getLong(3)
                    )
                )
            }
        }
        return out
    }

    // ---- Recovery history ----

    fun observeEvents(): Flow<List<RecoveryEvent>> =
        version.map { queryEvents() }.flowOn(Dispatchers.IO)

    suspend fun insertEvent(event: RecoveryEvent): Long = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("timestamp", event.timestamp)
            put("senderMasked", event.senderMasked)
            put("command", event.command)
            put("locationState", event.locationState)
            put("accuracyM", event.accuracyM)
            put("latitude", event.latitude)
            put("longitude", event.longitude)
            put("replyStatus", event.replyStatus)
        }
        val id = writableDatabase.insertOrThrow(T_EVENTS, null, values)
        bump()
        id
    }

    suspend fun deleteEvent(id: Long): Unit = withContext(Dispatchers.IO) {
        writableDatabase.delete(T_EVENTS, "id = ?", arrayOf(id.toString()))
        bump()
    }

    suspend fun clearEvents(): Unit = withContext(Dispatchers.IO) {
        writableDatabase.delete(T_EVENTS, null, null)
        bump()
    }

    private fun queryEvents(): List<RecoveryEvent> {
        val out = ArrayList<RecoveryEvent>()
        readableDatabase.rawQuery(
            """SELECT id, timestamp, senderMasked, command, locationState,
                      accuracyM, latitude, longitude, replyStatus
               FROM $T_EVENTS ORDER BY timestamp DESC""",
            null
        ).use { c ->
            while (c.moveToNext()) {
                out.add(
                    RecoveryEvent(
                        id = c.getLong(0),
                        timestamp = c.getLong(1),
                        senderMasked = c.getString(2),
                        command = c.getString(3),
                        locationState = c.getString(4),
                        accuracyM = if (c.isNull(5)) null else c.getFloat(5),
                        latitude = if (c.isNull(6)) null else c.getDouble(6),
                        longitude = if (c.isNull(7)) null else c.getDouble(7),
                        replyStatus = c.getString(8)
                    )
                )
            }
        }
        return out
    }
}
