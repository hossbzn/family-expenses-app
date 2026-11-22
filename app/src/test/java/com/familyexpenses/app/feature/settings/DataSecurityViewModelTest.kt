package com.familyexpenses.app.feature.settings

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.repository.BackupRepository
import com.familyexpenses.app.domain.model.PortableBackupSnapshot
import com.familyexpenses.app.domain.usecase.BackupRestoreUseCase
import com.familyexpenses.app.domain.usecase.ExportCsvUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class DataSecurityViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private var currentSnapshot = PortableBackupSnapshot(
        exportedAt = 1_700_000_000_000L,
        accounts = listOf(
            AccountEntity("account-personal", "Personal", AccountType.PERSONAL, "EUR", 0L, false, 0L),
            AccountEntity("account-family", "Familia", AccountType.FAMILY, "EUR", 0L, false, 0L),
        ),
        categories = listOf(
            CategoryEntity("cat-1", "Supermercado", "shopping_cart", CategoryType.EXPENSE, 1, true, 1, false, 0L),
        ),
        transactions = emptyList(),
        transfers = emptyList(),
        balanceAdjustments = emptyList(),
        recurrenceRules = emptyList(),
    )

    private val fakeRepo = object : BackupRepository {
        override suspend fun readSnapshot(): PortableBackupSnapshot = currentSnapshot
        override suspend fun replaceAll(snapshot: PortableBackupSnapshot) {
            currentSnapshot = snapshot
        }
    }

    private lateinit var backupRestoreUseCase: BackupRestoreUseCase
    private lateinit var viewModel: DataSecurityViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        backupRestoreUseCase = BackupRestoreUseCase(fakeRepo)
        viewModel = DataSecurityViewModel(
            exportCsvUseCase = ExportCsvUseCase(fakeRepo),
            backupRestoreUseCase = backupRestoreUseCase,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `emits SAF events for CSV and JSON export`() = runTest {
        val events = mutableListOf<DataSecurityEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { events.add(it) }
        }
        advanceUntilIdle()

        viewModel.onExportCsvClick()
        advanceUntilIdle()
        assertTrue(events.any { it is DataSecurityEvent.LaunchCreateCsv })

        viewModel.onExportBackupClick()
        advanceUntilIdle()
        assertTrue(events.any { it is DataSecurityEvent.LaunchCreateBackup })
    }

    @Test
    fun `validates backup before confirmation and replaces data on confirm`() = runTest {
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.currentSummary?.transactionsCount)

        // JSON inválido -> error sin confirmación
        viewModel.onBackupContentLoaded("{ invalid json")
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.pendingRestore)
        assertTrue(viewModel.uiState.value.isError)

        // JSON válido con 1 transacción -> abre confirmación y tras confirmar reemplaza datos
        val validSnapshot = currentSnapshot.copy(
            transactions = listOf(
                TransactionEntity(
                    id = "tx-restored",
                    type = TransactionType.EXPENSE,
                    amountMinor = 2500L,
                    payingAccountId = "account-personal",
                    responsibleAccountId = "account-family",
                    categoryId = "cat-1",
                    occurredAt = 5000L,
                    source = TransactionSource.MANUAL,
                    createdAt = 5000L,
                ),
            ),
        )
        val validJson = backupRestoreUseCase.encodeSnapshot(validSnapshot)

        viewModel.onBackupContentLoaded(validJson)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.pendingRestore)
        assertEquals(1, viewModel.uiState.value.pendingRestore?.preview?.transactionsCount)

        viewModel.onConfirmRestore()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingRestore)
        assertFalse(viewModel.uiState.value.isError)
        assertEquals(1, viewModel.uiState.value.currentSummary?.transactionsCount)
        assertEquals("tx-restored", currentSnapshot.transactions.first().id)
    }
}
