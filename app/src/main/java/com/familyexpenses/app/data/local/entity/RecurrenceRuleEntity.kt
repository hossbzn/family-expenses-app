package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.TransactionType

@Entity(
    tableName = "recurrence_rules",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["payingAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["responsibleAccountId"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index("payingAccountId"),
        Index("responsibleAccountId"),
        Index("categoryId"),
        Index("nextOccurrenceAt"),
    ],
)
data class RecurrenceRuleEntity(
    @PrimaryKey val id: String,
    val type: TransactionType,
    val payingAccountId: String,
    val responsibleAccountId: String,
    val categoryId: String,
    val amountMinor: Long,
    val note: String? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val dayOfMonth: Int = 1,
    val startAt: Long,
    val endAt: Long? = null,
    val nextOccurrenceAt: Long,
    val isActive: Boolean = true,
    val createdAt: Long,
)
