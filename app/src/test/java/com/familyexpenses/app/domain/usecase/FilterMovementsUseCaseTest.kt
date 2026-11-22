package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.domain.model.HistoryItem
import com.familyexpenses.app.domain.model.MovementKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class FilterMovementsUseCaseTest {

    private val useCase = FilterMovementsUseCase()
    private val zoneId = ZoneId.of("Europe/Madrid")

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(zoneId).toInstant().toEpochMilli() + 3_600_000L

    private val advanceExpense = HistoryItem(
        id = "exp-adv",
        kind = MovementKind.EXPENSE,
        amountMinor = 8000L,
        occurredAt = epochOf(2026, 11, 15),
        createdAt = 100L,
        payingAccountId = "account-personal",
        payingAccountName = "Personal",
        payingAccountType = AccountType.PERSONAL,
        responsibleAccountId = "account-family",
        responsibleAccountName = "Familia",
        responsibleAccountType = AccountType.FAMILY,
        categoryId = "cat-groceries",
        categoryName = "Supermercado",
        categoryIcon = "shopping_cart",
        note = "Compra semanal",
        isAdvance = true,
    )

    private val personalExpense = HistoryItem(
        id = "exp-pers",
        kind = MovementKind.EXPENSE,
        amountMinor = 1500L,
        occurredAt = epochOf(2026, 11, 14),
        createdAt = 90L,
        payingAccountId = "account-personal",
        payingAccountName = "Personal",
        payingAccountType = AccountType.PERSONAL,
        responsibleAccountId = "account-personal",
        responsibleAccountName = "Personal",
        responsibleAccountType = AccountType.PERSONAL,
        categoryId = "cat-transport",
        categoryName = "Transporte",
        categoryIcon = "directions_car",
        note = "Metro",
        isAdvance = false,
    )

    private val familyIncome = HistoryItem(
        id = "inc-fam",
        kind = MovementKind.INCOME,
        amountMinor = 100000L,
        occurredAt = epochOf(2026, 10, 5),
        createdAt = 80L,
        payingAccountId = "account-family",
        payingAccountName = "Familia",
        payingAccountType = AccountType.FAMILY,
        responsibleAccountId = "account-family",
        responsibleAccountName = "Familia",
        responsibleAccountType = AccountType.FAMILY,
        categoryId = "cat-salary",
        categoryName = "Salario",
        categoryIcon = "payments",
        note = "Aporte nómina",
        isAdvance = false,
    )

    private val reimbursementTransfer = HistoryItem(
        id = "tr-reimb",
        kind = MovementKind.REIMBURSEMENT,
        amountMinor = 3000L,
        occurredAt = epochOf(2026, 11, 20),
        createdAt = 110L,
        payingAccountId = "account-family",
        payingAccountName = "Familia",
        payingAccountType = AccountType.FAMILY,
        responsibleAccountId = "account-personal",
        responsibleAccountName = "Personal",
        responsibleAccountType = AccountType.PERSONAL,
        categoryId = null,
        categoryName = null,
        categoryIcon = null,
        note = "Devolución parcial",
        isAdvance = false,
    )

    private val allItems = listOf(reimbursementTransfer, advanceExpense, personalExpense, familyIncome)

    @Test
    fun `account filter for expenses uses responsible account, not paying account`() {
        val period = MovementPeriodFilter.Year(2026)

        // Familia es responsable de advanceExpense (pagado por Personal), recibe familyIncome y participa en reimbursementTransfer
        val familyFiltered = useCase(
            items = allItems,
            criteria = MovementFilterCriteria(
                accountFilter = MovementAccountFilter.Account("account-family"),
                periodFilter = period,
            ),
            zoneId = zoneId,
        )
        assertEquals(listOf("tr-reimb", "exp-adv", "inc-fam"), familyFiltered.map { it.id })

        // Personal NO es responsable de advanceExpense -> no aparece como gasto de Personal, solo personalExpense + reimbursementTransfer
        val personalFiltered = useCase(
            items = allItems,
            criteria = MovementFilterCriteria(
                accountFilter = MovementAccountFilter.Account("account-personal"),
                periodFilter = period,
            ),
            zoneId = zoneId,
        )
        assertEquals(listOf("tr-reimb", "exp-pers"), personalFiltered.map { it.id })
    }

    @Test
    fun `advances filter returns advance expenses as single non-duplicated entries`() {
        val advancesOnly = useCase(
            items = allItems,
            criteria = MovementFilterCriteria(
                typeFilter = MovementTypeFilter.ADVANCES,
                periodFilter = MovementPeriodFilter.Year(2026),
            ),
            zoneId = zoneId,
        )
        assertEquals(listOf("exp-adv"), advancesOnly.map { it.id })
    }

    @Test
    fun `period filters support month, last 12 months, year, and custom range`() {
        // Noviembre 2026: excluye familyIncome de octubre
        val novOnly = useCase(
            items = allItems,
            criteria = MovementFilterCriteria(
                periodFilter = MovementPeriodFilter.Month(YearMonth.of(2026, 11)),
            ),
            zoneId = zoneId,
        )
        assertEquals(listOf("tr-reimb", "exp-adv", "exp-pers"), novOnly.map { it.id })

        // Últimos 12 meses desde Nov 2026: incluye octubre y noviembre
        val last12 = useCase(
            items = allItems,
            criteria = MovementFilterCriteria(
                periodFilter = MovementPeriodFilter.Last12Months(LocalDate.of(2026, 11, 21)),
            ),
            zoneId = zoneId,
        )
        assertEquals(4, last12.size)

        // Personalizado 14-16 Nov 2026
        val custom = useCase(
            items = allItems,
            criteria = MovementFilterCriteria(
                periodFilter = MovementPeriodFilter.Custom(
                    LocalDate.of(2026, 11, 14),
                    LocalDate.of(2026, 11, 16),
                ),
            ),
            zoneId = zoneId,
        )
        assertEquals(listOf("exp-adv", "exp-pers"), custom.map { it.id })
    }

    @Test
    fun `search query and category filter narrow results accurately`() {
        val byCategory = useCase(
            items = allItems,
            criteria = MovementFilterCriteria(
                categoryId = "cat-groceries",
                periodFilter = MovementPeriodFilter.Year(2026),
            ),
            zoneId = zoneId,
        )
        assertEquals(listOf("exp-adv"), byCategory.map { it.id })

        val bySearch = useCase(
            items = allItems,
            criteria = MovementFilterCriteria(
                searchQuery = "metro",
                periodFilter = MovementPeriodFilter.Year(2026),
            ),
            zoneId = zoneId,
        )
        assertEquals(listOf("exp-pers"), bySearch.map { it.id })
    }
}
