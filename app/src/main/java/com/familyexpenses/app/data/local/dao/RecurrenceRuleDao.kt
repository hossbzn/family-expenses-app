package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceRuleDao {

    @Upsert
    suspend fun upsert(rule: RecurrenceRuleEntity)

    @Query("SELECT * FROM recurrence_rules WHERE isActive = 1 ORDER BY nextOccurrenceAt ASC")
    suspend fun getActiveRules(): List<RecurrenceRuleEntity>

    @Query("SELECT * FROM recurrence_rules ORDER BY nextOccurrenceAt ASC")
    fun observeRules(): Flow<List<RecurrenceRuleEntity>>

    @Query("UPDATE recurrence_rules SET nextOccurrenceAt = :nextOccurrenceAt WHERE id = :ruleId")
    suspend fun updateNextOccurrence(ruleId: String, nextOccurrenceAt: Long)

    @Query("UPDATE recurrence_rules SET isActive = :isActive WHERE id = :ruleId")
    suspend fun updateActiveState(ruleId: String, isActive: Boolean)
}
