package com.familyexpenses.app.feature.expense

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
import com.familyexpenses.app.domain.usecase.AddExpenseUseCase
import com.familyexpenses.app.domain.usecase.ManageCategoriesUseCase
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
class AddExpenseViewModelTest {

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

    private val groceriesCategory = CategoryEntity(
        id = "cat-groceries",
        name = "Supermercado",
        type = CategoryType.EXPENSE,
        orderIndex = 1,
        isFavorite = true,
        favoritePosition = 1,
        createdAt = 0L,
    )

    private val homeCategory = CategoryEntity(
        id = "cat-home",
        name = "Hogar",
        type = CategoryType.EXPENSE,
        orderIndex = 2,
        isFavorite = true,
        favoritePosition = 2,
        createdAt = 0L,
    )

    private val accounts = mutableListOf(personalAccount, familyAccount)
    private val categories = mutableListOf(groceriesCategory, homeCategory)
    private val transactions = mutableListOf<TransactionEntity>()

    private val fakeAccountDao = object : AccountDao {
        override suspend fun upsert(account: AccountEntity) { accounts.add(account) }
        override suspend fun upsertAll(accounts: List<AccountEntity>) { this@AddExpenseViewModelTest.accounts.addAll(accounts) }
        override fun observeAccounts(): Flow<List<AccountEntity>> = flowOf(accounts)
        override suspend fun getAllAccounts(): List<AccountEntity> = accounts
        override suspend fun getAccountById(accountId: String): AccountEntity? = accounts.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accounts.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {}
    }

    private val fakeCategoryDao = object : CategoryDao {
        override suspend fun upsert(category: CategoryEntity) { categories.add(category) }
        override suspend fun upsertAll(categories: List<CategoryEntity>) { this@AddExpenseViewModelTest.categories.addAll(categories) }
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

    private val fakePreferences = object : UserPreferencesRepository {
        var privacyMode = false
        private var notificationsFlag = true
        var lastPaying: String? = null
        var lastResponsible: String? = null
        var lastCategory: String? = null

        override fun observePrivacyModeEnabled(): Flow<Boolean> = MutableStateFlow(privacyMode)
        override fun setPrivacyModeEnabled(enabled: Boolean) { privacyMode = enabled }
        override fun observeNotificationsEnabled(): Flow<Boolean> = MutableStateFlow(notificationsFlag)
        override fun setNotificationsEnabled(enabled: Boolean) { notificationsFlag = enabled }
        override fun getLastExpensePayingAccountId(): String? = lastPaying
        override fun setLastExpensePayingAccountId(accountId: String) { lastPaying = accountId }
        override fun getLastExpenseResponsibleAccountId(): String? = lastResponsible
        override fun setLastExpenseResponsibleAccountId(accountId: String) { lastResponsible = accountId }
        override fun getLastExpenseCategoryId(): String? = lastCategory
        override fun setLastExpenseCategoryId(categoryId: String) { lastCategory = categoryId }
        override fun getLastIncomeAccountId(): String? = null
        override fun setLastIncomeAccountId(accountId: String) {}
        override fun getLastIncomeCategoryId(): String? = null
        override fun setLastIncomeCategoryId(categoryId: String) {}
        override fun getAccountIcon(accountId: String): String? = null
        override fun setAccountIcon(accountId: String, icon: String) {}
    }

    private lateinit var viewModel: AddExpenseViewModel
    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var addExpenseUseCase: AddExpenseUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactions.clear()

        accountRepository = AccountRepository(fakeAccountDao)
        categoryRepository = CategoryRepository(fakeCategoryDao)
        expenseRepository = ExpenseRepository(fakeTransactionDao)
        addExpenseUseCase = AddExpenseUseCase(accountRepository, categoryRepository, expenseRepository)

        viewModel = AddExpenseViewModel(
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            preferencesRepository = fakePreferences,
            addExpenseUseCase = addExpenseUseCase,
            manageCategoriesUseCase = ManageCategoriesUseCase(categoryRepository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has zero amount, save disabled, and accounts loaded`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("0", state.amountInput)
        assertEquals(0L, state.amountMinor)
        assertEquals("0 €", state.displayAmount)
        assertFalse(state.isSaveEnabled)
        assertEquals(personalAccount.id, state.payingAccountId)
        assertEquals(familyAccount.id, state.responsibleAccountId)
        assertEquals(groceriesCategory.id, state.selectedCategoryId)
        assertNotNull(state.advanceNotice)
    }

    @Test
    fun `numeric keypad updates amount and enables save when positive`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onDigit('4')
        viewModel.onDigit('5')
        viewModel.onComma()
        viewModel.onDigit('5')
        viewModel.onDigit('0')

        advanceUntilIdle()
        val state = viewModel.uiState.value

        assertEquals("45,50", state.amountInput)
        assertEquals(4550L, state.amountMinor)
        assertEquals("45,50 €", state.displayAmount)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun `backspace and clear update amount accordingly`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onDigit('8')
        viewModel.onDigit('0')
        advanceUntilIdle()
        assertEquals("80", viewModel.uiState.value.amountInput)

        viewModel.onBackspace()
        advanceUntilIdle()
        assertEquals("8", viewModel.uiState.value.amountInput)

        viewModel.onClear()
        advanceUntilIdle()
        assertEquals("0", viewModel.uiState.value.amountInput)
        assertFalse(viewModel.uiState.value.isSaveEnabled)
    }

    @Test
    fun `advance notice changes based on paying and responsible accounts`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onPayingAccountSelected(personalAccount.id)
        viewModel.onResponsibleAccountSelected(familyAccount.id)
        advanceUntilIdle()
        assertEquals("Adelanto automático: Familia te deberá este importe", viewModel.uiState.value.advanceNotice)

        viewModel.onPayingAccountSelected(familyAccount.id)
        viewModel.onResponsibleAccountSelected(personalAccount.id)
        advanceUntilIdle()
        assertEquals("Adelanto automático: Deberás este importe a Familia", viewModel.uiState.value.advanceNotice)

        viewModel.onPayingAccountSelected(personalAccount.id)
        viewModel.onResponsibleAccountSelected(personalAccount.id)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.advanceNotice)
    }

    @Test
    fun `discard dialog is shown only when there are uncommitted changes`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        val events = mutableListOf<AddExpenseEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { events.add(it) }
        }
        advanceUntilIdle()

        viewModel.onBackClicked()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showDiscardDialog)
        assertEquals(1, events.size)
        assertTrue(events.first() is AddExpenseEvent.NavigateBack)

        viewModel.onDigit('9')
        advanceUntilIdle()
        viewModel.onBackClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDiscardDialog)

        viewModel.onConfirmDiscard()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showDiscardDialog)
    }

    @Test
    fun `saveExpense creates transaction, persists preferences, and emits Saved event`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        val events = mutableListOf<AddExpenseEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { events.add(it) }
        }
        advanceUntilIdle()

        viewModel.onDigit('8')
        viewModel.onDigit('0')
        viewModel.onCategorySelected(groceriesCategory.id)
        viewModel.onPayingAccountSelected(personalAccount.id)
        viewModel.onResponsibleAccountSelected(familyAccount.id)
        viewModel.onNoteChanged("Compra semanal")
        advanceUntilIdle()

        viewModel.saveExpense()
        advanceUntilIdle()

        assertEquals(1, transactions.size)
        val created = transactions.first()
        assertEquals(TransactionType.EXPENSE, created.type)
        assertEquals(8000L, created.amountMinor)
        assertEquals(personalAccount.id, created.payingAccountId)
        assertEquals(familyAccount.id, created.responsibleAccountId)
        assertEquals(groceriesCategory.id, created.categoryId)
        assertEquals("Compra semanal", created.note)

        assertEquals(personalAccount.id, fakePreferences.lastPaying)
        assertEquals(familyAccount.id, fakePreferences.lastResponsible)
        assertEquals(groceriesCategory.id, fakePreferences.lastCategory)

        assertTrue(events.any { it is AddExpenseEvent.Saved && it.transactionId == created.id })
    }
}
