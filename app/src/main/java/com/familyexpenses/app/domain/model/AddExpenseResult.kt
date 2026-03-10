package com.familyexpenses.app.domain.model

data class AddExpenseResult(
    val transactionId: String,
    val createdReimbursementLedger: Boolean,
)
