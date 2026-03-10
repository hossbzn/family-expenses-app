package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyexpenses.app.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Upsert
    suspend fun upsertAll(accounts: List<AccountEntity>)

    @Query("SELECT * FROM accounts ORDER BY createdAt ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :accountId LIMIT 1")
    suspend fun getAccountById(accountId: String): AccountEntity?

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun countAccounts(): Int
}
