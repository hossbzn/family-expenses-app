package com.familyexpenses.app.feature.income

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
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.domain.usecase.AddIncomeUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddIncomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

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

    private val salaryCategory = CategoryEntity(
        id = "cat-salary",
        name = "Salario",
        type = CategoryType.INCOME,
        orderIndex = 1,
        isFavorite = false,
        favoritePosition = null,
        createdAt = 0L,
    )

    private val otherIncomeCategory = CategoryEntity(
        id = "cat-other-income",
        name = "Otros ingresos",
        type = CategoryType.INCOME,
        orderIndex = 2,
        isFavorite = false,
        favoritePosition = null,
        createdAt = 0L,
    )

    private val accounts = mutableListOf(personalAccount, familyAccount)
    private val categories = mutableListOf(salaryCategory, otherIncomeCategory)
    private val transactions = mutableListOf<TransactionEntity>()

    private val fakeAccountDao = object : AccountDao {
        override suspend fun upsert(account: AccountEntity) { accounts.add(account) }
        override suspend fun upsertAll(accounts: List<AccountEntity>) { this@AddIncomeViewModelTest.accounts.addAll(accounts) }
        override fun observeAccounts(): Flow<List<AccountEntity>> = flowOf(accounts)
        override suspend fun getAllAccounts(): List<AccountEntity> = accounts
        override suspend fun getAccountById(accountId: String): AccountEntity? = accounts.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accounts.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {}
    }

    private val fakeCategoryDao = object : CategoryDao {
        override suspend fun upsert(category: CategoryEntity) { categories.add(category) }
        override suspend fun upsertAll(categories: List<CategoryEntity>) { this@AddIncomeViewModelTest.categories.addAll(categories) }
        override fun observeCategories(): Flow<List<CategoryEntity>> = flowOf(categories)
        override fun observeActiveCategories(): Flow<List<CategoryEntity>> = flowOf(categories.filter { !it.isArchived })
        override fun observeActiveCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> =
            flowOf(categories.filter { !it.isArchived && it.type == type })
        override fun observeFavoriteExpenseCategories(): Flow<List<CategoryEntity>> = flowOf(emptyList())
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

    private val fakePreferences = object : UserPreferencesRepository {
        var privacyMode = false
        private var notificationsFlag = true
        var lastIncomeAccount: String? = null
        var lastIncomeCategory: String? = null

        override fun observePrivacyModeEnabled(): Flow<Boolean> = MutableStateFlow(privacyMode)
        override fun setPrivacyModeEnabled(enabled: Boolean) { privacyMode = enabled }
        override fun observeNotificationsEnabled(): Flow<Boolean> = MutableStateFlow(notificationsFlag)
        override fun setNotificationsEnabled(enabled: Boolean) { notificationsFlag = enabled }
        override fun getLastNotifiedPendingCount(): Int = 0
        override fun setLastNotifiedPendingCount(count: Int) {}
        override fun getLastExpensePayingAccountId(): String? = null
        override fun setLastExpensePayingAccountId(accountId: String) {}
        override fun getLastExpenseResponsibleAccountId(): String? = null
        override fun setLastExpenseResponsibleAccountId(accountId: String) {}
        override fun getLastExpenseCategoryId(): String? = null
        override fun setLastExpenseCategoryId(categoryId: String) {}
        override fun getLastIncomeAccountId(): String? = lastIncomeAccount
        override fun setLastIncomeAccountId(accountId: String) { lastIncomeAccount = accountId }
        override fun getLastIncomeCategoryId(): String? = lastIncomeCategory
        override fun setLastIncomeCategoryId(categoryId: String) { lastIncomeCategory = categoryId }
        override fun getAccountIcon(accountId: String): String? = null
        override fun setAccountIcon(accountId: String, icon: String) {}
    }

    private lateinit var viewModel: AddIncomeViewModel
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var addIncomeUseCase: AddIncomeUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactions.clear()

        accountRepository = AccountRepository(fakeAccountDao)
        categoryRepository = CategoryRepository(fakeCategoryDao)
        expenseRepository = ExpenseRepository(fakeTransactionDao)
        addIncomeUseCase = AddIncomeUseCase(accountRepository, categoryRepository, expenseRepository)

        viewModel = AddIncomeViewModel(
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            preferencesRepository = fakePreferences,
            addIncomeUseCase = addIncomeUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has zero amount, save disabled, and accounts and categories loaded`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("0", state.amountInput)
        assertEquals(0L, state.amountMinor)
        assertEquals("0 €", state.displayAmount)
        assertEquals(2, state.accounts.size)
        assertEquals(2, state.incomeCategories.size)
        assertEquals("cat-salary", state.selectedCategoryId)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun `keypad digits update amount and display correctly`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onDigit('1')
        viewModel.onDigit('2')
        viewModel.onComma()
        viewModel.onDigit('5')
        viewModel.onDigit('0')
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("12,50", state.amountInput)
        assertEquals(1250L, state.amountMinor)
        assertEquals("12,50 €", state.displayAmount)
        assertTrue(state.isSaveEnabled)

        viewModel.onBackspace()
        advanceUntilIdle()
        assertEquals("12,5", viewModel.uiState.value.amountInput)
        assertEquals(1250L, viewModel.uiState.value.amountMinor)

        viewModel.onClear()
        advanceUntilIdle()
        assertEquals("0", viewModel.uiState.value.amountInput)
        assertEquals(0L, viewModel.uiState.value.amountMinor)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun `selecting destination account and category updates state`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onDestinationAccountSelected("acc-family")
        viewModel.onCategorySelected("cat-other-income")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("acc-family", state.destinationAccountId)
        assertEquals("cat-other-income", state.selectedCategoryId)
    }

    @Test
    fun `save income persists transaction and emits Saved event with preferences updated`() = runTest {
        val events = mutableListOf<AddIncomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvents.collect { events.add(it) } }
        advanceUntilIdle()

        viewModel.onDigit('5')
        viewModel.onDigit('0')
        viewModel.onDestinationAccountSelected("acc-family")
        viewModel.onCategorySelected("cat-salary")
        viewModel.onNoteChanged("Nómina mensual")
        advanceUntilIdle()

        viewModel.saveIncome()
        advanceUntilIdle()

        assertEquals(1, transactions.size)
        val tx = transactions.first()
        assertEquals(5000L, tx.amountMinor)
        assertEquals(TransactionType.INCOME, tx.type)
        assertEquals("acc-family", tx.payingAccountId)
        assertEquals("acc-family", tx.responsibleAccountId)
        assertEquals("cat-salary", tx.categoryId)
        assertEquals("Nómina mensual", tx.note)

        assertEquals(1, events.size)
        assertTrue(events.first() is AddIncomeEvent.Saved)

        assertEquals("acc-family", fakePreferences.lastIncomeAccount)
        assertEquals("cat-salary", fakePreferences.lastIncomeCategory)
    }

    @Test
    fun `save income does not execute if amount is zero`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.saveIncome()
        advanceUntilIdle()

        assertTrue(transactions.isEmpty())
    }

    @Test
    fun `back clicked prompts discard dialog if amount is entered`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onDigit('1')
        advanceUntilIdle()
        viewModel.onBackClicked()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showDiscardDialog)

        viewModel.onDismissDiscardDialog()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `back clicked navigates back directly if form is clean`() = runTest {
        val events = mutableListOf<AddIncomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvents.collect { events.add(it) } }
        advanceUntilIdle()

        viewModel.onBackClicked()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDiscardDialog)
        assertEquals(1, events.size)
        assertEquals(AddIncomeEvent.NavigateBack, events.first())
    }

    @Test
    fun `confirm discard emits NavigateBack event`() = runTest {
        val events = mutableListOf<AddIncomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvents.collect { events.add(it) } }
        advanceUntilIdle()

        viewModel.onDigit('9')
        advanceUntilIdle()
        viewModel.onBackClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDiscardDialog)

        viewModel.onConfirmDiscard()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDiscardDialog)
        assertEquals(1, events.size)
        assertEquals(AddIncomeEvent.NavigateBack, events.first())
    }
}
