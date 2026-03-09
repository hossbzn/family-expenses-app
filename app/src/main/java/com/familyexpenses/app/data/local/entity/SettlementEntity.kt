package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: String,
    val settledAt: Long,
    val note: String? = null,
    val createdAt: Long,
)
