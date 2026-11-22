package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.OccurrenceStatus
import com.familyexpenses.app.core.model.RecurrenceExecutionMode
import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.RecurrenceRuleStatus
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.data.repository.BackupRepository
import com.familyexpenses.app.domain.model.BackupPreferencesSnapshot
import com.familyexpenses.app.domain.model.PortableBackupSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupRestoreUseCaseTest {

    private lateinit var storedSnapshot: PortableBackupSnapshot
    private lateinit var repository: BackupRepository
    private lateinit var useCase: BackupRestoreUseCase

    @Before
    fun setUp() {
        storedSnapshot = PortableBackupSnapshot(
            exportedAt = 1_700_000_000_000L,
            accounts = listOf(
                AccountEntity("account-personal", "Personal", AccountType.PERSONAL, "EUR", 100000L, false, 100L),
                AccountEntity("account-family", "Familia", AccountType.FAMILY, "EUR", 200000L, false, 200L),
            ),
            categories = listOf(
                CategoryEntity("cat-food", "Supermercado", "shopping_cart", CategoryType.EXPENSE, 1, true, 1, false, 100L),
                CategoryEntity("cat-sal", "Nómina", "payments", CategoryType.INCOME, 1, false, null, false, 100L),
            ),
            transactions = listOf(
                TransactionEntity(
                    id = "tx-1",
                    type = TransactionType.EXPENSE,
                    amountMinor = 8000L,
                    payingAccountId = "account-personal",
                    responsibleAccountId = "account-family",
                    categoryId = "cat-food",
                    occurredAt = 5000L,
                    note = "Compra \"grande\" con,\ncaracteres",
                    source = TransactionSource.MANUAL,
                    createdAt = 5000L,
                ),
            ),
            transfers = listOf(
                TransferEntity(
                    id = "tr-1",
                    type = TransferType.REIMBURSEMENT,
                    sourceAccountId = "account-family",
                    destinationAccountId = "account-personal",
                    amountMinor = 3000L,
                    occurredAt = 6000L,
                    note = "Parte 1",
                    createdAt = 6000L,
                ),
            ),
            balanceAdjustments = listOf(
                BalanceAdjustmentEntity(
                    id = "adj-1",
                    accountId = "account-personal",
                    amountDeltaMinor = 500L,
                    reason = "Efectivo encontrado",
                    occurredAt = 7000L,
                    createdAt = 7000L,
                ),
            ),
            recurrenceRules = listOf(
                RecurrenceRuleEntity(
                    id = "rule-1",
                    type = TransactionType.INCOME,
                    payingAccountId = "account-personal",
                    responsibleAccountId = "account-personal",
                    categoryId = "cat-sal",
                    amountMinor = 200000L,
                    note = "Mensual",
                    frequency = RecurrenceFrequency.MONTHLY,
                    dayOfMonth = 1,
                    startAt = 1000L,
                    endAt = null,
                    nextOccurrenceAt = 2000L,
                    status = RecurrenceRuleStatus.ACTIVE,
                    createdAt = 1000L,
                ),
            ),
            recurrenceOccurrences = listOf(
                RecurrenceOccurrenceEntity(
                    id = "occ-1",
                    ruleId = "rule-1",
                    status = OccurrenceStatus.PENDING,
                    scheduledAt = 2000L,
                    reviewAt = 2500L,
                    amountMinor = 200000L,
                    type = TransactionType.INCOME,
                    payingAccountId = "account-personal",
                    responsibleAccountId = "account-personal",
                    categoryId = "cat-sal",
                    note = "Ocurrencia 1",
                    transactionId = null,
                    createdAt = 2000L,
                ),
            ),
            preferences = BackupPreferencesSnapshot(
                lastExpensePayingAccountId = "account-personal",
                lastExpenseResponsibleAccountId = "account-family",
                lastExpenseCategoryId = "cat-food",
                privacyModeEnabled = true,
                accountIcons = mapOf("account-family" to "home", "account-personal" to "person"),
            ),
        )

        repository = object : BackupRepository {
            override suspend fun readSnapshot(): PortableBackupSnapshot = storedSnapshot
            override suspend fun replaceAll(snapshot: PortableBackupSnapshot) {
                storedSnapshot = snapshot
            }
        }
        useCase = BackupRestoreUseCase(repository)
    }

    @Test
    fun `round trip backup and full replacement preserve balances debt and preferences`() = runTest {
        val exported = useCase.exportBackupJson()
        assertTrue(exported.fileName.endsWith(".json"))
        assertEquals(1, exported.preview.transactionsCount)
        assertEquals(1, exported.preview.transfersCount)

        // Sobrescribir el estado local con datos distintos antes de restaurar
        storedSnapshot = PortableBackupSnapshot(
            exportedAt = 0L,
            accounts = listOf(AccountEntity("account-personal", "Vacio", AccountType.PERSONAL, "EUR", 0L, false, 0L)),
            categories = emptyList(),
            transactions = emptyList(),
            transfers = emptyList(),
            balanceAdjustments = emptyList(),
            recurrenceRules = emptyList(),
        )

        val preview = useCase.restoreBackup(exported.jsonContent)
        assertEquals(2, preview.accountsCount)
        assertEquals(2, preview.categoriesCount)
        assertEquals(1, preview.transactionsCount)
        assertEquals(1, preview.transfersCount)
        assertEquals(1, preview.adjustmentsCount)
        assertEquals(1, preview.recurrenceRulesCount)

        // Comprobar invariantes contables tras restauración
        val balanceUseCase = ComputeAccountBalanceUseCase()
        val debtUseCase = ComputeNetDebtUseCase()

        val personalBalance = balanceUseCase.compute(
            storedSnapshot.accounts[0],
            storedSnapshot.transactions,
            storedSnapshot.transfers,
            storedSnapshot.balanceAdjustments,
        )
        // Personal: 100000 - 8000 (gasto) + 3000 (devolucion) + 500 (ajuste) = 95500
        assertEquals(95500L, personalBalance)

        val familyBalance = balanceUseCase.compute(
            storedSnapshot.accounts[1],
            storedSnapshot.transactions,
            storedSnapshot.transfers,
            storedSnapshot.balanceAdjustments,
        )
        // Familia: 200000 - 3000 (devolucion) = 197000
        assertEquals(197000L, familyBalance)

        val netDebt = debtUseCase.compute(
            storedSnapshot.accounts,
            storedSnapshot.transactions,
            storedSnapshot.transfers,
        )
        // 8000 adelanto - 3000 devolución = 5000
        assertEquals(5000L, netDebt)
        assertEquals("Compra \"grande\" con,\ncaracteres", storedSnapshot.transactions.first().note)
        assertEquals("account-family", storedSnapshot.preferences.lastExpenseResponsibleAccountId)
        assertEquals(1, storedSnapshot.recurrenceOccurrences.size)
        assertEquals("occ-1", storedSnapshot.recurrenceOccurrences.first().id)
        assertEquals(2500L, storedSnapshot.recurrenceOccurrences.first().reviewAt)
        assertTrue(storedSnapshot.preferences.privacyModeEnabled)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects backup with unknown account foreign key`() {
        val corrupted = storedSnapshot.copy(
            transactions = listOf(
                storedSnapshot.transactions.first().copy(payingAccountId = "non-existent"),
            ),
        )
        val json = useCase.encodeSnapshot(corrupted)
        useCase.validateBackupJson(json)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects backup with unsupported schema version`() {
        val wrongVersion = storedSnapshot.copy(schemaVersion = 99)
        val json = useCase.encodeSnapshot(wrongVersion)
        useCase.validateBackupJson(json)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects backup exceeding 7 favorite expense categories`() {
        val eightFavs = (1..8).map { idx ->
            CategoryEntity(
                id = "cat-$idx",
                name = "Cat $idx",
                icon = "category",
                type = CategoryType.EXPENSE,
                orderIndex = idx,
                isFavorite = true,
                favoritePosition = idx,
                createdAt = 0L,
            )
        }
        val invalid = storedSnapshot.copy(
            categories = eightFavs,
            transactions = emptyList(),
            recurrenceRules = emptyList(),
        )
        val json = useCase.encodeSnapshot(invalid)
        useCase.validateBackupJson(json)
    }
}
