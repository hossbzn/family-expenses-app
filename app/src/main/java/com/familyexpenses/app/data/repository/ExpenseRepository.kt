package com.familyexpenses.app.data.repository

import androidx.room.withTransaction
import com.familyexpenses.app.core.model.LedgerStatus
import com.familyexpenses.app.data.local.AppDatabase
import com.familyexpenses.app.data.local.dao.ReimbursementLedgerDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.entity.ReimbursementLedgerEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val database: AppDatabase,
    private val transactionDao: TransactionDao,
    private val reimbursementLedgerDao: ReimbursementLedgerDao,
) {

    suspend fun addExpense(
        transaction: TransactionEntity,
        createReimbursementLedger: Boolean,
        familyAccountId: String,
        personalAccountId: String,
    ) {
        database.withTransaction {
            transactionDao.insertTransaction(transaction)

            if (createReimbursementLedger) {
                reimbursementLedgerDao.upsert(
                    ReimbursementLedgerEntity(
                        id = UUID.randomUUID().toString(),
                        familyAccountId = familyAccountId,
                        personalAccountId = personalAccountId,
                        sourceTransactionId = transaction.id,
                        amountMinor = transaction.amountMinor,
                        status = LedgerStatus.OPEN,
                        createdAt = transaction.createdAt,
                    ),
                )
            }
        }
    }
}
