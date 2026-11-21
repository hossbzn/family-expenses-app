package com.familyexpenses.app.data.repository

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.BalanceAdjustmentDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.TransferDao
import com.familyexpenses.app.domain.model.DashboardSummary
import com.familyexpenses.app.domain.usecase.ComputeAccountBalanceUseCase
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class DashboardRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
    private val balanceAdjustmentDao: BalanceAdjustmentDao,
    private val computeAccountBalanceUseCase: ComputeAccountBalanceUseCase,
    private val computeNetDebtUseCase: ComputeNetDebtUseCase,
) {

    fun observeSummary(): Flow<DashboardSummary> = combine(
        accountDao.observeAccounts(),
        transactionDao.observeTransactions(),
        transferDao.observeTransfers(),
        balanceAdjustmentDao.observeAdjustments(),
    ) { accounts, transactions, transfers, adjustments ->
        val personalAccount = accounts.firstOrNull { it.type == AccountType.PERSONAL }
        val familyAccount = accounts.firstOrNull { it.type == AccountType.FAMILY }

        val personalCash = personalAccount?.let {
            computeAccountBalanceUseCase.compute(
                account = it,
                transactions = transactions,
                transfers = transfers,
                adjustments = adjustments,
            )
        } ?: 0L

        val familyCash = familyAccount?.let {
            computeAccountBalanceUseCase.compute(
                account = it,
                transactions = transactions,
                transfers = transfers,
                adjustments = adjustments,
            )
        } ?: 0L

        val netDebt = computeNetDebtUseCase.compute(
            accounts = accounts,
            transactions = transactions,
            transfers = transfers,
        )

        DashboardSummary(
            personalCashBalanceMinor = personalCash,
            familyCashBalanceMinor = familyCash,
            netDebtMinor = netDebt,
        )
    }
}
