package com.familyexpenses.app.data.seed

import androidx.room.withTransaction
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.AppDatabase
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.UserDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.UserEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: AppDatabase,
    private val userDao: UserDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) {

    suspend fun seedDefaultsIfNeeded() {
        database.withTransaction {
            val now = System.currentTimeMillis()

            if (accountDao.countAccounts() == 0) {
                userDao.upsert(
                    UserEntity(
                        id = DEFAULT_USER_ID,
                        name = "Primary User",
                        createdAt = now,
                    ),
                )
                accountDao.upsertAll(
                    listOf(
                        AccountEntity(
                            id = PERSONAL_ACCOUNT_ID,
                            name = "Personal",
                            type = AccountType.PERSONAL,
                            currencyCode = "EUR",
                            initialBalanceMinor = 0L,
                            createdAt = now,
                        ),
                        AccountEntity(
                            id = FAMILY_ACCOUNT_ID,
                            name = "Familiar",
                            type = AccountType.FAMILY,
                            currencyCode = "EUR",
                            initialBalanceMinor = 0L,
                            createdAt = now,
                        ),
                    ),
                )
            }

            if (categoryDao.countCategories() == 0) {
                categoryDao.upsertAll(
                    listOf(
                        CategoryEntity(
                            id = "cat-groceries",
                            name = "Supermercado",
                            type = CategoryType.EXPENSE,
                            createdAt = now,
                        ),
                        CategoryEntity(
                            id = "cat-transport",
                            name = "Transporte",
                            type = CategoryType.EXPENSE,
                            createdAt = now,
                        ),
                        CategoryEntity(
                            id = "cat-salary",
                            name = "Salario",
                            type = CategoryType.INCOME,
                            createdAt = now,
                        ),
                    ),
                )
            }
        }
    }

    companion object {
        const val DEFAULT_USER_ID = "user-primary"
        const val PERSONAL_ACCOUNT_ID = "account-personal"
        const val FAMILY_ACCOUNT_ID = "account-family"
    }
}
