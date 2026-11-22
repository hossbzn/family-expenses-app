package com.familyexpenses.app.feature.account

import androidx.lifecycle.SavedStateHandle
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.BalanceAdjustmentDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.TransferDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.BalanceAdjustmentRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.TransferRepository
import com.familyexpenses.app.domain.usecase.ComputeAccountBalanceUseCase
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
import com.familyexpenses.app.domain.usecase.CreateBalanceAdjustmentUseCase
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
class AccountDetailViewModelTest {

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
    private val adjustmentsFlow = MutableStateFlow<List<BalanceAdjustmentEntity>>(emptyList())

    private val fakeAccountDao = object : AccountDao {
        override suspend fun upsert(account: AccountEntity) {
            accountsFlow.value = accountsFlow.value.filterNot { it.id == account.id } + account
        }
        override suspend fun upsertAll(accounts: List<AccountEntity>) { accountsFlow.value = accounts }
        override fun observeAccounts(): Flow<List<AccountEntity>> = accountsFlow
        override suspend fun getAllAccounts(): List<AccountEntity> = accountsFlow.value
        override suspend fun getAccountById(accountId: String): AccountEntity? =
            accountsFlow.value.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accountsFlow.value.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {
            accountsFlow.value = accountsFlow.value.map {
                if (it.id == accountId) it.copy(initialBalanceMinor = initialBalanceMinor) else it
            }
        }
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

    private val fakeAdjustmentDao = object : BalanceAdjustmentDao {
        override suspend fun insertAdjustment(adjustment: BalanceAdjustmentEntity) {
            adjustmentsFlow.value = adjustmentsFlow.value + adjustment
        }
        override fun observeAdjustments(): Flow<List<BalanceAdjustmentEntity>> = adjustmentsFlow
        override fun observeAdjustmentsForAccount(accountId: String): Flow<List<BalanceAdjustmentEntity>> = adjustmentsFlow
        override suspend fun getAllAdjustments(): List<BalanceAdjustmentEntity> = adjustmentsFlow.value
    }

    private lateinit var viewModel: AccountDetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        accountsFlow.value = listOf(personalAccount, familyAccount)
        transactionsFlow.value = emptyList()
        transfersFlow.value = emptyList()
        adjustmentsFlow.value = emptyList()

        val accountRepository = AccountRepository(fakeAccountDao)
        val expenseRepository = ExpenseRepository(fakeTransactionDao)
        val transferRepository = TransferRepository(fakeTransferDao)
        val adjustmentRepository = BalanceAdjustmentRepository(fakeAdjustmentDao)

        viewModel = AccountDetailViewModel(
            savedStateHandle = SavedStateHandle(mapOf(AccountDetailViewModel.ACCOUNT_ID_ARG to "account-personal")),
            accountRepository = accountRepository,
            expenseRepository = expenseRepository,
            transferRepository = transferRepository,
            balanceAdjustmentRepository = adjustmentRepository,
            computeAccountBalanceUseCase = ComputeAccountBalanceUseCase(),
            computeNetDebtUseCase = ComputeNetDebtUseCase(),
            createBalanceAdjustmentUseCase = CreateBalanceAdjustmentUseCase(accountRepository, adjustmentRepository),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows real cash balance and net debt for selected account`() = runTest {
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

        val state = viewModel.uiState.value
        assertEquals("Personal", state.account?.name)
        assertEquals(92000L, state.cashBalanceMinor)
        assertEquals(8000L, state.netDebtMinor)
    }

    @Test
    fun `balance adjustment requires positive amount and mandatory reason`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.openAdjustmentDialog()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAdjustmentDialogOpen)

        // Blank amount fails
        viewModel.saveBalanceAdjustment()
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.adjustmentErrorMessage)
        assertTrue(adjustmentsFlow.value.isEmpty())

        // Positive amount without reason fails
        viewModel.onAdjustmentAmountChanged("25,50")
        viewModel.saveBalanceAdjustment()
        advanceUntilIdle()
        assertEquals("El motivo es obligatorio para el ajuste.", viewModel.uiState.value.adjustmentErrorMessage)
        assertTrue(adjustmentsFlow.value.isEmpty())

        // Valid negative adjustment with reason updates cash balance without affecting debt
        viewModel.onAdjustmentSignChanged(false)
        viewModel.onAdjustmentReasonChanged("Cuadre de efectivo")
        viewModel.saveBalanceAdjustment()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isAdjustmentDialogOpen)
        assertNull(viewModel.uiState.value.adjustmentErrorMessage)
        assertEquals(1, adjustmentsFlow.value.size)
        assertEquals(-2550L, adjustmentsFlow.value.first().amountDeltaMinor)
        assertEquals("Cuadre de efectivo", adjustmentsFlow.value.first().reason)
        assertEquals(97450L, viewModel.uiState.value.cashBalanceMinor)
        assertEquals(0L, viewModel.uiState.value.netDebtMinor)
    }

    @Test
    fun `initial balance update recalculates real cash balance`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        viewModel.openInitialBalanceDialog()
        advanceUntilIdle()
        assertEquals("1000", viewModel.uiState.value.initialBalanceInput)

        viewModel.onInitialBalanceChanged("1500,25")
        viewModel.saveInitialBalance()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isInitialBalanceDialogOpen)
        assertEquals(150025L, viewModel.uiState.value.cashBalanceMinor)
    }

    @Test
    fun `undoTransfer deletes transfer and restores balance`() = runTest {
        transfersFlow.value = listOf(
            TransferEntity(
                id = "tr-1",
                type = TransferType.NORMAL,
                sourceAccountId = "account-personal",
                destinationAccountId = "account-family",
                amountMinor = 10000L,
                occurredAt = 100L,
                createdAt = 100L,
            ),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        assertEquals(90000L, viewModel.uiState.value.cashBalanceMinor)

        viewModel.undoTransfer("tr-1")
        advanceUntilIdle()

        assertEquals(100000L, viewModel.uiState.value.cashBalanceMinor)
        assertTrue(transfersFlow.value.isEmpty())
    }
}
