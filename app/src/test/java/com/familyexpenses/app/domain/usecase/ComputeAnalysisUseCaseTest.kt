package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class ComputeAnalysisUseCaseTest {

    private val useCase = ComputeAnalysisUseCase(FilterMovementsUseCase())
    private val zoneId = ZoneId.of("UTC")

    private fun epochOn(date: LocalDate): Long =
        date.atStartOfDay(zoneId).toInstant().toEpochMilli() + 3_600_000L

    private val categories = listOf(
        CategoryEntity(
            id = "cat-food",
            name = "Supermercado",
            icon = "shopping_cart",
            type = CategoryType.EXPENSE,
            orderIndex = 1,
            isFavorite = true,
            favoritePosition = 1,
            createdAt = 0L,
        ),
        CategoryEntity(
            id = "cat-archived",
            name = "Antigua",
            icon = "category",
            type = CategoryType.EXPENSE,
            orderIndex = 2,
            isFavorite = false,
            isArchived = true,
            createdAt = 0L,
        ),
        CategoryEntity(
            id = "cat-salary",
            name = "Nómina",
            icon = "payments",
            type = CategoryType.INCOME,
            orderIndex = 1,
            isFavorite = false,
            createdAt = 0L,
        ),
    )

    @Test
    fun `attributes expenses to responsible account excludes adjustments and preserves archived categories`() {
        val today = LocalDate.of(2026, 6, 15)
        val txs = listOf(
            // Personal paga 80 € atribuidos a Familia -> cuenta para Familia, NO para Personal
            TransactionEntity(
                id = "tx-1",
                type = TransactionType.EXPENSE,
                amountMinor = 8000L,
                payingAccountId = "account-personal",
                responsibleAccountId = "account-family",
                categoryId = "cat-food",
                occurredAt = epochOn(LocalDate.of(2026, 6, 5)),
                source = TransactionSource.MANUAL,
                createdAt = 0L,
            ),
            // Familia paga 20 € de Familia en categoría archivada -> debe conservarse en análisis
            TransactionEntity(
                id = "tx-2",
                type = TransactionType.EXPENSE,
                amountMinor = 2000L,
                payingAccountId = "account-family",
                responsibleAccountId = "account-family",
                categoryId = "cat-archived",
                occurredAt = epochOn(LocalDate.of(2026, 6, 10)),
                source = TransactionSource.MANUAL,
                createdAt = 0L,
            ),
            // Ajuste de saldo -> debe excluirse de gastos e ingresos
            TransactionEntity(
                id = "tx-adj",
                type = TransactionType.ADJUSTMENT,
                amountMinor = 5000L,
                payingAccountId = "account-family",
                responsibleAccountId = "account-family",
                categoryId = "cat-food",
                occurredAt = epochOn(LocalDate.of(2026, 6, 12)),
                source = TransactionSource.MANUAL,
                createdAt = 0L,
            ),
            // Ingreso en Familia -> excluido en serie EXPENSES
            TransactionEntity(
                id = "tx-inc",
                type = TransactionType.INCOME,
                amountMinor = 150000L,
                payingAccountId = "account-family",
                responsibleAccountId = "account-family",
                categoryId = "cat-salary",
                occurredAt = epochOn(LocalDate.of(2026, 6, 1)),
                source = TransactionSource.MANUAL,
                createdAt = 0L,
            ),
        )

        // Cuenta Familia, Gastos
        val familyExpenses = useCase.compute(
            transactions = txs,
            categories = categories,
            accountFilter = MovementAccountFilter.Account("account-family"),
            periodFilter = MovementPeriodFilter.Month(YearMonth.of(2026, 6)),
            seriesType = AnalysisSeriesType.EXPENSES,
            today = today,
            zoneId = zoneId,
        )

        assertEquals(10000L, familyExpenses.totalMinor)
        assertEquals(2, familyExpenses.transactionCount)
        assertEquals(listOf("cat-food", "cat-archived"), familyExpenses.categorySlices.map { it.categoryId })
        assertEquals(80f, familyExpenses.categorySlices[0].percentage, 0.01f)
        assertEquals(20f, familyExpenses.categorySlices[1].percentage, 0.01f)

        // Cuenta Personal, Gastos -> 0 (tx-1 fue pagado por Personal pero imputado a Familia)
        val personalExpenses = useCase.compute(
            transactions = txs,
            categories = categories,
            accountFilter = MovementAccountFilter.Account("account-personal"),
            periodFilter = MovementPeriodFilter.Month(YearMonth.of(2026, 6)),
            seriesType = AnalysisSeriesType.EXPENSES,
            today = today,
            zoneId = zoneId,
        )
        assertEquals(0L, personalExpenses.totalMinor)
        assertTrue(personalExpenses.categorySlices.isEmpty())

        // Serie INCOMES en Familia -> solo tx-inc (sin ajustes)
        val familyIncomes = useCase.compute(
            transactions = txs,
            categories = categories,
            accountFilter = MovementAccountFilter.Account("account-family"),
            periodFilter = MovementPeriodFilter.Month(YearMonth.of(2026, 6)),
            seriesType = AnalysisSeriesType.INCOMES,
            today = today,
            zoneId = zoneId,
        )
        assertEquals(150000L, familyIncomes.totalMinor)
        assertEquals(1, familyIncomes.categorySlices.size)
        assertEquals("cat-salary", familyIncomes.categorySlices.first().categoryId)
    }

    @Test
    fun `year evolution marks future months as null rather than fictitious zero`() {
        val today = LocalDate.of(2026, 4, 10)
        val txs = listOf(
            TransactionEntity(
                id = "tx-jan",
                type = TransactionType.EXPENSE,
                amountMinor = 4000L,
                payingAccountId = "account-family",
                responsibleAccountId = "account-family",
                categoryId = "cat-food",
                occurredAt = epochOn(LocalDate.of(2026, 1, 15)),
                source = TransactionSource.MANUAL,
                createdAt = 0L,
            ),
            TransactionEntity(
                id = "tx-apr",
                type = TransactionType.EXPENSE,
                amountMinor = 6000L,
                payingAccountId = "account-family",
                responsibleAccountId = "account-family",
                categoryId = "cat-food",
                occurredAt = epochOn(LocalDate.of(2026, 4, 3)),
                source = TransactionSource.MANUAL,
                createdAt = 0L,
            ),
        )

        val result = useCase.compute(
            transactions = txs,
            categories = categories,
            accountFilter = MovementAccountFilter.All,
            periodFilter = MovementPeriodFilter.Year(2026),
            seriesType = AnalysisSeriesType.EXPENSES,
            today = today,
            zoneId = zoneId,
        )

        assertEquals(12, result.evolutionBuckets.size)
        // Enero (idx 0): 4000
        assertEquals(4000L, result.evolutionBuckets[0].amountMinor)
        // Febrero y Marzo (idx 1, 2): transcurridos sin gastos => 0L
        assertEquals(0L, result.evolutionBuckets[1].amountMinor)
        assertEquals(0L, result.evolutionBuckets[2].amountMinor)
        // Abril (idx 3): 6000
        assertEquals(6000L, result.evolutionBuckets[3].amountMinor)
        // Mayo..Diciembre (idx 4..11): futuros => null
        for (i in 4..11) {
            assertTrue(result.evolutionBuckets[i].isFuture)
            assertNull(result.evolutionBuckets[i].amountMinor)
        }
        // Media sobre los 4 meses transcurridos (10000 / 4 = 2500)
        assertEquals(2500L, result.averagePerBucketMinor)
    }

    @Test
    fun `month and custom periods produce expected bucket granularities`() {
        val today = LocalDate.of(2026, 3, 15)

        val monthResult = useCase.compute(
            transactions = emptyList(),
            categories = categories,
            accountFilter = MovementAccountFilter.All,
            periodFilter = MovementPeriodFilter.Month(YearMonth.of(2026, 3)),
            seriesType = AnalysisSeriesType.EXPENSES,
            today = today,
            zoneId = zoneId,
        )
        assertEquals(31, monthResult.evolutionBuckets.size)
        assertFalse(monthResult.evolutionBuckets[14].isFuture) // 15 de marzo
        assertTrue(monthResult.evolutionBuckets[15].isFuture) // 16 de marzo futuro

        // Rango personalizado de 60 días -> tramos semanales
        val customWeekly = useCase.compute(
            transactions = emptyList(),
            categories = categories,
            accountFilter = MovementAccountFilter.All,
            periodFilter = MovementPeriodFilter.Custom(
                startDate = LocalDate.of(2026, 1, 1),
                endDate = LocalDate.of(2026, 3, 1),
            ),
            seriesType = AnalysisSeriesType.EXPENSES,
            today = today,
            zoneId = zoneId,
        )
        assertEquals(9, customWeekly.evolutionBuckets.size) // 60 días / 7 = 8 semanas completas + 4 días
    }
}
