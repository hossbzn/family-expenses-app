package com.familyexpenses.app.domain.model

data class AddExpenseResult(
    val transactionId: String,
    val isAdvance: Boolean,
)
