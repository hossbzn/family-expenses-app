package com.familyexpenses.app.feature.analysis

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionSource
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
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.domain.usecase.AnalysisSeriesType
import com.familyexpenses.app.domain.usecase.ComputeAnalysisUseCase
import com.familyexpenses.app.domain.usecase.FilterMovementsUseCase
import com.familyexpenses.app.domain.usecase.MovementAccountFilter
import com.familyexpenses.app.domain.usecase.MovementTypeFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val personalAccount = AccountEntity(
        id = "account-personal",
        name = "Personal",
        type = AccountType.PERSONAL,
        currencyCode = "EUR",
        initialBalanceMinor = 100000L,
        createdAt = 0L,
    )

    private val familyAccount = AccountEntity(
        id = "account-family",
        name = "Familia",
        type = AccountType.FAMILY,
        currencyCode = "EUR",
        initialBalanceMinor = 200000L,
        createdAt = 0L,
    )

    private val foodCategory = CategoryEntity(
        id = "cat-food",
        name = "Supermercado",
        icon = "shopping_cart",
        type = CategoryType.EXPENSE,
        orderIndex = 1,
        isFavorite = true,
        favoritePosition = 1,
        createdAt = 0L,
    )

    private val salaryCategory = CategoryEntity(
        id = "cat-salary",
        name = "Nómina",
        icon = "payments",
        type = CategoryType.INCOME,
        orderIndex = 1,
        isFavorite = false,
        createdAt = 0L,
    )

    private val accountsFlow = MutableStateFlow(listOf(personalAccount, familyAccount))
    private val categoriesFlow = MutableStateFlow(listOf(foodCategory, salaryCategory))
    private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())

    private val fakeAccountDao = object : AccountDao {
        override suspend fun upsert(account: AccountEntity) {}
        override suspend fun upsertAll(accounts: List<AccountEntity>) {}
        override fun observeAccounts(): Flow<List<AccountEntity>> = accountsFlow
        override suspend fun getAllAccounts(): List<AccountEntity> = accountsFlow.value
        override suspend fun getAccountById(accountId: String): AccountEntity? =
            accountsFlow.value.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accountsFlow.value.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {}
    }

    private val fakeCategoryDao = object : CategoryDao {
        override suspend fun upsert(category: CategoryEntity) {}
        override suspend fun upsertAll(categories: List<CategoryEntity>) {}
        override fun observeCategories(): Flow<List<CategoryEntity>> = categoriesFlow
        override fun observeActiveCategories(): Flow<List<CategoryEntity>> = categoriesFlow
        override fun observeActiveCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> = categoriesFlow
        override fun observeFavoriteExpenseCategories(): Flow<List<CategoryEntity>> = categoriesFlow
        override suspend fun getCategoryById(categoryId: String): CategoryEntity? =
            categoriesFlow.value.find { it.id == categoryId }
        override suspend fun countCategories(): Int = categoriesFlow.value.size
    }

    private val fakeTransactionDao = object : TransactionDao {
        override suspend fun insertTransaction(transaction: TransactionEntity) {}
        override fun observeTransactions(): Flow<List<TransactionEntity>> = transactionsFlow
        override suspend fun getAllTransactions(): List<TransactionEntity> = transactionsFlow.value
        override suspend fun getTransactionById(transactionId: String): TransactionEntity? = null
        override suspend fun deleteTransaction(transactionId: String) {}
    }

    private lateinit var viewModel: AnalysisViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val now = System.currentTimeMillis()
        transactionsFlow.value = listOf(
            TransactionEntity(
                id = "tx-exp",
                type = TransactionType.EXPENSE,
                amountMinor = 4500L,
                payingAccountId = "account-personal",
                responsibleAccountId = "account-family",
                categoryId = "cat-food",
                occurredAt = now,
                source = TransactionSource.MANUAL,
                createdAt = now,
            ),
            TransactionEntity(
                id = "tx-inc",
                type = TransactionType.INCOME,
                amountMinor = 120000L,
                payingAccountId = "account-personal",
                responsibleAccountId = "account-personal",
                categoryId = "cat-salary",
                occurredAt = now,
                source = TransactionSource.MANUAL,
                createdAt = now,
            ),
        )

        val fakePreferences = object : UserPreferencesRepository {
            var privacyMode = false
            var notifications = true
            override fun observePrivacyModeEnabled(): Flow<Boolean> = MutableStateFlow(privacyMode)
            override fun setPrivacyModeEnabled(enabled: Boolean) { privacyMode = enabled }
            override fun observeNotificationsEnabled(): Flow<Boolean> = MutableStateFlow(notifications)
            override fun setNotificationsEnabled(enabled: Boolean) { notifications = enabled }
            override fun getLastNotifiedPendingFingerprint(): String = ""
            override fun setLastNotifiedPendingFingerprint(fingerprint: String) {}
            override fun getLastExpensePayingAccountId(): String? = null
            override fun setLastExpensePayingAccountId(accountId: String) {}
            override fun getLastExpenseResponsibleAccountId(): String? = null
            override fun setLastExpenseResponsibleAccountId(accountId: String) {}
            override fun getLastExpenseCategoryId(): String? = null
            override fun setLastExpenseCategoryId(categoryId: String) {}
            override fun getLastIncomeAccountId(): String? = null
            override fun setLastIncomeAccountId(accountId: String) {}
            override fun getLastIncomeCategoryId(): String? = null
            override fun setLastIncomeCategoryId(categoryId: String) {}
            override fun getAccountIcon(accountId: String): String? = null
            override fun setAccountIcon(accountId: String, icon: String) {}
        }

        viewModel = AnalysisViewModel(
            accountRepository = AccountRepository(fakeAccountDao),
            expenseRepository = ExpenseRepository(fakeTransactionDao),
            categoryRepository = CategoryRepository(fakeCategoryDao),
            userPreferencesRepository = fakePreferences,
            computeAnalysisUseCase = ComputeAnalysisUseCase(FilterMovementsUseCase()),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `defaults to categories tab and expenses series then switches series and builds drilldown`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        var state = viewModel.uiState.value
        assertEquals(AnalysisTab.CATEGORIES, state.selectedTab)
        assertEquals(AnalysisSeriesType.EXPENSES, state.seriesType)
        assertEquals(4500L, state.totalMinor)
        assertEquals(1, state.categorySlices.size)

        // Cambiar a Familia + Evolución + Ingresos -> 0 ingresos en Familia
        viewModel.onAccountFilterSelected(MovementAccountFilter.Account("account-family"))
        viewModel.onTabSelected(AnalysisTab.EVOLUTION)
        viewModel.onSeriesTypeSelected(AnalysisSeriesType.INCOMES)
        advanceUntilIdle()

        state = viewModel.uiState.value
        assertEquals(AnalysisTab.EVOLUTION, state.selectedTab)
        assertEquals(AnalysisSeriesType.INCOMES, state.seriesType)
        assertEquals(0L, state.totalMinor)

        // Cambiar a Personal -> 120000L ingresos en Personal
        viewModel.onAccountFilterSelected(MovementAccountFilter.Account("account-personal"))
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(120000L, state.totalMinor)

        // Verificar parámetros de drill-down para categoría
        val catReq = viewModel.buildCategoryDrillDown("cat-salary")
        assertEquals("account-personal", catReq.accountId)
        assertEquals(MovementTypeFilter.INCOMES.name, catReq.typeFilterName)
        assertEquals("cat-salary", catReq.categoryId)
        assertEquals("MONTH", catReq.periodTypeName)

        // Verificar parámetros de drill-down para barra anual (mes completo -> MONTH)
        viewModel.onSelectYearPeriod()
        advanceUntilIdle()
        val janBucket = viewModel.uiState.value.evolutionBuckets.first()
        val bucketReq = viewModel.buildBucketDrillDown(janBucket)
        assertEquals("MONTH", bucketReq.periodTypeName)
        assertEquals(LocalDate.of(LocalDate.now().year, 1, 1).toString(), bucketReq.periodStartIso)
        assertEquals("", bucketReq.categoryId)
    }
}
