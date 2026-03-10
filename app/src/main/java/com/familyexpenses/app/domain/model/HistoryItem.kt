package com.familyexpenses.app.domain.model

import com.familyexpenses.app.core.model.AccountType

data class HistoryItem(
    val id: String,
    val amountMinor: Long,
    val occurredAt: Long,
    val accountName: String,
    val accountType: AccountType,
    val categoryName: String?,
    val note: String?,
    val paidFromPersonal: Boolean,
)
