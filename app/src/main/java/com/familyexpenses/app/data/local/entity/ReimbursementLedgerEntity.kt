package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.LedgerStatus

@Entity(
    tableName = "reimbursement_ledger",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["familyAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["personalAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sourceTransactionId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("familyAccountId"),
        Index("personalAccountId"),
        Index("sourceTransactionId"),
    ],
)
data class ReimbursementLedgerEntity(
    @PrimaryKey val id: String,
    val familyAccountId: String,
    val personalAccountId: String,
    val sourceTransactionId: String,
    val amountMinor: Long,
    val status: LedgerStatus = LedgerStatus.OPEN,
    val createdAt: Long,
    val settledAt: Long? = null,
)
