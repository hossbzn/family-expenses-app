package com.familyexpenses.app.domain.model

data class PendingSettlementSummary(
    val netDebtMinor: Long = 0L, // Positivo: Familia debe a Personal. Negativo: Personal debe a Familia.
    val pendingAdvancesCount: Int = 0,
)
