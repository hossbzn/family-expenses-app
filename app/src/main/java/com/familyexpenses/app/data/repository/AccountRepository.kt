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

    suspend fun getAccountById(accountId: String): AccountEntity? = accountDao.getAccountById(accountId)
}
