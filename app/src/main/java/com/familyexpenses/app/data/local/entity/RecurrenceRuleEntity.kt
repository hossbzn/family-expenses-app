package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.RecurrenceFrequency

@Entity(
    tableName = "recurrence_rules",
    foreignKeys = [
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("transactionId"),
    ],
)
data class RecurrenceRuleEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val frequency: RecurrenceFrequency,
    val intervalCount: Int = 1,
    val dayOfMonth: Int? = null,
    val dayOfWeek: Int? = null,
    val nextOccurrenceAt: Long,
    val isActive: Boolean = true,
    val createdAt: Long,
)
