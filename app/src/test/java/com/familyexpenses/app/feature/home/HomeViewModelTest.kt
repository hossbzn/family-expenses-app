package com.familyexpenses.app.feature.home

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.BalanceAdjustmentDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.TransferDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.data.repository.DashboardRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.HistoryRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.domain.usecase.ComputeAccountBalanceUseCase
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
import com.familyexpenses.app.domain.usecase.GetDashboardSummaryUseCase
import com.familyexpenses.app.domain.usecase.GetHistoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val personalAccount = AccountEntity(
        id = "acc-personal",
        name = "Personal",
        type = AccountType.PERSONAL,
        currencyCode = "EUR",
        initialBalanceMinor = 150000L,
        createdAt = 0L,
    )

    private val familyAccount = AccountEntity(
        id = "acc-family",
        name = "Familia",
        type = AccountType.FAMILY,
        currencyCode = "EUR",
        initialBalanceMinor = 300000L,
        createdAt = 0L,
    )

    private val accounts = mutableListOf(personalAccount, familyAccount)
    private val transactions = mutableListOf<TransactionEntity>()
    private val transfers = mutableListOf<TransferEntity>()
    private val adjustments = mutableListOf<BalanceAdjustmentEntity>()

    private val fakeAccountDao = object : AccountDao {
        override suspend fun upsert(account: AccountEntity) { accounts.add(account) }
        override suspend fun upsertAll(accounts: List<AccountEntity>) { this@HomeViewModelTest.accounts.addAll(accounts) }
        override fun observeAccounts(): Flow<List<AccountEntity>> = flowOf(accounts)
        override suspend fun getAllAccounts(): List<AccountEntity> = accounts
        override suspend fun getAccountById(accountId: String): AccountEntity? = accounts.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accounts.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {}
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

    private val fakeTransferDao = object : TransferDao {
        override suspend fun insertTransfer(transfer: TransferEntity) { transfers.add(transfer) }
        override fun observeTransfers(): Flow<List<TransferEntity>> = flowOf(transfers)
        override suspend fun getAllTransfers(): List<TransferEntity> = transfers
        override suspend fun getTransferById(transferId: String): TransferEntity? = transfers.find { it.id == transferId }
        override suspend fun deleteTransfer(transferId: String) { transfers.removeAll { it.id == transferId } }
    }

    private val fakeAdjustmentDao = object : BalanceAdjustmentDao {
        override suspend fun insertAdjustment(adjustment: BalanceAdjustmentEntity) { adjustments.add(adjustment) }
        override fun observeAdjustments(): Flow<List<BalanceAdjustmentEntity>> = flowOf(adjustments)
        override fun observeAdjustmentsForAccount(accountId: String): Flow<List<BalanceAdjustmentEntity>> =
            flowOf(adjustments.filter { it.accountId == accountId })
        override suspend fun getAllAdjustments(): List<BalanceAdjustmentEntity> = adjustments
    }

    private val fakeCategoryDao = object : CategoryDao {
        override suspend fun upsert(category: CategoryEntity) {}
        override suspend fun upsertAll(categories: List<CategoryEntity>) {}
        override fun observeCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeActiveCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeActiveCategoriesByType(type: com.familyexpenses.app.core.model.CategoryType): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override fun observeFavoriteExpenseCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
        override suspend fun getCategoryById(categoryId: String): CategoryEntity? = null
        override suspend fun countCategories(): Int = 0
    }

    private lateinit var viewModel: HomeViewModel
    private lateinit var expenseRepository: ExpenseRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactions.clear()
        transfers.clear()
        adjustments.clear()

        val computeAccountBalanceUseCase = ComputeAccountBalanceUseCase()
        val computeNetDebtUseCase = ComputeNetDebtUseCase()

        val dashboardRepository = DashboardRepository(
            accountDao = fakeAccountDao,
            transactionDao = fakeTransactionDao,
            transferDao = fakeTransferDao,
            balanceAdjustmentDao = fakeAdjustmentDao,
            computeAccountBalanceUseCase = computeAccountBalanceUseCase,
            computeNetDebtUseCase = computeNetDebtUseCase,
        )

        expenseRepository = ExpenseRepository(fakeTransactionDao)
        val getDashboardSummaryUseCase = GetDashboardSummaryUseCase(dashboardRepository)

        val fakeOccurrenceDao = object : com.familyexpenses.app.data.local.dao.RecurrenceOccurrenceDao {
            override suspend fun upsert(occurrence: com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity) {}
            override suspend fun upsertAll(occurrences: List<com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity>) {}
            override suspend fun getOccurrenceById(id: String): com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity? = null
            override fun observeOccurrencesByRule(ruleId: String): Flow<List<com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity>> = flowOf(emptyList())
            override fun observePendingOccurrences(): Flow<List<com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity>> = flowOf(emptyList())
            override suspend fun getPendingOccurrences(): List<com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity> = emptyList()
            override suspend fun getAllOccurrences(): List<com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity> = emptyList()
            override suspend fun updateStatus(id: String, status: com.familyexpenses.app.core.model.OccurrenceStatus, transactionId: String?) {}
            override suspend fun hasOccurrenceForDate(ruleId: String, scheduledAt: Long): Boolean = false
        }

        val historyRepository = HistoryRepository(
            transactionDao = fakeTransactionDao,
            transferDao = fakeTransferDao,
            accountDao = fakeAccountDao,
            categoryDao = fakeCategoryDao,
        )

        val fakePreferences = object : UserPreferencesRepository {
            var privacyMode = false
            private val privacyFlow = MutableStateFlow(false)
            override fun observePrivacyModeEnabled(): Flow<Boolean> = privacyFlow
            override fun setPrivacyModeEnabled(enabled: Boolean) {
                privacyMode = enabled
                privacyFlow.value = enabled
            }
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

        viewModel = HomeViewModel(
            getDashboardSummaryUseCase = getDashboardSummaryUseCase,
            getHistoryUseCase = GetHistoryUseCase(historyRepository),
            recurrenceOccurrenceDao = fakeOccurrenceDao,
            expenseRepository = expenseRepository,
            userPreferencesRepository = fakePreferences,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state reflects cash balances and zero debt`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(150000L, state.personalCashBalanceMinor)
        assertEquals(300000L, state.familyCashBalanceMinor)
        assertEquals(0L, state.netDebtMinor)
        assertFalse(state.privacyModeEnabled)
    }

    @Test
    fun `togglePrivacyMode toggles privacy mode flag`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.privacyModeEnabled)

        viewModel.togglePrivacyMode()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.privacyModeEnabled)
    }

    @Test
    fun `state reflects net debt when personal pays for family expense`() = runTest {
        transactions.add(
            TransactionEntity(
                id = "tx-1",
                payingAccountId = "acc-personal",
                responsibleAccountId = "acc-family",
                categoryId = "cat-food",
                amountMinor = 8000L,
                type = TransactionType.EXPENSE,
                source = TransactionSource.MANUAL,
                occurredAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis(),
            ),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Personal paga 80 €: caja Personal 1500 - 80 = 1420 € (142000 céntimos)
        assertEquals(142000L, state.personalCashBalanceMinor)
        // Caja Familia intacta: 3000 € (300000 céntimos)
        assertEquals(300000L, state.familyCashBalanceMinor)
        // Familia debe 80 € a Personal: +8000
        assertEquals(8000L, state.netDebtMinor)
    }

    @Test
    fun `undoTransaction removes transaction and updates balance`() = runTest {
        val tx = TransactionEntity(
            id = "tx-to-undo",
            payingAccountId = "acc-personal",
            responsibleAccountId = "acc-personal",
            categoryId = "cat-food",
            amountMinor = 2000L,
            type = TransactionType.EXPENSE,
            source = TransactionSource.MANUAL,
            occurredAt = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
        )
        transactions.add(tx)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(1, transactions.size)

        viewModel.undoTransaction("tx-to-undo")
        advanceUntilIdle()

        assertEquals(0, transactions.size)
    }
}
