package com.familyexpenses.app.feature.transfer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.TransferRepository
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
import com.familyexpenses.app.domain.usecase.ExecuteTransferUseCase
import com.familyexpenses.app.feature.expense.NumericKeypadHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

@HiltViewModel
class TransferViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountRepository: AccountRepository,
    expenseRepository: ExpenseRepository,
    transferRepository: TransferRepository,
    private val computeNetDebtUseCase: ComputeNetDebtUseCase,
    private val executeTransferUseCase: ExecuteTransferUseCase,
) : ViewModel() {

    private val initialAccountId: String? = savedStateHandle.get<String>(INITIAL_ACCOUNT_ID_ARG)
    private val initialType: TransferType? = savedStateHandle.get<String>(INITIAL_TYPE_ARG)
        ?.takeIf { it.isNotBlank() }
        ?.let { raw -> TransferType.entries.firstOrNull { it.name == raw } }

    private val inputState = MutableStateFlow(
        TransferFormState(
            selectedType = initialType,
            sourceAccountId = initialAccountId ?: "",
        ),
    )

    private val events = MutableSharedFlow<TransferEvent>()
    val uiEvents = events.asSharedFlow()

    val uiState: StateFlow<TransferUiState> = combine(
        accountRepository.observeAccounts(),
        expenseRepository.observeTransactions(),
        transferRepository.observeTransfers(),
        inputState,
    ) { allAccounts, transactions, transfers, form ->
        val accounts = allAccounts.filter { !it.isArchived }
        val personalAccount = accounts.firstOrNull { it.type == AccountType.PERSONAL }
        val familyAccount = accounts.firstOrNull { it.type == AccountType.FAMILY }

        val netDebtMinor = computeNetDebtUseCase.compute(
            accounts = allAccounts,
            transactions = transactions,
            transfers = transfers,
        )
        val pendingDebtMinor = abs(netDebtMinor)

        // Determinar cuentas origen y destino según el tipo de transferencia
        val (activeSourceId, activeDestinationId) = when (form.selectedType) {
            TransferType.REIMBURSEMENT -> {
                if (netDebtMinor > 0L && familyAccount != null && personalAccount != null) {
                    // Familia debe a Personal -> Familia devuelve a Personal
                    familyAccount.id to personalAccount.id
                } else if (netDebtMinor < 0L && personalAccount != null && familyAccount != null) {
                    // Personal debe a Familia -> Personal devuelve a Familia
                    personalAccount.id to familyAccount.id
                } else {
                    val src = form.sourceAccountId.takeIf { id -> accounts.any { it.id == id } }
                        ?: personalAccount?.id
                        ?: accounts.firstOrNull()?.id
                        ?: ""
                    val dst = accounts.firstOrNull { it.id != src }?.id ?: ""
                    src to dst
                }
            }
            TransferType.NORMAL, null -> {
                val src = form.sourceAccountId.takeIf { id -> accounts.any { it.id == id } }
                    ?: personalAccount?.id
                    ?: accounts.firstOrNull()?.id
                    ?: ""
                val dst = form.destinationAccountId.takeIf { id -> accounts.any { it.id == id } && id != src }
                    ?: accounts.firstOrNull { it.id != src }?.id
                    ?: ""
                src to dst
            }
        }

        val effectiveAmountInput = if (
            form.selectedType == TransferType.REIMBURSEMENT &&
            !form.hasEditedAmount &&
            pendingDebtMinor > 0L
        ) {
            NumericKeypadHandler.fromMinorAmount(pendingDebtMinor)
        } else {
            form.amountInput
        }
        val amountMinor = NumericKeypadHandler.toMinorAmount(effectiveAmountInput)

        val validationMessage = when (form.selectedType) {
            null -> null
            TransferType.REIMBURSEMENT -> when {
                pendingDebtMinor == 0L -> "No hay deuda pendiente que devolver."
                amountMinor > pendingDebtMinor -> "El importe supera la deuda pendiente (${formatCurrencyText(pendingDebtMinor)})."
                else -> null
            }
            TransferType.NORMAL -> null
        }

        val isSaveEnabled = when (form.selectedType) {
            null -> false
            TransferType.NORMAL ->
                amountMinor > 0L &&
                    activeSourceId.isNotBlank() &&
                    activeDestinationId.isNotBlank() &&
                    activeSourceId != activeDestinationId &&
                    !form.isSaving
            TransferType.REIMBURSEMENT ->
                pendingDebtMinor > 0L &&
                    amountMinor in 1L..pendingDebtMinor &&
                    activeSourceId.isNotBlank() &&
                    activeDestinationId.isNotBlank() &&
                    activeSourceId != activeDestinationId &&
                    !form.isSaving
        }

        TransferUiState(
            selectedType = form.selectedType,
            accounts = accounts,
            sourceAccountId = activeSourceId,
            destinationAccountId = activeDestinationId,
            amountInput = effectiveAmountInput,
            amountMinor = amountMinor,
            displayAmount = NumericKeypadHandler.formatDisplay(effectiveAmountInput),
            netDebtMinor = netDebtMinor,
            pendingDebtMinor = pendingDebtMinor,
            noteInput = form.noteInput,
            occurredAt = form.occurredAt,
            isDetailsExpanded = form.isDetailsExpanded,
            isSaving = form.isSaving,
            showDiscardDialog = form.showDiscardDialog,
            validationMessage = validationMessage,
            errorMessage = form.errorMessage,
            isSaveEnabled = isSaveEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransferUiState(),
    )

    fun onTypeSelected(type: TransferType) {
        val currentState = uiState.value
        inputState.update { form ->
            val newAmountInput = when (type) {
                TransferType.REIMBURSEMENT -> {
                    if (currentState.pendingDebtMinor > 0L) {
                        NumericKeypadHandler.fromMinorAmount(currentState.pendingDebtMinor)
                    } else {
                        "0"
                    }
                }
                TransferType.NORMAL -> {
                    if (form.selectedType == TransferType.REIMBURSEMENT && !form.hasEditedAmount) {
                        "0"
                    } else {
                        form.amountInput
                    }
                }
            }
            form.copy(
                selectedType = type,
                amountInput = newAmountInput,
                hasEditedAmount = false,
                errorMessage = null,
            )
        }
    }

    fun onSourceAccountSelected(accountId: String) {
        val snapshot = uiState.value
        if (snapshot.selectedType == TransferType.REIMBURSEMENT) return
        val otherAccount = snapshot.accounts.firstOrNull { it.id != accountId }?.id ?: ""
        inputState.update {
            it.copy(
                sourceAccountId = accountId,
                destinationAccountId = if (it.destinationAccountId == accountId || it.destinationAccountId.isBlank()) {
                    otherAccount
                } else {
                    it.destinationAccountId
                },
                errorMessage = null,
            )
        }
    }

    fun onSwapAccounts() {
        val snapshot = uiState.value
        if (snapshot.selectedType == TransferType.REIMBURSEMENT) return
        inputState.update {
            it.copy(
                sourceAccountId = snapshot.destinationAccountId,
                destinationAccountId = snapshot.sourceAccountId,
                errorMessage = null,
            )
        }
    }

    fun onDigit(digit: Char) {
        val prefillFallback = uiState.value.amountInput
        inputState.update { state ->
            val base = if (!state.hasEditedAmount && state.selectedType == TransferType.REIMBURSEMENT) {
                prefillFallback
            } else {
                state.amountInput
            }
            val updated = NumericKeypadHandler.onDigit(base, digit)
            state.copy(amountInput = updated, hasEditedAmount = true, errorMessage = null)
        }
    }

    fun onComma() {
        val prefillFallback = uiState.value.amountInput
        inputState.update { state ->
            val base = if (!state.hasEditedAmount && state.selectedType == TransferType.REIMBURSEMENT) {
                prefillFallback
            } else {
                state.amountInput
            }
            val updated = NumericKeypadHandler.onComma(base)
            state.copy(amountInput = updated, hasEditedAmount = true, errorMessage = null)
        }
    }

    fun onClear() {
        inputState.update { state ->
            state.copy(
                amountInput = NumericKeypadHandler.onClear(),
                hasEditedAmount = true,
                errorMessage = null,
            )
        }
    }

    fun onBackspace() {
        val prefillFallback = uiState.value.amountInput
        inputState.update { state ->
            val base = if (!state.hasEditedAmount && state.selectedType == TransferType.REIMBURSEMENT) {
                prefillFallback
            } else {
                state.amountInput
            }
            val updated = NumericKeypadHandler.onBackspace(base)
            state.copy(amountInput = updated, hasEditedAmount = true, errorMessage = null)
        }
    }

    fun onNoteChanged(note: String) {
        inputState.update { it.copy(noteInput = note, errorMessage = null) }
    }

    fun onDateSelected(timestamp: Long) {
        inputState.update { it.copy(occurredAt = timestamp) }
    }

    fun onToggleDetails() {
        inputState.update { it.copy(isDetailsExpanded = !it.isDetailsExpanded) }
    }

    fun onBackClicked() {
        val form = inputState.value
        val snapshot = uiState.value
        val hasChanges = (form.selectedType == TransferType.NORMAL && snapshot.amountMinor > 0L) ||
            form.hasEditedAmount ||
            form.noteInput.isNotBlank()
        if (hasChanges) {
            inputState.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch { events.emit(TransferEvent.NavigateBack) }
        }
    }

    fun onConfirmDiscard() {
        inputState.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch { events.emit(TransferEvent.NavigateBack) }
    }

    fun onDismissDiscardDialog() {
        inputState.update { it.copy(showDiscardDialog = false) }
    }

    fun saveTransfer() {
        val snapshot = uiState.value
        val selectedType = snapshot.selectedType ?: return
        if (!snapshot.isSaveEnabled) return

        viewModelScope.launch {
            inputState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                executeTransferUseCase(
                    type = selectedType,
                    sourceAccountId = snapshot.sourceAccountId,
                    destinationAccountId = snapshot.destinationAccountId,
                    amountMinor = snapshot.amountMinor,
                    note = snapshot.noteInput.takeIf { it.isNotBlank() },
                    occurredAt = snapshot.occurredAt,
                )
            }.onSuccess { transferId ->
                inputState.update { it.copy(isSaving = false) }
                events.emit(TransferEvent.Saved(transferId = transferId))
            }.onFailure { error ->
                inputState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Error al guardar la transferencia.",
                    )
                }
            }
        }
    }

    private fun formatCurrencyText(minor: Long): String {
        val whole = minor / 100L
        val cents = minor % 100L
        return if (cents == 0L) "$whole €" else "$whole,${cents.toString().padStart(2, '0')} €"
    }

    companion object {
        const val INITIAL_ACCOUNT_ID_ARG = "accountId"
        const val INITIAL_TYPE_ARG = "initialType"
    }
}

private data class TransferFormState(
    val selectedType: TransferType? = null,
    val sourceAccountId: String = "",
    val destinationAccountId: String = "",
    val amountInput: String = "0",
    val hasEditedAmount: Boolean = false,
    val noteInput: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isDetailsExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val errorMessage: String? = null,
)

data class TransferUiState(
    val selectedType: TransferType? = null,
    val accounts: List<AccountEntity> = emptyList(),
    val sourceAccountId: String = "",
    val destinationAccountId: String = "",
    val amountInput: String = "0",
    val amountMinor: Long = 0L,
    val displayAmount: String = "0 €",
    val netDebtMinor: Long = 0L,
    val pendingDebtMinor: Long = 0L,
    val noteInput: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isDetailsExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val validationMessage: String? = null,
    val errorMessage: String? = null,
    val isSaveEnabled: Boolean = false,
)

sealed interface TransferEvent {
    data class Saved(val transferId: String) : TransferEvent
    data object NavigateBack : TransferEvent
}
