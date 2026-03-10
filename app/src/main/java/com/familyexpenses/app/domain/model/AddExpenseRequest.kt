package com.familyexpenses.app.domain.model

import com.familyexpenses.app.core.model.TransactionType

data class AddExpenseRequest(
    val type: TransactionType,
    val amountMinor: Long,
    val accountId: String,
    val categoryId: String,
    val note: String?,
    val paidFromPersonal: Boolean,
    val occurredAt: Long,
)
