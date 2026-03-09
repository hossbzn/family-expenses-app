package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "settlement_items",
    foreignKeys = [
        ForeignKey(
            entity = SettlementEntity::class,
            parentColumns = ["id"],
            childColumns = ["settlementId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ReimbursementLedgerEntity::class,
            parentColumns = ["id"],
            childColumns = ["ledgerId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("settlementId"),
        Index("ledgerId"),
    ],
)
data class SettlementItemEntity(
    @PrimaryKey val id: String,
    val settlementId: String,
    val ledgerId: String,
    val amountMinor: Long,
)
