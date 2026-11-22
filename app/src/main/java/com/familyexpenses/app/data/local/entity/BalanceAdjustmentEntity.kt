package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "balance_adjustments",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("accountId"),
        Index("occurredAt"),
    ],
)
data class BalanceAdjustmentEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val amountDeltaMinor: Long,
    val reason: String,
    val occurredAt: Long,
    val createdAt: Long,
)
