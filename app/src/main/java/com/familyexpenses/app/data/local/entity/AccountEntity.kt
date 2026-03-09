package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.AccountType

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val initialBalanceMinor: Long,
    val isArchived: Boolean = false,
    val createdAt: Long,
)
