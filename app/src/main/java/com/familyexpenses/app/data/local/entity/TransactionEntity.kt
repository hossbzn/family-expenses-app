package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.TransactionType

@Entity(
    tableName = "transactions",
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
data class TransactionEntity(
    @PrimaryKey val id: String,
    val accountId: String,
    val categoryId: String?,
    val type: TransactionType,
    val amountMinor: Long,
    val paidFromPersonal: Boolean = false,
    val note: String? = null,
    val occurredAt: Long,
    val createdAt: Long,
)
