package com.familyexpenses.app.feature.history

import androidx.lifecycle.SavedStateHandle
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.TransferDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.HistoryRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.domain.usecase.FilterMovementsUseCase
import com.familyexpenses.app.domain.usecase.GetHistoryUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

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

    private val accountsFlow = MutableStateFlow(listOf(personalAccount, familyAccount))
    private val categoriesFlow = MutableStateFlow(listOf(foodCategory))
    private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private val transfersFlow = MutableStateFlow<List<TransferEntity>>(emptyList())

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

    private val fakeTransferDao = object : TransferDao {
        override suspend fun insertTransfer(transfer: TransferEntity) {}
        override fun observeTransfers(): Flow<List<TransferEntity>> = transfersFlow
        override suspend fun getAllTransfers(): List<TransferEntity> = transfersFlow.value
        override suspend fun getTransferById(transferId: String): TransferEntity? = null
        override suspend fun deleteTransfer(transferId: String) {}
    }

    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val now = System.currentTimeMillis()
        transactionsFlow.value = listOf(
            TransactionEntity(
                id = "tx-adv",
                type = TransactionType.EXPENSE,
                amountMinor = 8000L,
                payingAccountId = "account-personal",
                responsibleAccountId = "account-family",
                categoryId = "cat-food",
                occurredAt = now,
                note = "Compra del mes",
                source = TransactionSource.MANUAL,
                createdAt = now,
            ),
        )
        transfersFlow.value = listOf(
            TransferEntity(
                id = "tr-1",
                type = TransferType.REIMBURSEMENT,
                sourceAccountId = "account-family",
                destinationAccountId = "account-personal",
                amountMinor = 3000L,
                occurredAt = now - 86_400_000L,
                note = null,
                createdAt = now - 86_400_000L,
            ),
        )

        val historyRepository = HistoryRepository(
            transactionDao = fakeTransactionDao,
            transferDao = fakeTransferDao,
            accountDao = fakeAccountDao,
            categoryDao = fakeCategoryDao,
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

        viewModel = HistoryViewModel(
            savedStateHandle = SavedStateHandle(),
            getHistoryUseCase = GetHistoryUseCase(historyRepository),
            accountRepository = AccountRepository(fakeAccountDao),
            categoryRepository = CategoryRepository(fakeCategoryDao),
            userPreferencesRepository = fakePreferences,
            filterMovementsUseCase = FilterMovementsUseCase(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `groups movements by day descending with two-line descriptions`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.totalCount)
        assertEquals(2, state.dayGroups.size)

        val todayRow = state.dayGroups[0].rows.first()
        assertEquals("Compra del mes", todayRow.line1Title)
        assertTrue(todayRow.line2Subtitle.contains("Personal → Familia (Adelanto)"))

        val yesterdayRow = state.dayGroups[1].rows.first()
        assertEquals("Devolución de adelanto", yesterdayRow.line1Title)
        assertEquals("Familia → Personal (Devolución)", yesterdayRow.line2Subtitle)
    }

    @Test
    fun `filters by responsible account and type properly`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Filtrar gastos solo en Personal -> tx-adv tiene responsable Familia, por lo que no aparece bajo Personal
        viewModel.onAccountFilterSelected(MovementAccountFilter.Account("account-personal"))
        viewModel.onTypeFilterSelected(MovementTypeFilter.EXPENSES)
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.totalCount)

        // Filtrar gastos bajo Familia -> aparece tx-adv
        viewModel.onAccountFilterSelected(MovementAccountFilter.Account("account-family"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.totalCount)
        assertEquals("tx-adv", viewModel.uiState.value.dayGroups.first().rows.first().id)

        // Reset limpia filtros
        viewModel.onResetFilters()
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.totalCount)
    }

    @Test
    fun `initializes period and category filters from navigation arguments`() = runTest {
        val historyRepository = HistoryRepository(
            transactionDao = fakeTransactionDao,
            transferDao = fakeTransferDao,
            accountDao = fakeAccountDao,
            categoryDao = fakeCategoryDao,
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
        val drillDownVm = HistoryViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    HistoryViewModel.INITIAL_ACCOUNT_ID_ARG to "account-family",
                    HistoryViewModel.INITIAL_CATEGORY_ID_ARG to "cat-food",
                    HistoryViewModel.INITIAL_TYPE_FILTER_ARG to MovementTypeFilter.EXPENSES.name,
                    HistoryViewModel.INITIAL_PERIOD_TYPE_ARG to "CUSTOM",
                    HistoryViewModel.INITIAL_PERIOD_START_ARG to "2026-01-01",
                    HistoryViewModel.INITIAL_PERIOD_END_ARG to "2026-01-31",
                ),
            ),
            getHistoryUseCase = GetHistoryUseCase(historyRepository),
            accountRepository = AccountRepository(fakeAccountDao),
            categoryRepository = CategoryRepository(fakeCategoryDao),
            userPreferencesRepository = fakePreferences,
            filterMovementsUseCase = FilterMovementsUseCase(),
        )
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { drillDownVm.uiState.collect() }
        advanceUntilIdle()

        val state = drillDownVm.uiState.value
        assertEquals(MovementAccountFilter.Account("account-family"), state.criteria.accountFilter)
        assertEquals("cat-food", state.criteria.categoryId)
        assertEquals(MovementTypeFilter.EXPENSES, state.criteria.typeFilter)
        assertEquals(
            com.familyexpenses.app.domain.usecase.MovementPeriodFilter.Custom(
                java.time.LocalDate.of(2026, 1, 1),
                java.time.LocalDate.of(2026, 1, 31),
            ),
            state.criteria.periodFilter,
        )
    }
}
