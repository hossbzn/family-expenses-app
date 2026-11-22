package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyexpenses.app.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {

    @Upsert
    suspend fun insertTransfer(transfer: TransferEntity)

    @Query("SELECT * FROM transfers ORDER BY occurredAt DESC")
    fun observeTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers ORDER BY occurredAt DESC")
    suspend fun getAllTransfers(): List<TransferEntity>

    @Query("SELECT * FROM transfers WHERE id = :transferId LIMIT 1")
    suspend fun getTransferById(transferId: String): TransferEntity?

    @Query("DELETE FROM transfers WHERE id = :transferId")
    suspend fun deleteTransfer(transferId: String)
}
