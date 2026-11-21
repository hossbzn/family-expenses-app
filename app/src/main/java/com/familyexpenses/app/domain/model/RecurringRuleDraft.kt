package com.familyexpenses.app.domain.model

import com.familyexpenses.app.core.model.TransactionType

data class RecurringRuleDraft(
    val type: TransactionType,
    val payingAccountId: String,
    val responsibleAccountId: String,
    val categoryId: String,
    val amountMinor: Long,
    val note: String?,
    val startAt: Long,
    val endAt: Long? = null,
)
