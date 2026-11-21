package com.familyexpenses.app.data.repository

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.TransferDao
import com.familyexpenses.app.domain.model.HistoryItem
import com.familyexpenses.app.domain.model.MovementKind
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Singleton
class HistoryRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) {

    fun observeHistory(): Flow<List<HistoryItem>> = combine(
        transactionDao.observeTransactions(),
        transferDao.observeTransfers(),
        accountDao.observeAccounts(),
        categoryDao.observeCategories(),
    ) { transactions, transfers, accounts, categories ->
        val accountMap = accounts.associateBy { it.id }
        val categoryMap = categories.associateBy { it.id }

        val txItems = transactions.map { transaction ->
            val payingAccount = accountMap[transaction.payingAccountId]
            val responsibleAccount = accountMap[transaction.responsibleAccountId]
            val category = categoryMap[transaction.categoryId]
            val kind = when (transaction.type) {
                TransactionType.EXPENSE -> MovementKind.EXPENSE
                TransactionType.INCOME -> MovementKind.INCOME
                TransactionType.ADJUSTMENT -> MovementKind.EXPENSE
            }

            HistoryItem(
                id = transaction.id,
                kind = kind,
                amountMinor = transaction.amountMinor,
                occurredAt = transaction.occurredAt,
                createdAt = transaction.createdAt,
                payingAccountId = transaction.payingAccountId,
                payingAccountName = payingAccount?.name ?: "Cuenta desconocida",
                payingAccountType = payingAccount?.type ?: AccountType.OTHER,
                responsibleAccountId = transaction.responsibleAccountId,
                responsibleAccountName = responsibleAccount?.name ?: "Cuenta desconocida",
                responsibleAccountType = responsibleAccount?.type ?: AccountType.OTHER,
                categoryId = transaction.categoryId,
                categoryName = category?.name,
                categoryIcon = category?.icon,
                note = transaction.note,
                isAdvance = transaction.type == TransactionType.EXPENSE &&
                    transaction.payingAccountId != transaction.responsibleAccountId,
            )
        }

        val transferItems = transfers.map { transfer ->
            val sourceAccount = accountMap[transfer.sourceAccountId]
            val destinationAccount = accountMap[transfer.destinationAccountId]
            val kind = when (transfer.type) {
                TransferType.NORMAL -> MovementKind.NORMAL_TRANSFER
                TransferType.REIMBURSEMENT -> MovementKind.REIMBURSEMENT
            }

            HistoryItem(
                id = transfer.id,
                kind = kind,
                amountMinor = transfer.amountMinor,
                occurredAt = transfer.occurredAt,
                createdAt = transfer.createdAt,
                payingAccountId = transfer.sourceAccountId,
                payingAccountName = sourceAccount?.name ?: "Cuenta desconocida",
                payingAccountType = sourceAccount?.type ?: AccountType.OTHER,
                responsibleAccountId = transfer.destinationAccountId,
                responsibleAccountName = destinationAccount?.name ?: "Cuenta desconocida",
                responsibleAccountType = destinationAccount?.type ?: AccountType.OTHER,
                categoryId = null,
                categoryName = null,
                categoryIcon = null,
                note = transfer.note,
                isAdvance = false,
            )
        }

        (txItems + transferItems).sortedWith(
            compareByDescending<HistoryItem> { it.occurredAt }
                .thenByDescending { it.createdAt },
        )
    }
}
