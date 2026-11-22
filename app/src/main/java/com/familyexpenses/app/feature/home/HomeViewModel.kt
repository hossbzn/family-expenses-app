package com.familyexpenses.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.data.local.dao.RecurrenceOccurrenceDao
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.domain.usecase.GetDashboardSummaryUseCase
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
    recurrenceOccurrenceDao: RecurrenceOccurrenceDao,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        getDashboardSummaryUseCase(),
        recurrenceOccurrenceDao.observePendingOccurrences(),
    ) { summary, pendingOccurrences ->
        HomeUiState(
            personalCashBalanceMinor = summary.personalCashBalanceMinor,
            familyCashBalanceMinor = summary.familyCashBalanceMinor,
            netDebtMinor = summary.netDebtMinor,
            pendingRecurrencesCount = pendingOccurrences.size,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

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
)
