package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BalanceAdjustmentDao {

    @Upsert
    suspend fun insertAdjustment(adjustment: BalanceAdjustmentEntity)

    @Query("SELECT * FROM balance_adjustments ORDER BY occurredAt DESC")
    fun observeAdjustments(): Flow<List<BalanceAdjustmentEntity>>

    @Query("SELECT * FROM balance_adjustments WHERE accountId = :accountId ORDER BY occurredAt DESC")
    fun observeAdjustmentsForAccount(accountId: String): Flow<List<BalanceAdjustmentEntity>>

    @Query("SELECT * FROM balance_adjustments ORDER BY occurredAt DESC")
    suspend fun getAllAdjustments(): List<BalanceAdjustmentEntity>
}
