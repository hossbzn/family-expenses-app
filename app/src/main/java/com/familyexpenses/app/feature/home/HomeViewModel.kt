package com.familyexpenses.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.data.local.dao.RecurrenceOccurrenceDao
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.domain.model.HistoryItem
import com.familyexpenses.app.domain.usecase.GetDashboardSummaryUseCase
import com.familyexpenses.app.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    getHistoryUseCase: GetHistoryUseCase,
    recurrenceOccurrenceDao: RecurrenceOccurrenceDao,
    private val expenseRepository: ExpenseRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        getDashboardSummaryUseCase(),
        recurrenceOccurrenceDao.observePendingOccurrences(),
        getHistoryUseCase(),
        userPreferencesRepository.observePrivacyModeEnabled(),
    ) { summary, pendingOccurrences, history, privacyEnabled ->
        HomeUiState(
            personalCashBalanceMinor = summary.personalCashBalanceMinor,
            familyCashBalanceMinor = summary.familyCashBalanceMinor,
            netDebtMinor = summary.netDebtMinor,
            pendingRecurrencesCount = pendingOccurrences.size,
            recentMovements = history.take(3),
            privacyModeEnabled = privacyEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    fun togglePrivacyMode() {
        val current = uiState.value.privacyModeEnabled
        userPreferencesRepository.setPrivacyModeEnabled(!current)
    }

    fun undoTransaction(transactionId: String) {
        viewModelScope.launch {
            expenseRepository.deleteTransaction(transactionId)
        }
    }
}

data class HomeUiState(
    val personalCashBalanceMinor: Long = 0L,
    val familyCashBalanceMinor: Long = 0L,
    val netDebtMinor: Long = 0L,
    val pendingRecurrencesCount: Int = 0,
    val recentMovements: List<HistoryItem> = emptyList(),
    val privacyModeEnabled: Boolean = false,
)
