package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.TransactionType

@Entity(
    tableName = "recurrence_rules",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
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
        Index("accountId"),
        Index("categoryId"),
    ],
)
data class RecurrenceRuleEntity(
    @PrimaryKey val id: String,
    val type: TransactionType,
    val accountId: String,
    val categoryId: String,
    val amountMinor: Long,
    val paidFromPersonal: Boolean = false,
    val note: String? = null,
    val startAt: Long,
    val endAt: Long? = null,
    val nextOccurrenceAt: Long,
    val isActive: Boolean = true,
    val createdAt: Long,
)
