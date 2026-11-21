package com.familyexpenses.app.domain.model

import com.familyexpenses.app.core.model.TransactionType

data class AddExpenseRequest(
    val type: TransactionType,
    val amountMinor: Long,
    val payingAccountId: String,
    val responsibleAccountId: String,
    val categoryId: String,
    val note: String?,
    val occurredAt: Long,
)
