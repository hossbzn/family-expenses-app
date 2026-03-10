package com.familyexpenses.app.domain.model

data class AddExpenseRequest(
    val amountMinor: Long,
    val accountId: String,
    val categoryId: String,
    val note: String?,
    val paidFromPersonal: Boolean,
    val occurredAt: Long,
)
