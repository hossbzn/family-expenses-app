package com.familyexpenses.app.domain.model

import com.familyexpenses.app.core.model.TransactionType

data class RecurringRuleDraft(
    val type: TransactionType,
    val accountId: String,
    val categoryId: String,
    val amountMinor: Long,
    val paidFromPersonal: Boolean,
    val note: String?,
    val startAt: Long,
    val endAt: Long? = null,
)
