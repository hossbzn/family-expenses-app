package com.familyexpenses.app.data.repository

import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.TransferDao
import com.familyexpenses.app.domain.model.PendingSettlementSummary
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class SettlementRepository @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
    private val computeNetDebtUseCase: ComputeNetDebtUseCase,
) {

    fun observePendingSummary(): Flow<PendingSettlementSummary> = combine(
        accountDao.observeAccounts(),
        transactionDao.observeTransactions(),
        transferDao.observeTransfers(),
    ) { accounts, transactions, transfers ->
        val netDebt = computeNetDebtUseCase.compute(
            accounts = accounts,
            transactions = transactions,
            transfers = transfers,
        )

        val advancesCount = transactions.count {
            it.type == TransactionType.EXPENSE && it.payingAccountId != it.responsibleAccountId
        }

        PendingSettlementSummary(
            netDebtMinor = netDebt,
            pendingAdvancesCount = advancesCount,
        )
    }
}
