package com.familyexpenses.app.data.repository

import androidx.room.withTransaction
import com.familyexpenses.app.data.local.AppDatabase
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.BalanceAdjustmentDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.RecurrenceOccurrenceDao
import com.familyexpenses.app.data.local.dao.RecurrenceRuleDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.TransferDao
import com.familyexpenses.app.domain.model.BackupPreferencesSnapshot
import com.familyexpenses.app.domain.model.PortableBackupSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class RoomBackupRepository @Inject constructor(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val transactionDao: TransactionDao,
    private val transferDao: TransferDao,
    private val balanceAdjustmentDao: BalanceAdjustmentDao,
    private val recurrenceRuleDao: RecurrenceRuleDao,
    private val recurrenceOccurrenceDao: RecurrenceOccurrenceDao,
    private val preferencesRepository: UserPreferencesRepository,
) : BackupRepository {

    override suspend fun readSnapshot(): PortableBackupSnapshot {
        val accounts = accountDao.getAllAccounts()
        val categories = categoryDao.observeCategories().first()
        val transactions = transactionDao.getAllTransactions()
        val transfers = transferDao.getAllTransfers()
        val adjustments = balanceAdjustmentDao.getAllAdjustments()
        val rules = recurrenceRuleDao.observeRules().first()
        val occurrences = recurrenceOccurrenceDao.getAllOccurrences()
        val privacyModeEnabled = preferencesRepository.observePrivacyModeEnabled().first()

        val accountIcons = buildMap {
            accounts.forEach { account ->
                preferencesRepository.getAccountIcon(account.id)?.let { icon ->
                    put(account.id, icon)
                }
            }
        }

        return PortableBackupSnapshot(
            exportedAt = System.currentTimeMillis(),
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            transfers = transfers,
            balanceAdjustments = adjustments,
            recurrenceRules = rules,
            recurrenceOccurrences = occurrences,
            preferences = BackupPreferencesSnapshot(
                lastExpensePayingAccountId = preferencesRepository.getLastExpensePayingAccountId(),
                lastExpenseResponsibleAccountId = preferencesRepository.getLastExpenseResponsibleAccountId(),
                lastExpenseCategoryId = preferencesRepository.getLastExpenseCategoryId(),
                lastIncomeAccountId = preferencesRepository.getLastIncomeAccountId(),
                lastIncomeCategoryId = preferencesRepository.getLastIncomeCategoryId(),
                privacyModeEnabled = privacyModeEnabled,
                accountIcons = accountIcons,
            ),
        )
    }

    override suspend fun replaceAll(snapshot: PortableBackupSnapshot) {
        database.withTransaction {
            val writableDb = database.openHelper.writableDatabase
            // Borrado en orden inverso de claves foráneas
            writableDb.execSQL("DELETE FROM recurrence_occurrences")
            writableDb.execSQL("DELETE FROM transactions")
            writableDb.execSQL("DELETE FROM transfers")
            writableDb.execSQL("DELETE FROM balance_adjustments")
            writableDb.execSQL("DELETE FROM recurrence_rules")
            writableDb.execSQL("DELETE FROM categories")
            writableDb.execSQL("DELETE FROM accounts")

            // Inserción atómica padre -> hijo
            accountDao.upsertAll(snapshot.accounts)
            categoryDao.upsertAll(snapshot.categories)
            snapshot.recurrenceRules.forEach { recurrenceRuleDao.upsert(it) }
            snapshot.transactions.forEach { transactionDao.insertTransaction(it) }
            snapshot.transfers.forEach { transferDao.insertTransfer(it) }
            snapshot.balanceAdjustments.forEach { balanceAdjustmentDao.insertAdjustment(it) }
            recurrenceOccurrenceDao.upsertAll(snapshot.recurrenceOccurrences)
        }

        database.invalidationTracker.refreshVersionsAsync()

        preferencesRepository.setPrivacyModeEnabled(snapshot.preferences.privacyModeEnabled)

        snapshot.preferences.lastExpensePayingAccountId?.let {
            preferencesRepository.setLastExpensePayingAccountId(it)
        }
        snapshot.preferences.lastExpenseResponsibleAccountId?.let {
            preferencesRepository.setLastExpenseResponsibleAccountId(it)
        }
        snapshot.preferences.lastExpenseCategoryId?.let {
            preferencesRepository.setLastExpenseCategoryId(it)
        }
        snapshot.preferences.lastIncomeAccountId?.let {
            preferencesRepository.setLastIncomeAccountId(it)
        }
        snapshot.preferences.lastIncomeCategoryId?.let {
            preferencesRepository.setLastIncomeCategoryId(it)
        }
        snapshot.preferences.accountIcons.forEach { (accountId, icon) ->
            preferencesRepository.setAccountIcon(accountId, icon)
        }
    }
}
