package com.familyexpenses.app.domain.model

data class DashboardSummary(
    val personalCashBalanceMinor: Long = 0L,
    val familyCashBalanceMinor: Long = 0L,
    val netDebtMinor: Long = 0L, // Positivo: Familia debe a Personal. Negativo: Personal debe a Familia.
    val personalMonthlyNetMinor: Long = 0L,
    val familyMonthlyNetMinor: Long = 0L,
    val currentMonthName: String = "",
)
