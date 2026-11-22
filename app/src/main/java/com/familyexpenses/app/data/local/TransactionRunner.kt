package com.familyexpenses.app.data.local

import androidx.room.withTransaction
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionRunner {
    suspend operator fun <R> invoke(block: suspend () -> R): R
}

@Singleton
class RoomTransactionRunner @Inject constructor(
    private val database: AppDatabase,
) : TransactionRunner {
    override suspend operator fun <R> invoke(block: suspend () -> R): R {
        return database.withTransaction(block)
    }
}
