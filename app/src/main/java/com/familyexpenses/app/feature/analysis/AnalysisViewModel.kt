package com.familyexpenses.app.feature.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.domain.usecase.AnalysisCategorySlice
import com.familyexpenses.app.domain.usecase.AnalysisEvolutionBucket
import com.familyexpenses.app.domain.usecase.AnalysisSeriesType
import com.familyexpenses.app.domain.usecase.ComputeAnalysisUseCase
import com.familyexpenses.app.domain.usecase.MovementAccountFilter
import com.familyexpenses.app.domain.usecase.MovementPeriodFilter
import com.familyexpenses.app.domain.usecase.MovementTypeFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class AnalysisTab {
    CATEGORIES,
    EVOLUTION,
}

data class AnalysisDrillDownRequest(
    val accountId: String,
    val typeFilterName: String,
    val categoryId: String,
    val periodTypeName: String,
    val periodStartIso: String,
    val periodEndIso: String,
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    accountRepository: AccountRepository,
    expenseRepository: ExpenseRepository,
    categoryRepository: CategoryRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val computeAnalysisUseCase: ComputeAnalysisUseCase,
) : ViewModel() {

    private val filterState = MutableStateFlow(AnalysisFilterState())

    val uiState: StateFlow<AnalysisUiState> = combine(
        accountRepository.observeAccounts(),
        expenseRepository.observeTransactions(),
        categoryRepository.observeCategories(),
        userPreferencesRepository.observePrivacyModeEnabled(),
        filterState,
    ) { accounts, transactions, categories, privacyEnabled, state ->
        val activeAccounts = accounts.filter { !it.isArchived }
        val result = computeAnalysisUseCase.compute(
            transactions = transactions,
            categories = categories,
            accountFilter = state.accountFilter,
            periodFilter = state.periodFilter,
            seriesType = state.seriesType,
        )

        AnalysisUiState(
            selectedTab = state.selectedTab,
            accounts = activeAccounts,
            accountFilter = state.accountFilter,
            periodFilter = state.periodFilter,
            periodLabel = formatPeriodLabel(state.periodFilter),
            seriesType = state.seriesType,
            totalMinor = result.totalMinor,
            averagePerBucketMinor = result.averagePerBucketMinor,
            transactionCount = result.transactionCount,
            categorySlices = result.categorySlices,
            evolutionBuckets = result.evolutionBuckets,
            isCustomDateRangeOpen = state.isCustomDateRangeOpen,
            privacyModeEnabled = privacyEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalysisUiState(),
    )

    fun onTabSelected(tab: AnalysisTab) {
        filterState.update { it.copy(selectedTab = tab) }
    }

    fun onAccountFilterSelected(accountFilter: MovementAccountFilter) {
        filterState.update { it.copy(accountFilter = accountFilter) }
    }

    fun onSeriesTypeSelected(seriesType: AnalysisSeriesType) {
        filterState.update { it.copy(seriesType = seriesType) }
    }

    fun onSelectMonthPeriod() {
        filterState.update { it.copy(periodFilter = MovementPeriodFilter.Month(YearMonth.now())) }
    }

    fun onSelectLast12MonthsPeriod() {
        filterState.update { it.copy(periodFilter = MovementPeriodFilter.Last12Months(LocalDate.now())) }
    }

    fun onSelectYearPeriod() {
        filterState.update { it.copy(periodFilter = MovementPeriodFilter.Year(LocalDate.now().year)) }
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
                periodFilter = MovementPeriodFilter.Custom(startDate, endDate),
                isCustomDateRangeOpen = false,
            )
        }
    }

    fun onPreviousPeriod() {
        filterState.update { state ->
            val updated = when (val current = state.periodFilter) {
                is MovementPeriodFilter.Month -> MovementPeriodFilter.Month(current.yearMonth.minusMonths(1))
                is MovementPeriodFilter.Year -> MovementPeriodFilter.Year(current.year - 1)
                is MovementPeriodFilter.Last12Months -> current
                is MovementPeriodFilter.Custom -> current
            }
            state.copy(periodFilter = updated)
        }
    }

    fun onNextPeriod() {
        filterState.update { state ->
            val updated = when (val current = state.periodFilter) {
                is MovementPeriodFilter.Month -> MovementPeriodFilter.Month(current.yearMonth.plusMonths(1))
                is MovementPeriodFilter.Year -> MovementPeriodFilter.Year(current.year + 1)
                is MovementPeriodFilter.Last12Months -> current
                is MovementPeriodFilter.Custom -> current
            }
            state.copy(periodFilter = updated)
        }
    }

    fun buildCategoryDrillDown(categoryId: String): AnalysisDrillDownRequest {
        val state = filterState.value
        val (periodType, startIso, endIso) = encodePeriod(state.periodFilter)
        return AnalysisDrillDownRequest(
            accountId = encodeAccountId(state.accountFilter),
            typeFilterName = encodeTypeFilter(state.seriesType),
            categoryId = categoryId,
            periodTypeName = periodType,
            periodStartIso = startIso,
            periodEndIso = endIso,
        )
    }

    fun buildBucketDrillDown(bucket: AnalysisEvolutionBucket): AnalysisDrillDownRequest {
        val state = filterState.value
        val startYm = YearMonth.from(bucket.startDate)
        val isFullMonth = bucket.startDate.dayOfMonth == 1 &&
            bucket.endDate == startYm.atEndOfMonth()

        val (periodType, startIso, endIso) = if (isFullMonth) {
            Triple("MONTH", bucket.startDate.toString(), bucket.endDate.toString())
        } else {
            Triple("CUSTOM", bucket.startDate.toString(), bucket.endDate.toString())
        }

        return AnalysisDrillDownRequest(
            accountId = encodeAccountId(state.accountFilter),
            typeFilterName = encodeTypeFilter(state.seriesType),
            categoryId = "",
            periodTypeName = periodType,
            periodStartIso = startIso,
            periodEndIso = endIso,
        )
    }

    private fun encodeAccountId(accountFilter: MovementAccountFilter): String = when (accountFilter) {
        MovementAccountFilter.All -> ""
        is MovementAccountFilter.Account -> accountFilter.accountId
    }

    private fun encodeTypeFilter(seriesType: AnalysisSeriesType): String = when (seriesType) {
        AnalysisSeriesType.EXPENSES -> MovementTypeFilter.EXPENSES.name
        AnalysisSeriesType.INCOMES -> MovementTypeFilter.INCOMES.name
    }

    private fun encodePeriod(periodFilter: MovementPeriodFilter): Triple<String, String, String> =
        when (periodFilter) {
            is MovementPeriodFilter.Month -> Triple(
                "MONTH",
                periodFilter.yearMonth.atDay(1).toString(),
                periodFilter.yearMonth.atEndOfMonth().toString(),
            )
            is MovementPeriodFilter.Last12Months -> Triple(
                "LAST_12_MONTHS",
                YearMonth.from(periodFilter.referenceDate).minusMonths(11).atDay(1).toString(),
                periodFilter.referenceDate.toString(),
            )
            is MovementPeriodFilter.Year -> Triple(
                "YEAR",
                LocalDate.of(periodFilter.year, 1, 1).toString(),
                LocalDate.of(periodFilter.year, 12, 31).toString(),
            )
            is MovementPeriodFilter.Custom -> Triple(
                "CUSTOM",
                periodFilter.startDate.toString(),
                periodFilter.endDate.toString(),
            )
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
}

private data class AnalysisFilterState(
    val selectedTab: AnalysisTab = AnalysisTab.CATEGORIES,
    val accountFilter: MovementAccountFilter = MovementAccountFilter.All,
    val periodFilter: MovementPeriodFilter = MovementPeriodFilter.Month(YearMonth.now()),
    val seriesType: AnalysisSeriesType = AnalysisSeriesType.EXPENSES,
    val isCustomDateRangeOpen: Boolean = false,
)

data class AnalysisUiState(
    val selectedTab: AnalysisTab = AnalysisTab.CATEGORIES,
    val accounts: List<AccountEntity> = emptyList(),
    val accountFilter: MovementAccountFilter = MovementAccountFilter.All,
    val periodFilter: MovementPeriodFilter = MovementPeriodFilter.Month(YearMonth.now()),
    val periodLabel: String = "",
    val seriesType: AnalysisSeriesType = AnalysisSeriesType.EXPENSES,
    val totalMinor: Long = 0L,
    val averagePerBucketMinor: Long = 0L,
    val transactionCount: Int = 0,
    val categorySlices: List<AnalysisCategorySlice> = emptyList(),
    val evolutionBuckets: List<AnalysisEvolutionBucket> = emptyList(),
    val isCustomDateRangeOpen: Boolean = false,
    val privacyModeEnabled: Boolean = false,
)
