package com.familyexpenses.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(defaultValue = "0") val amountMinor: Long,
    val settledAt: Long,
    val note: String? = null,
    val createdAt: Long,
)
