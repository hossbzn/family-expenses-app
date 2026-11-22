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
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.TransferRepository
import com.familyexpenses.app.domain.model.MovementKind
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
import com.familyexpenses.app.domain.usecase.UpdateTransactionUseCase
import com.familyexpenses.app.domain.usecase.UpdateTransferUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovementDetailViewModelTest {

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

    private val homeCategory = CategoryEntity(
        id = "cat-home",
        name = "Hogar",
        icon = "home",
        type = CategoryType.EXPENSE,
        orderIndex = 2,
        isFavorite = true,
        favoritePosition = 2,
        createdAt = 0L,
    )

    private val accountsFlow = MutableStateFlow(listOf(personalAccount, familyAccount))
    private val categoriesFlow = MutableStateFlow(listOf(foodCategory, homeCategory))
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
        override suspend fun insertTransaction(transaction: TransactionEntity) {
            transactionsFlow.value = transactionsFlow.value.filterNot { it.id == transaction.id } + transaction
        }
        override fun observeTransactions(): Flow<List<TransactionEntity>> = transactionsFlow
        override suspend fun getAllTransactions(): List<TransactionEntity> = transactionsFlow.value
        override suspend fun getTransactionById(transactionId: String): TransactionEntity? =
            transactionsFlow.value.find { it.id == transactionId }
        override suspend fun deleteTransaction(transactionId: String) {
            transactionsFlow.value = transactionsFlow.value.filterNot { it.id == transactionId }
        }
    }

    private val fakeTransferDao = object : TransferDao {
        override suspend fun insertTransfer(transfer: TransferEntity) {
            transfersFlow.value = transfersFlow.value.filterNot { it.id == transfer.id } + transfer
        }
        override fun observeTransfers(): Flow<List<TransferEntity>> = transfersFlow
        override suspend fun getAllTransfers(): List<TransferEntity> = transfersFlow.value
        override suspend fun getTransferById(transferId: String): TransferEntity? =
            transfersFlow.value.find { it.id == transferId }
        override suspend fun deleteTransfer(transferId: String) {
            transfersFlow.value = transfersFlow.value.filterNot { it.id == transferId }
        }
    }

    private lateinit var accountRepository: AccountRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var transferRepository: TransferRepository
    private val computeNetDebtUseCase = ComputeNetDebtUseCase()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactionsFlow.value = emptyList()
        transfersFlow.value = emptyList()

        accountRepository = AccountRepository(fakeAccountDao)
        categoryRepository = CategoryRepository(fakeCategoryDao)
        expenseRepository = ExpenseRepository(fakeTransactionDao)
        transferRepository = TransferRepository(fakeTransferDao)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(kind: MovementKind, id: String): MovementDetailViewModel =
        MovementDetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    MovementDetailViewModel.MOVEMENT_KIND_ARG to kind.name,
                    MovementDetailViewModel.MOVEMENT_ID_ARG to id,
                ),
            ),
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            expenseRepository = expenseRepository,
            transferRepository = transferRepository,
            updateTransactionUseCase = UpdateTransactionUseCase(accountRepository, categoryRepository, expenseRepository),
            updateTransferUseCase = UpdateTransferUseCase(
                accountRepository,
                expenseRepository,
                transferRepository,
                computeNetDebtUseCase,
            ),
        )

    @Test
    fun `editing expense updates amount, category, responsible account, and recalculates net debt`() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(
                id = "tx-1",
                type = TransactionType.EXPENSE,
                amountMinor = 4000L,
                payingAccountId = "account-personal",
                responsibleAccountId = "account-personal",
                categoryId = "cat-food",
                occurredAt = 1000L,
                note = "Inicial",
                source = TransactionSource.MANUAL,
                createdAt = 1000L,
            ),
        )

        val viewModel = createViewModel(MovementKind.EXPENSE, "tx-1")
        val events = mutableListOf<MovementDetailEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvents.collect { events.add(it) } }
        advanceUntilIdle()

        assertEquals("40", viewModel.uiState.value.amountInput)

        viewModel.onAmountChanged("85,50")
        viewModel.onResponsibleAccountSelected("account-family")
        viewModel.onCategorySelected("cat-home")
        viewModel.onNoteChanged("Editado")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAdvance)
        viewModel.saveChanges()
        advanceUntilIdle()

        val updated = transactionsFlow.value.first()
        assertEquals(8550L, updated.amountMinor)
        assertEquals("account-personal", updated.payingAccountId)
        assertEquals("account-family", updated.responsibleAccountId)
        assertEquals("cat-home", updated.categoryId)
        assertEquals("Editado", updated.note)
        assertEquals(listOf(MovementDetailEvent.Saved), events)

        val debt = computeNetDebtUseCase.compute(accountsFlow.value, transactionsFlow.value, transfersFlow.value)
        assertEquals(8550L, debt)
    }

    @Test
    fun `deleting expense removes it and emits Deleted event`() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(
                id = "tx-del",
                type = TransactionType.EXPENSE,
                amountMinor = 5000L,
                payingAccountId = "account-personal",
                responsibleAccountId = "account-family",
                categoryId = "cat-food",
                occurredAt = 1000L,
                source = TransactionSource.MANUAL,
                createdAt = 1000L,
            ),
        )

        val viewModel = createViewModel(MovementKind.EXPENSE, "tx-del")
        val events = mutableListOf<MovementDetailEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvents.collect { events.add(it) } }
        advanceUntilIdle()

        viewModel.onDeleteClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDeleteDialog)

        viewModel.onConfirmDelete()
        advanceUntilIdle()

        assertTrue(transactionsFlow.value.isEmpty())
        assertEquals(listOf(MovementDetailEvent.Deleted), events)
    }

    @Test
    fun `editing reimbursement enforces debt cap excluding itself`() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(
                id = "tx-1",
                type = TransactionType.EXPENSE,
                amountMinor = 8000L,
                payingAccountId = "account-personal",
                responsibleAccountId = "account-family",
                categoryId = "cat-food",
                occurredAt = 1000L,
                source = TransactionSource.MANUAL,
                createdAt = 1000L,
            ),
        )
        transfersFlow.value = listOf(
            TransferEntity(
                id = "tr-1",
                type = TransferType.REIMBURSEMENT,
                sourceAccountId = "account-family",
                destinationAccountId = "account-personal",
                amountMinor = 3000L,
                occurredAt = 2000L,
                createdAt = 2000L,
            ),
        )

        val viewModel = createViewModel(MovementKind.REIMBURSEMENT, "tr-1")
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Intentar editar a 90 € cuando la deuda bruta sin esta devolución es 80 € -> debe fallar
        viewModel.onAmountChanged("90")
        advanceUntilIdle()
        viewModel.saveChanges()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertEquals(3000L, transfersFlow.value.first().amountMinor)

        // Editar a 60 € (<= 80 €) -> debe guardar y dejar deuda en 20 €
        viewModel.onAmountChanged("60")
        advanceUntilIdle()
        viewModel.saveChanges()
        advanceUntilIdle()

        assertEquals(6000L, transfersFlow.value.first().amountMinor)
        val remainingDebt = computeNetDebtUseCase.compute(accountsFlow.value, transactionsFlow.value, transfersFlow.value)
        assertEquals(2000L, remainingDebt)
    }
}
