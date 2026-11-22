package com.familyexpenses.app.feature.transfer

import androidx.lifecycle.SavedStateHandle
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.TransferDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.TransferRepository
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
import com.familyexpenses.app.domain.usecase.ExecuteTransferUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransferViewModelTest {

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

    private val accountsFlow = MutableStateFlow(listOf(personalAccount, familyAccount))
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

    private val fakeTransactionDao = object : TransactionDao {
        override suspend fun insertTransaction(transaction: TransactionEntity) {
            transactionsFlow.value = transactionsFlow.value + transaction
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
            transfersFlow.value = transfersFlow.value + transfer
        }
        override fun observeTransfers(): Flow<List<TransferEntity>> = transfersFlow
        override suspend fun getAllTransfers(): List<TransferEntity> = transfersFlow.value
        override suspend fun getTransferById(transferId: String): TransferEntity? =
            transfersFlow.value.find { it.id == transferId }
        override suspend fun deleteTransfer(transferId: String) {
            transfersFlow.value = transfersFlow.value.filterNot { it.id == transferId }
        }
    }

    private lateinit var viewModel: TransferViewModel
    private val computeNetDebtUseCase = ComputeNetDebtUseCase()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountsFlow.value = listOf(personalAccount, familyAccount)
        transactionsFlow.value = emptyList()
        transfersFlow.value = emptyList()

        val accountRepository = AccountRepository(fakeAccountDao)
        val expenseRepository = ExpenseRepository(fakeTransactionDao)
        val transferRepository = TransferRepository(fakeTransferDao)

        viewModel = TransferViewModel(
            savedStateHandle = SavedStateHandle(mapOf(TransferViewModel.INITIAL_ACCOUNT_ID_ARG to "account-personal")),
            accountRepository = accountRepository,
            expenseRepository = expenseRepository,
            transferRepository = transferRepository,
            computeNetDebtUseCase = computeNetDebtUseCase,
            executeTransferUseCase = ExecuteTransferUseCase(
                accountRepository = accountRepository,
                expenseRepository = expenseRepository,
                transferRepository = transferRepository,
                computeNetDebtUseCase = computeNetDebtUseCase,
            ),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state requires explicit type selection and keeps save disabled`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.selectedType)
        assertEquals("account-personal", state.sourceAccountId)
        assertEquals("account-family", state.destinationAccountId)
        assertEquals(0L, state.amountMinor)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun `normal transfer swaps accounts and saves without altering debt`() = runTest {
        val events = mutableListOf<TransferEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvents.collect { events.add(it) } }
        advanceUntilIdle()

        viewModel.onTypeSelected(TransferType.NORMAL)
        viewModel.onSwapAccounts()
        viewModel.onDigit('5')
        viewModel.onDigit('0')
        viewModel.onNoteChanged("Aporte mensual")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TransferType.NORMAL, state.selectedType)
        assertEquals("account-family", state.sourceAccountId)
        assertEquals("account-personal", state.destinationAccountId)
        assertEquals(5000L, state.amountMinor)
        assertTrue(state.isSaveEnabled)

        viewModel.saveTransfer()
        advanceUntilIdle()

        assertEquals(1, transfersFlow.value.size)
        val saved = transfersFlow.value.first()
        assertEquals(TransferType.NORMAL, saved.type)
        assertEquals("account-family", saved.sourceAccountId)
        assertEquals("account-personal", saved.destinationAccountId)
        assertEquals(5000L, saved.amountMinor)
        assertEquals("Aporte mensual", saved.note)
        assertTrue(events.firstOrNull() is TransferEvent.Saved)
    }

    @Test
    fun `reimbursement prefills direction and full pending debt when family owes personal`() = runTest {
        // Personal paga 80,50 € de Familia -> Familia debe a Personal (+8050)
        transactionsFlow.value = listOf(
            TransactionEntity(
                id = "tx-1",
                payingAccountId = "account-personal",
                responsibleAccountId = "account-family",
                categoryId = "cat-1",
                amountMinor = 8050L,
                type = TransactionType.EXPENSE,
                source = TransactionSource.MANUAL,
                occurredAt = 100L,
                createdAt = 100L,
            ),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onTypeSelected(TransferType.REIMBURSEMENT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(TransferType.REIMBURSEMENT, state.selectedType)
        assertEquals("account-family", state.sourceAccountId)
        assertEquals("account-personal", state.destinationAccountId)
        assertEquals("80,50", state.amountInput)
        assertEquals(8050L, state.amountMinor)
        assertNull(state.validationMessage)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun `reimbursement prefills reverse direction when personal owes family`() = runTest {
        // Familia paga 45 € de Personal -> Personal debe a Familia (-4500)
        transactionsFlow.value = listOf(
            TransactionEntity(
                id = "tx-1",
                payingAccountId = "account-family",
                responsibleAccountId = "account-personal",
                categoryId = "cat-1",
                amountMinor = 4500L,
                type = TransactionType.EXPENSE,
                source = TransactionSource.MANUAL,
                occurredAt = 100L,
                createdAt = 100L,
            ),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onTypeSelected(TransferType.REIMBURSEMENT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("account-personal", state.sourceAccountId)
        assertEquals("account-family", state.destinationAccountId)
        assertEquals("45", state.amountInput)
        assertEquals(4500L, state.amountMinor)
        assertTrue(state.isSaveEnabled)
    }

    @Test
    fun `reimbursement allows partial amount and blocks exceeding pending debt`() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(
                id = "tx-1",
                payingAccountId = "account-personal",
                responsibleAccountId = "account-family",
                categoryId = "cat-1",
                amountMinor = 8000L,
                type = TransactionType.EXPENSE,
                source = TransactionSource.MANUAL,
                occurredAt = 100L,
                createdAt = 100L,
            ),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onTypeSelected(TransferType.REIMBURSEMENT)
        advanceUntilIdle()

        // Superar deuda pendiente (100 > 80)
        viewModel.onClear()
        viewModel.onDigit('1')
        viewModel.onDigit('0')
        viewModel.onDigit('0')
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.validationMessage)
        assertFalse(viewModel.uiState.value.isSaveEnabled)

        // Devolución parcial (30 <= 80)
        viewModel.onClear()
        viewModel.onDigit('3')
        viewModel.onDigit('0')
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.validationMessage)
        assertTrue(viewModel.uiState.value.isSaveEnabled)

        viewModel.saveTransfer()
        advanceUntilIdle()

        assertEquals(1, transfersFlow.value.size)
        assertEquals(3000L, transfersFlow.value.first().amountMinor)
        assertEquals(TransferType.REIMBURSEMENT, transfersFlow.value.first().type)
        // Deuda restante debe ser 50 € (5000 céntimos)
        assertEquals(5000L, viewModel.uiState.value.netDebtMinor)
    }

    @Test
    fun `reimbursement with zero pending debt disables save and shows notice`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.onTypeSelected(TransferType.REIMBURSEMENT)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0L, state.pendingDebtMinor)
        assertEquals("No hay deuda pendiente que devolver.", state.validationMessage)
        assertFalse(state.isSaveEnabled)
    }

    @Test
    fun `back clicked prompts discard when edited and navigates back when clean`() = runTest {
        val events = mutableListOf<TransferEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiEvents.collect { events.add(it) } }
        advanceUntilIdle()

        // Limpio -> vuelve directo
        viewModel.onBackClicked()
        advanceUntilIdle()
        assertEquals(listOf(TransferEvent.NavigateBack), events)

        // Con datos introducidos -> pide confirmación
        viewModel.onTypeSelected(TransferType.NORMAL)
        viewModel.onDigit('1')
        viewModel.onBackClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDiscardDialog)

        viewModel.onConfirmDiscard()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.showDiscardDialog)
        assertEquals(2, events.size)
    }

    @Test
    fun `initialType REIMBURSEMENT prefills type direction and pending debt automatically`() = runTest {
        transactionsFlow.value = listOf(
            TransactionEntity(
                id = "tx-1",
                payingAccountId = "account-personal",
                responsibleAccountId = "account-family",
                categoryId = "cat-1",
                amountMinor = 6500L,
                type = TransactionType.EXPENSE,
                source = TransactionSource.MANUAL,
                occurredAt = 100L,
                createdAt = 100L,
            ),
        )
        val accountRepository = AccountRepository(fakeAccountDao)
        val expenseRepository = ExpenseRepository(fakeTransactionDao)
        val transferRepository = TransferRepository(fakeTransferDao)
        val prefilledVm = TransferViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    TransferViewModel.INITIAL_ACCOUNT_ID_ARG to "account-family",
                    TransferViewModel.INITIAL_TYPE_ARG to TransferType.REIMBURSEMENT.name,
                ),
            ),
            accountRepository = accountRepository,
            expenseRepository = expenseRepository,
            transferRepository = transferRepository,
            computeNetDebtUseCase = computeNetDebtUseCase,
            executeTransferUseCase = ExecuteTransferUseCase(
                accountRepository = accountRepository,
                expenseRepository = expenseRepository,
                transferRepository = transferRepository,
                computeNetDebtUseCase = computeNetDebtUseCase,
            ),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { prefilledVm.uiState.collect() }
        advanceUntilIdle()

        val state = prefilledVm.uiState.value
        assertEquals(TransferType.REIMBURSEMENT, state.selectedType)
        assertEquals("account-family", state.sourceAccountId)
        assertEquals("account-personal", state.destinationAccountId)
        assertEquals("65", state.amountInput)
        assertEquals(6500L, state.amountMinor)
        assertTrue(state.isSaveEnabled)
    }
}
