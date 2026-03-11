package com.familyexpenses.app.feature.settlement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.domain.usecase.CreateFullSettlementUseCase
import com.familyexpenses.app.domain.usecase.GetPendingSettlementUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PendingSettlementViewModel @Inject constructor(
    getPendingSettlementUseCase: GetPendingSettlementUseCase,
    private val createFullSettlementUseCase: CreateFullSettlementUseCase,
) : ViewModel() {

    private val screenState = MutableStateFlow(PendingSettlementScreenState())
    private val events = MutableSharedFlow<PendingSettlementEvent>()
    val uiEvents = events.asSharedFlow()

    val uiState: StateFlow<PendingSettlementUiState> = combine(
        getPendingSettlementUseCase(),
        screenState,
    ) { summary, state ->
        PendingSettlementUiState(
            pendingAmountMinor = summary.pendingAmountMinor,
            pendingEntriesCount = summary.pendingEntriesCount,
            isSettling = state.isSettling,
            errorMessage = state.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = PendingSettlementUiState(),
    )

    fun settle() {
        if (uiState.value.pendingEntriesCount == 0) {
            screenState.update { it.copy(errorMessage = "No hay pendiente por liquidar.") }
            return
        }

        viewModelScope.launch {
            screenState.update { it.copy(isSettling = true, errorMessage = null) }
            runCatching {
                createFullSettlementUseCase()
            }.onSuccess {
                screenState.update { it.copy(isSettling = false, errorMessage = null) }
                events.emit(PendingSettlementEvent.Settled)
            }.onFailure { error ->
                screenState.update {
                    it.copy(
                        isSettling = false,
                        errorMessage = error.message ?: "No se pudo completar la liquidacion.",
                    )
                }
            }
        }
    }
}

data class PendingSettlementUiState(
    val pendingAmountMinor: Long = 0L,
    val pendingEntriesCount: Int = 0,
    val isSettling: Boolean = false,
    val errorMessage: String? = null,
)

private data class PendingSettlementScreenState(
    val isSettling: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PendingSettlementEvent {
    data object Settled : PendingSettlementEvent
}
