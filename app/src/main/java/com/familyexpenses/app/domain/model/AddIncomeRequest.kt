package com.familyexpenses.app.domain.model

data class AddIncomeRequest(
    val amountMinor: Long,
    val destinationAccountId: String,
    val categoryId: String,
    val note: String? = null,
    val occurredAt: Long = System.currentTimeMillis(),
)
