package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyexpenses.app.core.model.OccurrenceStatus
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceOccurrenceDao {

    @Upsert
    suspend fun upsert(occurrence: RecurrenceOccurrenceEntity)

    @Upsert
    suspend fun upsertAll(occurrences: List<RecurrenceOccurrenceEntity>)

    @Query("SELECT * FROM recurrence_occurrences WHERE id = :id LIMIT 1")
    suspend fun getOccurrenceById(id: String): RecurrenceOccurrenceEntity?

    @Query("SELECT * FROM recurrence_occurrences WHERE ruleId = :ruleId ORDER BY scheduledAt DESC")
    fun observeOccurrencesByRule(ruleId: String): Flow<List<RecurrenceOccurrenceEntity>>

    @Query("SELECT * FROM recurrence_occurrences WHERE status = 'PENDING' AND (reviewAt IS NULL OR reviewAt <= :now) ORDER BY COALESCE(reviewAt, scheduledAt) ASC")
    fun observePendingOccurrences(now: Long = System.currentTimeMillis()): Flow<List<RecurrenceOccurrenceEntity>>

    @Query("SELECT * FROM recurrence_occurrences WHERE status = 'PENDING' AND (reviewAt IS NULL OR reviewAt <= :now) ORDER BY COALESCE(reviewAt, scheduledAt) ASC")
    suspend fun getPendingOccurrences(now: Long = System.currentTimeMillis()): List<RecurrenceOccurrenceEntity>

    @Query("SELECT * FROM recurrence_occurrences ORDER BY scheduledAt ASC")
    suspend fun getAllOccurrences(): List<RecurrenceOccurrenceEntity>

    @Query("UPDATE recurrence_occurrences SET status = :status, transactionId = :transactionId WHERE id = :id")
    suspend fun updateStatus(id: String, status: OccurrenceStatus, transactionId: String? = null)

    @Query("SELECT EXISTS(SELECT 1 FROM recurrence_occurrences WHERE ruleId = :ruleId AND scheduledAt = :scheduledAt)")
    suspend fun hasOccurrenceForDate(ruleId: String, scheduledAt: Long): Boolean
}
