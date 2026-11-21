package com.familyexpenses.app.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.domain.model.HistoryItem
import com.familyexpenses.app.domain.model.MovementKind
import com.familyexpenses.app.domain.usecase.FilterMovementsUseCase
import com.familyexpenses.app.domain.usecase.GetHistoryUseCase
import com.familyexpenses.app.domain.usecase.MovementAccountFilter
import com.familyexpenses.app.domain.usecase.MovementFilterCriteria
import com.familyexpenses.app.domain.usecase.MovementPeriodFilter
import com.familyexpenses.app.domain.usecase.MovementTypeFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    getHistoryUseCase: GetHistoryUseCase,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val filterMovementsUseCase: FilterMovementsUseCase,
) : ViewModel() {

    private val initialAccountId: String? = savedStateHandle.get<String>(INITIAL_ACCOUNT_ID_ARG)
        ?.takeIf { it.isNotBlank() }
    private val initialCategoryId: String? = savedStateHandle.get<String>(INITIAL_CATEGORY_ID_ARG)
        ?.takeIf { it.isNotBlank() }
    private val initialTypeFilterName: String? = savedStateHandle.get<String>(INITIAL_TYPE_FILTER_ARG)
        ?.takeIf { it.isNotBlank() }
    private val initialPeriodTypeName: String? = savedStateHandle.get<String>(INITIAL_PERIOD_TYPE_ARG)
        ?.takeIf { it.isNotBlank() }
    private val initialPeriodStart: String? = savedStateHandle.get<String>(INITIAL_PERIOD_START_ARG)
        ?.takeIf { it.isNotBlank() }
    private val initialPeriodEnd: String? = savedStateHandle.get<String>(INITIAL_PERIOD_END_ARG)
        ?.takeIf { it.isNotBlank() }

    private val filterState = MutableStateFlow(
        HistoryFilterState(
            criteria = MovementFilterCriteria(
                accountFilter = if (initialAccountId != null) {
                    MovementAccountFilter.Account(initialAccountId)
                } else {
                    MovementAccountFilter.All
                },
                typeFilter = initialTypeFilterName?.let { name ->
                    runCatching { MovementTypeFilter.valueOf(name) }.getOrNull()
                } ?: MovementTypeFilter.ALL,
                categoryId = initialCategoryId,
                periodFilter = resolveInitialPeriodFilter(
                    periodTypeName = initialPeriodTypeName,
                    startIso = initialPeriodStart,
                    endIso = initialPeriodEnd,
                ),
            ),
        ),
    )

    val uiState: StateFlow<HistoryUiState> = combine(
        getHistoryUseCase(),
        accountRepository.observeAccounts(),
        categoryRepository.observeCategories(),
        filterState,
    ) { items, accounts, categories, state ->
        val activeAccounts = accounts.filter { !it.isArchived }
        val filteredItems = filterMovementsUseCase(items, state.criteria)

        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val yesterday = today.minusDays(1)

        val groups = filteredItems
            .groupBy { item ->
                Instant.ofEpochMilli(item.occurredAt).atZone(zoneId).toLocalDate()
            }
            .entries
            .sortedByDescending { it.key }
            .map { (date, dayItems) ->
                MovementDayGroup(
                    date = date,
                    headerLabel = formatDayHeader(date, today, yesterday),
                    rows = dayItems.map { it.toRowUiModel() },
                )
            }

        val selectedCategory = state.criteria.categoryId?.let { id ->
            categories.firstOrNull { it.id == id }
        }

        HistoryUiState(
            accounts = activeAccounts,
            categories = categories,
            selectedCategoryName = selectedCategory?.name,
            criteria = state.criteria,
            periodLabel = formatPeriodLabel(state.criteria.periodFilter),
            dayGroups = groups,
            totalCount = filteredItems.size,
            isSearchExpanded = state.isSearchExpanded,
            isCategorySheetOpen = state.isCategorySheetOpen,
            isCustomDateRangeOpen = state.isCustomDateRangeOpen,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun onAccountFilterSelected(accountFilter: MovementAccountFilter) {
        filterState.update { it.copy(criteria = it.criteria.copy(accountFilter = accountFilter)) }
    }

    fun onTypeFilterSelected(typeFilter: MovementTypeFilter) {
        filterState.update { it.copy(criteria = it.criteria.copy(typeFilter = typeFilter)) }
    }

    fun onCategorySelected(categoryId: String?) {
        filterState.update {
            it.copy(
                criteria = it.criteria.copy(categoryId = categoryId),
                isCategorySheetOpen = false,
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        filterState.update { it.copy(criteria = it.criteria.copy(searchQuery = query)) }
    }

    fun onToggleSearch() {
        filterState.update { state ->
            val newExpanded = !state.isSearchExpanded
            state.copy(
                isSearchExpanded = newExpanded,
                criteria = if (!newExpanded) state.criteria.copy(searchQuery = "") else state.criteria,
            )
        }
    }

    fun onOpenCategorySheet() {
        filterState.update { it.copy(isCategorySheetOpen = true) }
    }

    fun onDismissCategorySheet() {
        filterState.update { it.copy(isCategorySheetOpen = false) }
    }

    fun onSelectMonthPeriod() {
        filterState.update {
            it.copy(criteria = it.criteria.copy(periodFilter = MovementPeriodFilter.Month(YearMonth.now())))
        }
    }

    fun onSelectLast12MonthsPeriod() {
        filterState.update {
            it.copy(criteria = it.criteria.copy(periodFilter = MovementPeriodFilter.Last12Months(LocalDate.now())))
        }
    }

    fun onSelectYearPeriod() {
        filterState.update {
            it.copy(criteria = it.criteria.copy(periodFilter = MovementPeriodFilter.Year(LocalDate.now().year)))
        }
    }

    fun onOpenCustomPeriod() {
        filterState.update { it.copy(isCustomDateRangeOpen = true) }
    }

    fun onDismissCustomPeriod() {
        filterState.update { it.copy(isCustomDateRangeOpen = false) }
    }

    fun onCustomPeriodSelected(startMillis: Long, endMillis: Long) {
        val zoneId = ZoneId.systemDefault()
        val startDate = Instant.ofEpochMilli(startMillis).atZone(zoneId).toLocalDate()
        val endDate = Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalDate()
        filterState.update {
            it.copy(
                criteria = it.criteria.copy(periodFilter = MovementPeriodFilter.Custom(startDate, endDate)),
                isCustomDateRangeOpen = false,
            )
        }
    }

    fun onPreviousPeriod() {
        filterState.update { state ->
            val updated = when (val current = state.criteria.periodFilter) {
                is MovementPeriodFilter.Month -> MovementPeriodFilter.Month(current.yearMonth.minusMonths(1))
                is MovementPeriodFilter.Year -> MovementPeriodFilter.Year(current.year - 1)
                is MovementPeriodFilter.Last12Months -> current
                is MovementPeriodFilter.Custom -> current
            }
            state.copy(criteria = state.criteria.copy(periodFilter = updated))
        }
    }

    fun onNextPeriod() {
        filterState.update { state ->
            val updated = when (val current = state.criteria.periodFilter) {
                is MovementPeriodFilter.Month -> MovementPeriodFilter.Month(current.yearMonth.plusMonths(1))
                is MovementPeriodFilter.Year -> MovementPeriodFilter.Year(current.year + 1)
                is MovementPeriodFilter.Last12Months -> current
                is MovementPeriodFilter.Custom -> current
            }
            state.copy(criteria = state.criteria.copy(periodFilter = updated))
        }
    }

    fun onResetFilters() {
        filterState.update {
            HistoryFilterState(
                criteria = MovementFilterCriteria(
                    periodFilter = MovementPeriodFilter.Month(YearMonth.now()),
                ),
            )
        }
    }

    private fun HistoryItem.toRowUiModel(): MovementRowUiModel {
        val line1Title = when (kind) {
            MovementKind.EXPENSE -> note?.takeIf { it.isNotBlank() } ?: categoryName ?: "Gasto"
            MovementKind.INCOME -> note?.takeIf { it.isNotBlank() } ?: categoryName ?: "Ingreso"
            MovementKind.NORMAL_TRANSFER -> note?.takeIf { it.isNotBlank() } ?: "Transferencia"
            MovementKind.REIMBURSEMENT -> note?.takeIf { it.isNotBlank() } ?: "Devolución de adelanto"
        }

        val line2Subtitle = when (kind) {
            MovementKind.EXPENSE -> {
                val catLabel = categoryName ?: "Sin categoría"
                if (isAdvance) {
                    "$catLabel · $payingAccountName → $responsibleAccountName (Adelanto)"
                } else {
                    "$catLabel · $responsibleAccountName"
                }
            }
            MovementKind.INCOME -> {
                val catLabel = categoryName ?: "Ingreso"
                "$catLabel · $responsibleAccountName"
            }
            MovementKind.NORMAL_TRANSFER -> {
                "$payingAccountName → $responsibleAccountName"
            }
            MovementKind.REIMBURSEMENT -> {
                "$payingAccountName → $responsibleAccountName (Devolución)"
            }
        }

        val amountText = when (kind) {
            MovementKind.EXPENSE -> formatCurrency(-amountMinor, showPlus = false)
            MovementKind.INCOME -> formatCurrency(amountMinor, showPlus = true)
            MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> formatCurrency(amountMinor, showPlus = false)
        }

        return MovementRowUiModel(
            id = id,
            kind = kind,
            line1Title = line1Title,
            line2Subtitle = line2Subtitle,
            amountText = amountText,
            categoryIcon = categoryIcon,
            isAdvance = isAdvance,
        )
    }

    private fun formatCurrency(minorAmount: Long, showPlus: Boolean): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "ES")).apply {
            currency = Currency.getInstance("EUR")
        }
        val formatted = formatter.format(minorAmount / 100.0)
        return if (showPlus && minorAmount > 0L) "+$formatted" else formatted
    }

    private fun formatDayHeader(date: LocalDate, today: LocalDate, yesterday: LocalDate): String = when (date) {
        today -> "Hoy"
        yesterday -> "Ayer"
        else -> {
            val pattern = if (date.year == today.year) "EEEE, d 'de' MMMM" else "d 'de' MMMM yyyy"
            date.format(DateTimeFormatter.ofPattern(pattern, Locale("es", "ES")))
                .replaceFirstChar { it.uppercase() }
        }
    }

    private fun formatPeriodLabel(periodFilter: MovementPeriodFilter): String = when (periodFilter) {
        is MovementPeriodFilter.Month -> {
            periodFilter.yearMonth
                .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES")))
                .replaceFirstChar { it.uppercase() }
        }
        is MovementPeriodFilter.Last12Months -> "Últimos 12 meses"
        is MovementPeriodFilter.Year -> "Año ${periodFilter.year}"
        is MovementPeriodFilter.Custom -> {
            val fmt = DateTimeFormatter.ofPattern("d MMM yy", Locale("es", "ES"))
            "${periodFilter.startDate.format(fmt)} – ${periodFilter.endDate.format(fmt)}"
        }
    }

    private fun resolveInitialPeriodFilter(
        periodTypeName: String?,
        startIso: String?,
        endIso: String?,
    ): MovementPeriodFilter {
        val parsedStart = startIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val parsedEnd = endIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return when (periodTypeName) {
            "MONTH" -> MovementPeriodFilter.Month(parsedStart?.let { YearMonth.from(it) } ?: YearMonth.now())
            "LAST_12_MONTHS" -> MovementPeriodFilter.Last12Months(parsedEnd ?: LocalDate.now())
            "YEAR" -> MovementPeriodFilter.Year(parsedStart?.year ?: LocalDate.now().year)
            "CUSTOM" -> if (parsedStart != null && parsedEnd != null) {
                MovementPeriodFilter.Custom(parsedStart, parsedEnd)
            } else {
                MovementPeriodFilter.Month(YearMonth.now())
            }
            else -> MovementPeriodFilter.Month(YearMonth.now())
        }
    }

    companion object {
        const val INITIAL_ACCOUNT_ID_ARG = "initialAccountId"
        const val INITIAL_CATEGORY_ID_ARG = "initialCategoryId"
        const val INITIAL_TYPE_FILTER_ARG = "initialTypeFilter"
        const val INITIAL_PERIOD_TYPE_ARG = "initialPeriodType"
        const val INITIAL_PERIOD_START_ARG = "initialPeriodStart"
        const val INITIAL_PERIOD_END_ARG = "initialPeriodEnd"
    }
}

private data class HistoryFilterState(
    val criteria: MovementFilterCriteria = MovementFilterCriteria(),
    val isSearchExpanded: Boolean = false,
    val isCategorySheetOpen: Boolean = false,
    val isCustomDateRangeOpen: Boolean = false,
)

data class HistoryUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryName: String? = null,
    val criteria: MovementFilterCriteria = MovementFilterCriteria(),
    val periodLabel: String = "",
    val dayGroups: List<MovementDayGroup> = emptyList(),
    val totalCount: Int = 0,
    val isSearchExpanded: Boolean = false,
    val isCategorySheetOpen: Boolean = false,
    val isCustomDateRangeOpen: Boolean = false,
)

data class MovementDayGroup(
    val date: LocalDate,
    val headerLabel: String,
    val rows: List<MovementRowUiModel>,
)

data class MovementRowUiModel(
    val id: String,
    val kind: MovementKind,
    val line1Title: String,
    val line2Subtitle: String,
    val amountText: String,
    val categoryIcon: String?,
    val isAdvance: Boolean,
)
