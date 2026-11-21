package com.familyexpenses.app.domain.model

import com.familyexpenses.app.core.model.AccountType

enum class MovementKind {
    EXPENSE,
    INCOME,
    NORMAL_TRANSFER,
    REIMBURSEMENT,
}

data class HistoryItem(
    val id: String,
    val kind: MovementKind,
    val amountMinor: Long,
    val occurredAt: Long,
    val createdAt: Long,
    val payingAccountId: String,
    val payingAccountName: String,
    val payingAccountType: AccountType,
    val responsibleAccountId: String,
    val responsibleAccountName: String,
    val responsibleAccountType: AccountType,
    val categoryId: String?,
    val categoryName: String?,
    val categoryIcon: String?,
    val note: String?,
    val isAdvance: Boolean,
)
