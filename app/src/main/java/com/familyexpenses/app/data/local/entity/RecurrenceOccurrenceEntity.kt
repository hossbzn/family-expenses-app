package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.OccurrenceStatus
import com.familyexpenses.app.core.model.TransactionType

@Entity(
    tableName = "recurrence_occurrences",
    foreignKeys = [
        ForeignKey(
            entity = RecurrenceRuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["ruleId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("ruleId"),
        Index("transactionId"),
        Index("status"),
        Index("scheduledAt"),
    ],
)
data class RecurrenceOccurrenceEntity(
    @PrimaryKey val id: String,
    val ruleId: String,
    val status: OccurrenceStatus,
    val scheduledAt: Long,
    val amountMinor: Long,
    val type: TransactionType,
    val payingAccountId: String,
    val responsibleAccountId: String,
    val categoryId: String,
    val note: String?,
    val transactionId: String? = null,
    val createdAt: Long,
)
