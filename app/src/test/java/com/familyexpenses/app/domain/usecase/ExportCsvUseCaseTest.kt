package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.data.repository.BackupRepository
import com.familyexpenses.app.domain.model.PortableBackupSnapshot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class ExportCsvUseCaseTest {

    @Test
    fun `exports unified validated csv escaping commas quotes and newlines accurately`() = runTest {
        val snapshot = PortableBackupSnapshot(
            exportedAt = 1_700_000_000_000L,
            accounts = listOf(
                AccountEntity("account-personal", "Personal", AccountType.PERSONAL, "EUR", 10000L, false, 0L),
                AccountEntity("account-family", "Familia", AccountType.FAMILY, "EUR", 20000L, false, 0L),
            ),
            categories = listOf(
                CategoryEntity("cat-food", "Supermercado", "shopping_cart", CategoryType.EXPENSE, 1, true, 1, false, 0L),
                CategoryEntity("cat-sal", "Nómina", "payments", CategoryType.INCOME, 1, false, null, false, 0L),
            ),
            transactions = listOf(
                TransactionEntity(
                    id = "tx-1",
                    type = TransactionType.EXPENSE,
                    amountMinor = 8050L,
                    payingAccountId = "account-personal",
                    responsibleAccountId = "account-family",
                    categoryId = "cat-food",
                    occurredAt = 5000L,
                    note = "Compra \"semanal\", fruta\ny pan",
                    source = TransactionSource.MANUAL,
                    createdAt = 5000L,
                ),
                TransactionEntity(
                    id = "tx-2",
                    type = TransactionType.INCOME,
                    amountMinor = 150005L,
                    payingAccountId = "account-personal",
                    responsibleAccountId = "account-personal",
                    categoryId = "cat-sal",
                    occurredAt = 4000L,
                    note = null,
                    source = TransactionSource.AUTO_RECURRENT,
                    createdAt = 4000L,
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
                    note = "Devolución parcial",
                    createdAt = 6000L,
                ),
            ),
            balanceAdjustments = listOf(
                BalanceAdjustmentEntity(
                    id = "adj-1",
                    accountId = "account-family",
                    amountDeltaMinor = -1205L,
                    reason = "Cuadre, caja \"efectivo\"",
                    occurredAt = 3000L,
                    createdAt = 3000L,
                ),
            ),
            recurrenceRules = emptyList(),
        )

        val repo = object : BackupRepository {
            override suspend fun readSnapshot(): PortableBackupSnapshot = snapshot
            override suspend fun replaceAll(snapshot: PortableBackupSnapshot) {}
        }

        val useCase = ExportCsvUseCase(repo)
        val result = useCase.export(zoneId = ZoneId.of("UTC"))

        assertEquals(4, result.rowCount)
        assertTrue(result.fileName.endsWith(".csv"))

        val records = useCase.parseCsvRecords(result.csvContent)
        assertEquals(5, records.size) // header + 4 rows
        assertEquals(ExportCsvUseCase.HEADERS, records[0])

        // Orden descendente por occurredAt: tr-1 (6000), tx-1 (5000), tx-2 (4000), adj-1 (3000)
        assertEquals("tr-1", records[1][0])
        assertEquals("DEVOLUCION_ADELANTO", records[1][1])
        assertEquals("30.00", records[1][3])

        assertEquals("tx-1", records[2][0])
        assertEquals("GASTO", records[2][1])
        assertEquals("-80.50", records[2][3])
        assertEquals("-8050", records[2][4])
        assertEquals("Personal", records[2][5])
        assertEquals("Familia", records[2][6])
        assertEquals("SI", records[2][8])
        assertEquals("Compra \"semanal\", fruta\ny pan", records[2][9])

        assertEquals("tx-2", records[3][0])
        assertEquals("INGRESO", records[3][1])
        assertEquals("1500.05", records[3][3])
        assertEquals("AUTO_RECURRENT", records[3][10])

        assertEquals("adj-1", records[4][0])
        assertEquals("AJUSTE_SALDO", records[4][1])
        assertEquals("-12.05", records[4][3])
        assertEquals("Cuadre, caja \"efectivo\"", records[4][9])
    }
}
