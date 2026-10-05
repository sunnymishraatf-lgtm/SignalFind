package com.signalfind.data.repo

import com.signalfind.data.db.DatabaseHelper
import com.signalfind.data.db.RecoveryEvent
import com.signalfind.data.db.TrustedNumber
import kotlinx.coroutines.flow.Flow

class TrustedNumberRepository(private val db: DatabaseHelper) {
    fun observeAll(): Flow<List<TrustedNumber>> = db.observeTrustedNumbers()

    suspend fun add(number: String, label: String): Long =
        db.addTrustedNumber(number, label)

    suspend fun remove(entry: TrustedNumber) = db.deleteTrustedNumber(entry)

    suspend fun count(): Int = db.trustedNumberCount()

    /** True when [sender] matches any trusted entry (see PhoneNumberUtils.matches). */
    suspend fun isTrusted(sender: String): Boolean = db.isTrustedNumber(sender)
}

class HistoryRepository(private val db: DatabaseHelper) {
    fun observeAll(): Flow<List<RecoveryEvent>> = db.observeEvents()

    suspend fun record(event: RecoveryEvent): Long = db.insertEvent(event)

    suspend fun delete(id: Long) = db.deleteEvent(id)

    suspend fun clear() = db.clearEvents()
}
