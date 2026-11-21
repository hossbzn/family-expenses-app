package com.familyexpenses.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType

@Entity(
    tableName = "transactions",
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
        Index("occurredAt"),
    ],
)
data class TransactionEntity(
    @PrimaryKey val id: String,
    val type: TransactionType,
    val amountMinor: Long,
    val payingAccountId: String,
    val responsibleAccountId: String,
    val categoryId: String,
    val occurredAt: Long,
    val note: String? = null,
    @ColumnInfo(defaultValue = "MANUAL") val source: TransactionSource = TransactionSource.MANUAL,
    val recurrenceRuleId: String? = null,
    val createdAt: Long,
)
