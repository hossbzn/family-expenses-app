package com.familyexpenses.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.domain.usecase.GetDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = getDashboardSummaryUseCase()
        .map { summary ->
            HomeUiState(
                personalCashBalanceMinor = summary.personalCashBalanceMinor,
                familyCashBalanceMinor = summary.familyCashBalanceMinor,
                netDebtMinor = summary.netDebtMinor,
            )
        }
        .stateIn(
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
)
