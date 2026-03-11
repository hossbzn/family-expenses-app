package com.familyexpenses.app.domain.model

data class PendingSettlementSummary(
    val pendingAmountMinor: Long = 0L,
    val pendingEntriesCount: Int = 0,
)
