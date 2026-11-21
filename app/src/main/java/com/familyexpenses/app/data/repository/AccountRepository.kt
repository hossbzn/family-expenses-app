package com.familyexpenses.app.data.repository

import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class AccountRepository @Inject constructor(
    private val accountDao: AccountDao,
) {
    fun observeAccounts(): Flow<List<AccountEntity>> = accountDao.observeAccounts()

    suspend fun getAllAccounts(): List<AccountEntity> = accountDao.getAllAccounts()

    suspend fun getAccountById(accountId: String): AccountEntity? = accountDao.getAccountById(accountId)

    suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {
        accountDao.updateInitialBalance(accountId, initialBalanceMinor)
    }

    suspend fun updateAccountName(accountId: String, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "El nombre de la cuenta no puede estar vacío." }
        val existing = accountDao.getAccountById(accountId) ?: return
        accountDao.upsert(existing.copy(name = trimmed))
    }
}
