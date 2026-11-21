package com.familyexpenses.app.data.repository

import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val transactionDao: TransactionDao,
) {

    fun observeTransactions(): Flow<List<TransactionEntity>> = transactionDao.observeTransactions()

    suspend fun getAllTransactions(): List<TransactionEntity> = transactionDao.getAllTransactions()

    suspend fun getTransactionById(id: String): TransactionEntity? = transactionDao.getTransactionById(id)

    suspend fun addTransaction(transaction: TransactionEntity) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transactionId: String) {
        transactionDao.deleteTransaction(transactionId)
    }
}
