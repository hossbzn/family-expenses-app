package com.familyexpenses.app.domain.model

data class DashboardSummary(
    val personalBalanceMinor: Long = 0L,
    val familyBalanceMinor: Long = 0L,
    val pendingReimbursementMinor: Long = 0L,
)
