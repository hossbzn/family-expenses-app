package com.familyexpenses.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.domain.usecase.GetDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DashboardViewModel @Inject constructor(
    getDashboardSummaryUseCase: GetDashboardSummaryUseCase,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = getDashboardSummaryUseCase()
        .map { summary ->
            DashboardUiState(
                personalBalanceMinor = summary.personalBalanceMinor,
                familyBalanceMinor = summary.familyBalanceMinor,
                pendingReimbursementMinor = summary.pendingReimbursementMinor,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState(),
        )
}

data class DashboardUiState(
    val personalBalanceMinor: Long = 0L,
    val familyBalanceMinor: Long = 0L,
    val pendingReimbursementMinor: Long = 0L,
)
