package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeMonthlyNetResultUseCaseTest {

    private val useCase = ComputeMonthlyNetResultUseCase()
    private val zoneId = ZoneId.of("Europe/Madrid")
    private val targetMonth = YearMonth.of(2026, 11)

    private fun millisOn(date: LocalDate): Long =
        date.atTime(12, 0).atZone(zoneId).toInstant().toEpochMilli()

    @Test
    fun `empty month returns zero`() {
        val result = useCase.compute(
            accountId = "acc-family",
            transactions = emptyList(),
            yearMonth = targetMonth,
            zoneId = zoneId,
        )
        assertEquals(0L, result)
    }

    @Test
    fun `computes net result using responsible account and ignores other months and adjustments`() {
        val txs = listOf(
            TransactionEntity(
                id = "inc-nov",
                type = TransactionType.INCOME,
                amountMinor = 200000L,
                payingAccountId = "acc-family",
                responsibleAccountId = "acc-family",
                categoryId = "cat-salary",
                source = TransactionSource.MANUAL,
                occurredAt = millisOn(LocalDate.of(2026, 11, 5)),
                createdAt = 1L,
            ),
            TransactionEntity(
                id = "exp-nov-advance",
                type = TransactionType.EXPENSE,
                amountMinor = 8000L,
                payingAccountId = "acc-personal",
                responsibleAccountId = "acc-family",
                categoryId = "cat-food",
                source = TransactionSource.MANUAL,
                occurredAt = millisOn(LocalDate.of(2026, 11, 10)),
                createdAt = 2L,
            ),
            TransactionEntity(
                id = "adj-nov",
                type = TransactionType.ADJUSTMENT,
                amountMinor = 50000L,
                payingAccountId = "acc-family",
                responsibleAccountId = "acc-family",
                categoryId = "",
                source = TransactionSource.MANUAL,
                occurredAt = millisOn(LocalDate.of(2026, 11, 12)),
                createdAt = 3L,
            ),
            TransactionEntity(
                id = "exp-oct",
                type = TransactionType.EXPENSE,
                amountMinor = 99900L,
                payingAccountId = "acc-family",
                responsibleAccountId = "acc-family",
                categoryId = "cat-food",
                source = TransactionSource.MANUAL,
                occurredAt = millisOn(LocalDate.of(2026, 10, 31)),
                createdAt = 4L,
            ),
            TransactionEntity(
                id = "exp-dec",
                type = TransactionType.EXPENSE,
                amountMinor = 10000L,
                payingAccountId = "acc-family",
                responsibleAccountId = "acc-family",
                categoryId = "cat-food",
                source = TransactionSource.MANUAL,
                occurredAt = millisOn(LocalDate.of(2026, 12, 1)),
                createdAt = 5L,
            ),
        )

        val familyNet = useCase.compute("acc-family", txs, yearMonth = targetMonth, zoneId = zoneId)
        val personalNet = useCase.compute("acc-personal", txs, yearMonth = targetMonth, zoneId = zoneId)

        assertEquals(192000L, familyNet)
        assertEquals(0L, personalNet)
    }

    @Test
    fun `negative monthly result preserves sign`() {
        val txs = listOf(
            TransactionEntity(
                id = "exp-1",
                type = TransactionType.EXPENSE,
                amountMinor = 15000L,
                payingAccountId = "acc-personal",
                responsibleAccountId = "acc-personal",
                categoryId = "cat-food",
                source = TransactionSource.MANUAL,
                occurredAt = millisOn(LocalDate.of(2026, 11, 15)),
                createdAt = 1L,
            ),
        )

        assertEquals(-15000L, useCase.compute("acc-personal", txs, yearMonth = targetMonth, zoneId = zoneId))
    }

    @Test
    fun `normal transfers in current month add to destination and subtract from source while reimbursements are excluded`() {
        val transfers = listOf(
            TransferEntity(
                id = "tr-normal-nov",
                sourceAccountId = "acc-personal",
                destinationAccountId = "acc-family",
                amountMinor = 30000L,
                type = TransferType.NORMAL,
                occurredAt = millisOn(LocalDate.of(2026, 11, 2)),
                createdAt = 1L,
            ),
            TransferEntity(
                id = "tr-reimb-nov",
                sourceAccountId = "acc-family",
                destinationAccountId = "acc-personal",
                amountMinor = 8000L,
                type = TransferType.REIMBURSEMENT,
                occurredAt = millisOn(LocalDate.of(2026, 11, 12)),
                createdAt = 2L,
            ),
            TransferEntity(
                id = "tr-normal-oct",
                sourceAccountId = "acc-personal",
                destinationAccountId = "acc-family",
                amountMinor = 50000L,
                type = TransferType.NORMAL,
                occurredAt = millisOn(LocalDate.of(2026, 10, 28)),
                createdAt = 3L,
            ),
        )

        val familyNet = useCase.compute("acc-family", emptyList(), transfers, targetMonth, zoneId)
        val personalNet = useCase.compute("acc-personal", emptyList(), transfers, targetMonth, zoneId)

        assertEquals(30000L, familyNet)
        assertEquals(-30000L, personalNet)
    }

    @Test
    fun `formatMonthName returns spanish lowercase month`() {
        assertEquals("noviembre", useCase.formatMonthName(YearMonth.of(2026, 11)))
    }
}
