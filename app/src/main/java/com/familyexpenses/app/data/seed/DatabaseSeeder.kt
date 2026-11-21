package com.familyexpenses.app.data.seed

import androidx.room.withTransaction
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.AppDatabase
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: AppDatabase,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
) {

    suspend fun seedDefaultsIfNeeded() {
        database.withTransaction {
            val now = System.currentTimeMillis()

            if (accountDao.countAccounts() == 0) {
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
                            name = "Familia",
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
                        // 7 Categorías favoritas de gasto (V6: exactamente 7 favoritas)
                        CategoryEntity(
                            id = "cat-groceries",
                            name = "Supermercado",
                            icon = "shopping_cart",
                            type = CategoryType.EXPENSE,
                            orderIndex = 1,
                            isFavorite = true,
                            favoritePosition = 1,
                            createdAt = now,
                        ),
                        CategoryEntity(
                            id = "cat-home",
                            name = "Hogar",
                            icon = "home",
                            type = CategoryType.EXPENSE,
                            orderIndex = 2,
                            isFavorite = true,
                            favoritePosition = 2,
                            createdAt = now,
                        ),
                        CategoryEntity(
                            id = "cat-transport",
                            name = "Transporte",
                            icon = "directions_car",
                            type = CategoryType.EXPENSE,
                            orderIndex = 3,
                            isFavorite = true,
                            favoritePosition = 3,
                            createdAt = now,
                        ),
                        CategoryEntity(
                            id = "cat-restaurants",
                            name = "Restaurantes",
                            icon = "restaurant",
                            type = CategoryType.EXPENSE,
                            orderIndex = 4,
                            isFavorite = true,
                            favoritePosition = 4,
                            createdAt = now,
                        ),
                        CategoryEntity(
                            id = "cat-health",
                            name = "Salud",
                            icon = "medical_services",
                            type = CategoryType.EXPENSE,
                            orderIndex = 5,
                            isFavorite = true,
                            favoritePosition = 5,
                            createdAt = now,
                        ),
                        CategoryEntity(
                            id = "cat-leisure",
                            name = "Ocio",
                            icon = "sports_esports",
                            type = CategoryType.EXPENSE,
                            orderIndex = 6,
                            isFavorite = true,
                            favoritePosition = 6,
                            createdAt = now,
                        ),
                        CategoryEntity(
                            id = "cat-bills",
                            name = "Suministros",
                            icon = "receipt_long",
                            type = CategoryType.EXPENSE,
                            orderIndex = 7,
                            isFavorite = true,
                            favoritePosition = 7,
                            createdAt = now,
                        ),
                        // Categorías de gasto adicionales (no favoritas)
                        CategoryEntity(
                            id = "cat-clothing",
                            name = "Ropa",
                            icon = "checkroom",
                            type = CategoryType.EXPENSE,
                            orderIndex = 8,
                            isFavorite = false,
                            favoritePosition = null,
                            createdAt = now,
                        ),
                        CategoryEntity(
                            id = "cat-education",
                            name = "Educación",
                            icon = "school",
                            type = CategoryType.EXPENSE,
                            orderIndex = 9,
                            isFavorite = false,
                            favoritePosition = null,
                            createdAt = now,
                        ),
                        // Categorías de ingreso (orden manual, sin favoritas)
                        CategoryEntity(
                            id = "cat-salary",
                            name = "Salario",
                            icon = "payments",
                            type = CategoryType.INCOME,
                            orderIndex = 1,
                            isFavorite = false,
                            favoritePosition = null,
                            createdAt = now,
                        ),
                        CategoryEntity(
                            id = "cat-other-income",
                            name = "Otros ingresos",
                            icon = "savings",
                            type = CategoryType.INCOME,
                            orderIndex = 2,
                            isFavorite = false,
                            favoritePosition = null,
                            createdAt = now,
                        ),
                    ),
                )
            }
        }
    }

    companion object {
        const val PERSONAL_ACCOUNT_ID = "account-personal"
        const val FAMILY_ACCOUNT_ID = "account-family"
    }
}
