package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyexpenses.app.data.local.entity.ReimbursementLedgerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReimbursementLedgerDao {

    @Upsert
    suspend fun upsert(entry: ReimbursementLedgerEntity)

    @Query("SELECT * FROM reimbursement_ledger WHERE status = 'OPEN' ORDER BY createdAt ASC")
    fun observeOpenEntries(): Flow<List<ReimbursementLedgerEntity>>
}
