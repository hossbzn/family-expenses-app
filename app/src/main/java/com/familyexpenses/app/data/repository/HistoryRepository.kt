package com.familyexpenses.app.data.repository

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.domain.model.HistoryItem
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class HistoryRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) {

    fun observeHistory(): Flow<List<HistoryItem>> = combine(
        transactionDao.observeTransactions(),
        accountDao.observeAccounts(),
        categoryDao.observeCategories(),
    ) { transactions, accounts, categories ->
        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }

        transactions.map { transaction ->
            val account = accountMap[transaction.accountId]
            val category = transaction.categoryId?.let(categoryMap::get)

            HistoryItem(
                id = transaction.id,
                amountMinor = transaction.amountMinor,
                occurredAt = transaction.occurredAt,
                accountName = account?.name ?: "Cuenta desconocida",
                accountType = account?.type ?: AccountType.OTHER,
                categoryName = category?.name,
                note = transaction.note,
                paidFromPersonal = transaction.paidFromPersonal,
            )
        }
    }
}
