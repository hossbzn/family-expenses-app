package com.familyexpenses.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.UserDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.UserEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DatabaseSeedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userDao: UserDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (accountDao.countAccounts() > 0) {
            return Result.success()
        }

        val now = System.currentTimeMillis()
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
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "database_seed"
        const val DEFAULT_USER_ID = "user-primary"
        const val PERSONAL_ACCOUNT_ID = "account-personal"
        const val FAMILY_ACCOUNT_ID = "account-family"
    }
}
