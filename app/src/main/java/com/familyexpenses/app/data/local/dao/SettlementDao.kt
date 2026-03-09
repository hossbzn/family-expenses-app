package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Upsert
import com.familyexpenses.app.data.local.entity.SettlementEntity
import com.familyexpenses.app.data.local.entity.SettlementItemEntity

@Dao
interface SettlementDao {

    @Upsert
    suspend fun upsertSettlement(settlement: SettlementEntity)

    @Upsert
    suspend fun upsertSettlementItems(items: List<SettlementItemEntity>)
}
