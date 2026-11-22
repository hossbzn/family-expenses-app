package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyexpenses.app.core.model.RecurrenceRuleStatus
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceRuleDao {

    @Upsert
    suspend fun upsert(rule: RecurrenceRuleEntity)

    @Query("SELECT * FROM recurrence_rules WHERE status = 'ACTIVE' ORDER BY nextOccurrenceAt ASC")
    suspend fun getActiveRules(): List<RecurrenceRuleEntity>

    @Query("SELECT * FROM recurrence_rules ORDER BY nextOccurrenceAt ASC")
    fun observeRules(): Flow<List<RecurrenceRuleEntity>>

    @Query("SELECT * FROM recurrence_rules WHERE status = :status ORDER BY nextOccurrenceAt ASC")
    fun observeRulesByStatus(status: RecurrenceRuleStatus): Flow<List<RecurrenceRuleEntity>>

    @Query("SELECT * FROM recurrence_rules WHERE id = :ruleId LIMIT 1")
    suspend fun getRuleById(ruleId: String): RecurrenceRuleEntity?

    @Query("UPDATE recurrence_rules SET nextOccurrenceAt = :nextOccurrenceAt WHERE id = :ruleId")
    suspend fun updateNextOccurrence(ruleId: String, nextOccurrenceAt: Long)

    @Query("UPDATE recurrence_rules SET status = :status WHERE id = :ruleId")
    suspend fun updateStatus(ruleId: String, status: RecurrenceRuleStatus)
}
