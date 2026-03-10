package com.familyexpenses.app.data.repository

import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.ReimbursementLedgerDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.domain.model.DashboardSummary
import com.familyexpenses.app.domain.rule.ComputeMonthlyBalanceRule
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class DashboardRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val reimbursementLedgerDao: ReimbursementLedgerDao,
    private val computeMonthlyBalanceRule: ComputeMonthlyBalanceRule,
) {

    fun observeSummary(
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Flow<DashboardSummary> = combine(
        accountDao.observeAccounts(),
        transactionDao.observeTransactions(),
        reimbursementLedgerDao.observeOpenEntries(),
    ) { accounts, transactions, openLedgerEntries ->
        val monthlyTotals = computeMonthlyBalanceRule.compute(
            accounts = accounts,
            transactions = transactions,
            today = today,
            zoneId = zoneId,
        )

        DashboardSummary(
            personalBalanceMinor = monthlyTotals.personalBalanceMinor,
            familyBalanceMinor = monthlyTotals.familyBalanceMinor,
            pendingReimbursementMinor = openLedgerEntries.sumOf { it.amountMinor },
        )
    }
}
