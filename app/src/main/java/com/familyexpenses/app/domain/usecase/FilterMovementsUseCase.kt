package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.domain.model.HistoryItem
import com.familyexpenses.app.domain.model.MovementKind
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

sealed interface MovementAccountFilter {
    data object All : MovementAccountFilter
    data class Account(val accountId: String) : MovementAccountFilter
}

enum class MovementTypeFilter {
    ALL,
    EXPENSES,
    INCOMES,
    ADVANCES,
    TRANSFERS,
    REIMBURSEMENTS,
}

sealed interface MovementPeriodFilter {
    data class Month(val yearMonth: YearMonth) : MovementPeriodFilter
    data class Last12Months(val referenceDate: LocalDate = LocalDate.now()) : MovementPeriodFilter
    data class Year(val year: Int) : MovementPeriodFilter
    data class Custom(val startDate: LocalDate, val endDate: LocalDate) : MovementPeriodFilter
}

data class MovementFilterCriteria(
    val accountFilter: MovementAccountFilter = MovementAccountFilter.All,
    val typeFilter: MovementTypeFilter = MovementTypeFilter.ALL,
    val categoryId: String? = null,
    val searchQuery: String = "",
    val periodFilter: MovementPeriodFilter = MovementPeriodFilter.Month(YearMonth.now()),
)

class FilterMovementsUseCase @Inject constructor() {

    operator fun invoke(
        items: List<HistoryItem>,
        criteria: MovementFilterCriteria,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<HistoryItem> {
        val (startDate, endDate) = resolveDateRange(criteria.periodFilter)
        val trimmedQuery = criteria.searchQuery.trim()

        return items.filter { item ->
            matchesAccount(item, criteria.accountFilter) &&
                matchesType(item, criteria.typeFilter) &&
                matchesCategory(item, criteria.categoryId) &&
                matchesPeriod(item.occurredAt, startDate, endDate, zoneId) &&
                matchesSearch(item, trimmedQuery)
        }
    }

    private fun matchesAccount(
        item: HistoryItem,
        accountFilter: MovementAccountFilter,
    ): Boolean = when (accountFilter) {
        MovementAccountFilter.All -> true
        is MovementAccountFilter.Account -> when (item.kind) {
            // En gastos usa cuenta RESPONSABLE; en ingresos usa destinataria (que coincide con responsibleAccountId)
            MovementKind.EXPENSE, MovementKind.INCOME ->
                item.responsibleAccountId == accountFilter.accountId
            // En transferencias y devoluciones es visible si participa cualquiera de las dos cuentas
            MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT ->
                item.payingAccountId == accountFilter.accountId ||
                    item.responsibleAccountId == accountFilter.accountId
        }
    }

    private fun matchesType(
        item: HistoryItem,
        typeFilter: MovementTypeFilter,
    ): Boolean = when (typeFilter) {
        MovementTypeFilter.ALL -> true
        MovementTypeFilter.EXPENSES -> item.kind == MovementKind.EXPENSE
        MovementTypeFilter.INCOMES -> item.kind == MovementKind.INCOME
        MovementTypeFilter.ADVANCES -> item.kind == MovementKind.EXPENSE && item.isAdvance
        MovementTypeFilter.TRANSFERS -> item.kind == MovementKind.NORMAL_TRANSFER
        MovementTypeFilter.REIMBURSEMENTS -> item.kind == MovementKind.REIMBURSEMENT
    }

    private fun matchesCategory(
        item: HistoryItem,
        categoryId: String?,
    ): Boolean {
        if (categoryId.isNullOrBlank()) return true
        return item.categoryId == categoryId
    }

    private fun matchesPeriod(
        occurredAt: Long,
        startDate: LocalDate,
        endDate: LocalDate,
        zoneId: ZoneId,
    ): Boolean {
        val itemDate = Instant.ofEpochMilli(occurredAt).atZone(zoneId).toLocalDate()
        return !itemDate.isBefore(startDate) && !itemDate.isAfter(endDate)
    }

    private fun matchesSearch(
        item: HistoryItem,
        query: String,
    ): Boolean {
        if (query.isEmpty()) return true
        return (item.note?.contains(query, ignoreCase = true) == true) ||
            (item.categoryName?.contains(query, ignoreCase = true) == true) ||
            item.payingAccountName.contains(query, ignoreCase = true) ||
            item.responsibleAccountName.contains(query, ignoreCase = true)
    }

    fun resolveDateRange(periodFilter: MovementPeriodFilter): Pair<LocalDate, LocalDate> =
        when (periodFilter) {
            is MovementPeriodFilter.Month ->
                periodFilter.yearMonth.atDay(1) to periodFilter.yearMonth.atEndOfMonth()
            is MovementPeriodFilter.Last12Months -> {
                val startMonth = YearMonth.from(periodFilter.referenceDate).minusMonths(11)
                startMonth.atDay(1) to periodFilter.referenceDate
            }
            is MovementPeriodFilter.Year ->
                LocalDate.of(periodFilter.year, 1, 1) to LocalDate.of(periodFilter.year, 12, 31)
            is MovementPeriodFilter.Custom ->
                if (periodFilter.startDate.isAfter(periodFilter.endDate)) {
                    periodFilter.endDate to periodFilter.startDate
                } else {
                    periodFilter.startDate to periodFilter.endDate
                }
        }
}
