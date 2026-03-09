package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity

@Dao
interface RecurrenceRuleDao {

    @Upsert
    suspend fun upsert(rule: RecurrenceRuleEntity)

    @Query("SELECT * FROM recurrence_rules WHERE isActive = 1")
    suspend fun getActiveRules(): List<RecurrenceRuleEntity>
}
