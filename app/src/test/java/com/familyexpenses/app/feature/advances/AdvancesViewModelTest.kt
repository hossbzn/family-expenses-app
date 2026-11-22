package com.familyexpenses.app.feature.advances

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
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.HistoryRepository
import com.familyexpenses.app.data.repository.TransferRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.domain.model.MovementKind
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdvancesViewModelTest {

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

    private lateinit var viewModel: AdvancesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        transactionsFlow.value = emptyList()
        transfersFlow.value = emptyList()

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

        viewModel = AdvancesViewModel(
            accountRepository = AccountRepository(fakeAccountDao),
            expenseRepository = ExpenseRepository(fakeTransactionDao),
            transferRepository = TransferRepository(fakeTransferDao),
            historyRepository = HistoryRepository(
                transactionDao = fakeTransactionDao,
                transferDao = fakeTransferDao,
                accountDao = fakeAccountDao,
                categoryDao = fakeCategoryDao,
            ),
            userPreferencesRepository = fakePreferences,
            computeNetDebtUseCase = ComputeNetDebtUseCase(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `zero debt state shows zero net debt and no debtor account`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0L, state.netDebtMinor)
        assertNull(state.debtorAccountId)
        assertEquals(0, state.advancesCount)
        assertEquals(0, state.reimbursementsCount)
    }

    @Test
    fun `bidirectional net debt and unified chronological history with filters work accurately`() = runTest {
        transactionsFlow.value = listOf(
            // Personal paga 80 € de Familia (+8000)
            TransactionEntity(
                id = "tx-adv-1",
                type = TransactionType.EXPENSE,
                amountMinor = 8000L,
                payingAccountId = "account-personal",
                responsibleAccountId = "account-family",
                categoryId = "cat-food",
                occurredAt = 1000L,
                note = "Compra familia",
                source = TransactionSource.MANUAL,
                createdAt = 1000L,
            ),
            // Gasto normal sin adelanto (NO debe aparecer en historial de adelantos)
            TransactionEntity(
                id = "tx-norm",
                type = TransactionType.EXPENSE,
                amountMinor = 2000L,
                payingAccountId = "account-personal",
                responsibleAccountId = "account-personal",
                categoryId = "cat-food",
                occurredAt = 1500L,
                source = TransactionSource.MANUAL,
                createdAt = 1500L,
            ),
            // Familia paga 100 € de Personal (-10000 -> neto -2000: Personal debe 20 € a Familia)
            TransactionEntity(
                id = "tx-adv-2",
                type = TransactionType.EXPENSE,
                amountMinor = 10000L,
                payingAccountId = "account-family",
                responsibleAccountId = "account-personal",
                categoryId = "cat-food",
                occurredAt = 2000L,
                note = "Pago por Personal",
                source = TransactionSource.MANUAL,
                createdAt = 2000L,
            ),
        )
        transfersFlow.value = listOf(
            // Transferencia normal (NO debe aparecer en historial de adelantos)
            TransferEntity(
                id = "tr-norm",
                type = TransferType.NORMAL,
                sourceAccountId = "account-personal",
                destinationAccountId = "account-family",
                amountMinor = 5000L,
                occurredAt = 2500L,
                createdAt = 2500L,
            ),
            // Devolución parcial Personal -> Familia de 5 € (+500 -> neto -1500)
            TransferEntity(
                id = "tr-reimb",
                type = TransferType.REIMBURSEMENT,
                sourceAccountId = "account-personal",
                destinationAccountId = "account-family",
                amountMinor = 500L,
                occurredAt = 3000L,
                note = "Devolución parcial",
                createdAt = 3000L,
            ),
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        // Filtro ALL: 2 adelantos + 1 devolución = 3 elementos en orden descendente
        var state = viewModel.uiState.value
        assertEquals(-1500L, state.netDebtMinor)
        assertEquals("account-personal", state.debtorAccountId)
        assertEquals(2, state.advancesCount)
        assertEquals(1, state.reimbursementsCount)
        assertEquals(listOf("tr-reimb", "tx-adv-2", "tx-adv-1"), state.items.map { it.id })
        assertEquals(MovementKind.REIMBURSEMENT, state.items[0].kind)
        assertEquals(MovementKind.EXPENSE, state.items[1].kind)

        // Filtro ADVANCES
        viewModel.onFilterSelected(AdvancesHistoryFilter.ADVANCES)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(listOf("tx-adv-2", "tx-adv-1"), state.items.map { it.id })

        // Filtro REIMBURSEMENTS
        viewModel.onFilterSelected(AdvancesHistoryFilter.REIMBURSEMENTS)
        advanceUntilIdle()
        state = viewModel.uiState.value
        assertEquals(listOf("tr-reimb"), state.items.map { it.id })
    }
}
