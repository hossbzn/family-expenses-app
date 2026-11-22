package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.TransferType

@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("sourceAccountId"),
        Index("destinationAccountId"),
        Index("occurredAt"),
    ],
)
data class TransferEntity(
    @PrimaryKey val id: String,
    val type: TransferType,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amountMinor: Long,
    val occurredAt: Long,
    val note: String? = null,
    val createdAt: Long,
)
