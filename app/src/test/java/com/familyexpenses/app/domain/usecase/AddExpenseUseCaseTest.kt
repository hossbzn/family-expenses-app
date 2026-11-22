package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.domain.model.AddExpenseRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddExpenseUseCaseTest {

    private val personalAccount = AccountEntity(
        id = "acc-personal",
        name = "Personal",
        type = AccountType.PERSONAL,
        currencyCode = "EUR",
        initialBalanceMinor = 100000L,
        createdAt = 0L,
    )

    private val familyAccount = AccountEntity(
        id = "acc-family",
        name = "Familia",
        type = AccountType.FAMILY,
        currencyCode = "EUR",
        initialBalanceMinor = 100000L,
        createdAt = 0L,
    )

    private val groceryCategory = CategoryEntity(
        id = "cat-groceries",
        name = "Supermercado",
        type = CategoryType.EXPENSE,
        createdAt = 0L,
    )

    private val accounts = mutableListOf(personalAccount, familyAccount)
    private val categories = mutableListOf(groceryCategory)
    private val transactions = mutableListOf<TransactionEntity>()

    private val fakeAccountDao = object : AccountDao {
        override suspend fun upsert(account: AccountEntity) { accounts.add(account) }
        override suspend fun upsertAll(accounts: List<AccountEntity>) { this@AddExpenseUseCaseTest.accounts.addAll(accounts) }
        override fun observeAccounts(): Flow<List<AccountEntity>> = flowOf(accounts)
        override suspend fun getAllAccounts(): List<AccountEntity> = accounts
        override suspend fun getAccountById(accountId: String): AccountEntity? = accounts.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accounts.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {}
    }

    private val fakeCategoryDao = object : CategoryDao {
        override suspend fun upsert(category: CategoryEntity) { categories.add(category) }
        override suspend fun upsertAll(categories: List<CategoryEntity>) { this@AddExpenseUseCaseTest.categories.addAll(categories) }
        override fun observeCategories(): Flow<List<CategoryEntity>> = flowOf(categories)
        override fun observeActiveCategories(): Flow<List<CategoryEntity>> = flowOf(categories.filter { !it.isArchived })
        override fun observeActiveCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> =
            flowOf(categories.filter { !it.isArchived && it.type == type })
        override fun observeFavoriteExpenseCategories(): Flow<List<CategoryEntity>> =
            flowOf(categories.filter { !it.isArchived && it.isFavorite })
        override suspend fun getCategoryById(categoryId: String): CategoryEntity? =
            categories.find { it.id == categoryId }
        override suspend fun countCategories(): Int = categories.size
    }

    private val fakeTransactionDao = object : TransactionDao {
        override suspend fun insertTransaction(transaction: TransactionEntity) { transactions.add(transaction) }
        override fun observeTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override suspend fun getAllTransactions(): List<TransactionEntity> = transactions
        override suspend fun getTransactionById(transactionId: String): TransactionEntity? =
            transactions.find { it.id == transactionId }
        override suspend fun deleteTransaction(transactionId: String) {
            transactions.removeAll { it.id == transactionId }
        }
    }

    private val accountRepository = AccountRepository(fakeAccountDao)
    private val categoryRepository = CategoryRepository(fakeCategoryDao)
    private val expenseRepository = ExpenseRepository(fakeTransactionDao)

    private lateinit var addExpenseUseCase: AddExpenseUseCase

    @Before
    fun setUp() {
        transactions.clear()
        addExpenseUseCase = AddExpenseUseCase(
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            expenseRepository = expenseRepository,
        )
    }

    @Test
    fun `adding expense with same paying and responsible account is not an advance`() {
        runBlocking {
            val result = addExpenseUseCase(
                AddExpenseRequest(
                    type = TransactionType.EXPENSE,
                    amountMinor = 2500L,
                    payingAccountId = personalAccount.id,
                    responsibleAccountId = personalAccount.id,
                    categoryId = groceryCategory.id,
                    note = "Comida personal",
                    occurredAt = 1000L,
                ),
            )

            assertNotNull(result.transactionId)
            assertFalse(result.isAdvance)
            assertEquals(1, transactions.size)
            val tx = transactions.first()
            assertEquals(personalAccount.id, tx.payingAccountId)
            assertEquals(personalAccount.id, tx.responsibleAccountId)
            assertEquals(2500L, tx.amountMinor)
        }
    }

    @Test
    fun `adding expense with different paying and responsible accounts marks advance true`() {
        runBlocking {
            val result = addExpenseUseCase(
                AddExpenseRequest(
                    type = TransactionType.EXPENSE,
                    amountMinor = 8000L,
                    payingAccountId = personalAccount.id,
                    responsibleAccountId = familyAccount.id,
                    categoryId = groceryCategory.id,
                    note = "Compra familiar",
                    occurredAt = 1000L,
                ),
            )

            assertNotNull(result.transactionId)
            assertTrue(result.isAdvance)
            assertEquals(1, transactions.size)
            val tx = transactions.first()
            assertEquals(personalAccount.id, tx.payingAccountId)
            assertEquals(familyAccount.id, tx.responsibleAccountId)
        }
    }

    @Test
    fun `adding expense rejects non-positive amount or missing category`() {
        runBlocking {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    addExpenseUseCase(
                        AddExpenseRequest(
                            type = TransactionType.EXPENSE,
                            amountMinor = 0L,
                            payingAccountId = personalAccount.id,
                            responsibleAccountId = familyAccount.id,
                            categoryId = groceryCategory.id,
                            note = null,
                            occurredAt = 1000L,
                        ),
                    )
                }
            }

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    addExpenseUseCase(
                        AddExpenseRequest(
                            type = TransactionType.EXPENSE,
                            amountMinor = 1000L,
                            payingAccountId = personalAccount.id,
                            responsibleAccountId = familyAccount.id,
                            categoryId = "",
                            note = null,
                            occurredAt = 1000L,
                        ),
                    )
                }
            }
        }
    }
}
