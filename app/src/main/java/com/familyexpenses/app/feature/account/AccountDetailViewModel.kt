package com.familyexpenses.app.feature.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.BalanceAdjustmentRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.TransferRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.data.seed.DatabaseSeeder
import com.familyexpenses.app.domain.usecase.ComputeAccountBalanceUseCase
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
import com.familyexpenses.app.domain.usecase.CreateBalanceAdjustmentUseCase
import com.familyexpenses.app.feature.expense.NumericKeypadHandler
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
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    expenseRepository: ExpenseRepository,
    private val transferRepository: TransferRepository,
    balanceAdjustmentRepository: BalanceAdjustmentRepository,
    userPreferencesRepository: UserPreferencesRepository,
    private val computeAccountBalanceUseCase: ComputeAccountBalanceUseCase,
    private val computeNetDebtUseCase: ComputeNetDebtUseCase,
    private val createBalanceAdjustmentUseCase: CreateBalanceAdjustmentUseCase,
) : ViewModel() {

    val accountId: String = savedStateHandle.get<String>(ACCOUNT_ID_ARG)
        ?: DatabaseSeeder.PERSONAL_ACCOUNT_ID

    private val dialogState = MutableStateFlow(AccountDetailDialogState())
    private val events = MutableSharedFlow<AccountDetailEvent>()
    val uiEvents = events.asSharedFlow()

    val uiState: StateFlow<AccountDetailUiState> = combine(
        accountRepository.observeAccounts(),
        expenseRepository.observeTransactions(),
        transferRepository.observeTransfers(),
        balanceAdjustmentRepository.observeAdjustments(),
        userPreferencesRepository.observePrivacyModeEnabled(),
        dialogState,
    ) { flowArray ->
        @Suppress("UNCHECKED_CAST")
        val accounts = flowArray[0] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val transactions = flowArray[1] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val transfers = flowArray[2] as List<TransferEntity>
        @Suppress("UNCHECKED_CAST")
        val adjustments = flowArray[3] as List<BalanceAdjustmentEntity>
        val privacyEnabled = flowArray[4] as Boolean
        val dialogs = flowArray[5] as AccountDetailDialogState

        val account = accounts.firstOrNull { it.id == accountId }
        val cashBalanceMinor = account?.let {
            computeAccountBalanceUseCase.compute(
                account = it,
                transactions = transactions,
                transfers = transfers,
                adjustments = adjustments,
            )
        } ?: 0L

        val netDebtMinor = computeNetDebtUseCase.compute(
            accounts = accounts,
            transactions = transactions,
            transfers = transfers,
        )

        AccountDetailUiState(
            account = account,
            cashBalanceMinor = cashBalanceMinor,
            netDebtMinor = netDebtMinor,
            isAdjustmentDialogOpen = dialogs.isAdjustmentDialogOpen,
            adjustmentIsPositive = dialogs.adjustmentIsPositive,
            adjustmentAmountInput = dialogs.adjustmentAmountInput,
            adjustmentReasonInput = dialogs.adjustmentReasonInput,
            adjustmentErrorMessage = dialogs.adjustmentErrorMessage,
            isInitialBalanceDialogOpen = dialogs.isInitialBalanceDialogOpen,
            initialBalanceInput = dialogs.initialBalanceInput,
            initialBalanceErrorMessage = dialogs.initialBalanceErrorMessage,
            isSaving = dialogs.isSaving,
            privacyModeEnabled = privacyEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountDetailUiState(),
    )

    fun openAdjustmentDialog() {
        dialogState.update {
            it.copy(
                isAdjustmentDialogOpen = true,
                adjustmentIsPositive = true,
                adjustmentAmountInput = "",
                adjustmentReasonInput = "",
                adjustmentErrorMessage = null,
            )
        }
    }

    fun dismissAdjustmentDialog() {
        dialogState.update { it.copy(isAdjustmentDialogOpen = false, adjustmentErrorMessage = null) }
    }

    fun onAdjustmentSignChanged(isPositive: Boolean) {
        dialogState.update { it.copy(adjustmentIsPositive = isPositive, adjustmentErrorMessage = null) }
    }

    fun onAdjustmentAmountChanged(input: String) {
        val filtered = sanitizeDecimalInput(input, allowNegative = false)
        dialogState.update { it.copy(adjustmentAmountInput = filtered, adjustmentErrorMessage = null) }
    }

    fun onAdjustmentReasonChanged(reason: String) {
        dialogState.update { it.copy(adjustmentReasonInput = reason, adjustmentErrorMessage = null) }
    }

    fun saveBalanceAdjustment() {
        val current = dialogState.value
        val minorAmount = NumericKeypadHandler.toMinorAmount(current.adjustmentAmountInput)
        if (minorAmount <= 0L) {
            dialogState.update { it.copy(adjustmentErrorMessage = "Introduce un importe mayor que cero.") }
            return
        }
        if (current.adjustmentReasonInput.isBlank()) {
            dialogState.update { it.copy(adjustmentErrorMessage = "El motivo es obligatorio para el ajuste.") }
            return
        }

        val signedDelta = if (current.adjustmentIsPositive) minorAmount else -minorAmount

        viewModelScope.launch {
            dialogState.update { it.copy(isSaving = true, adjustmentErrorMessage = null) }
            runCatching {
                createBalanceAdjustmentUseCase(
                    accountId = accountId,
                    amountDeltaMinor = signedDelta,
                    reason = current.adjustmentReasonInput,
                )
            }.onSuccess {
                dialogState.update {
                    it.copy(
                        isSaving = false,
                        isAdjustmentDialogOpen = false,
                        adjustmentAmountInput = "",
                        adjustmentReasonInput = "",
                    )
                }
                events.emit(AccountDetailEvent.Message("Ajuste de saldo registrado"))
            }.onFailure { error ->
                dialogState.update {
                    it.copy(
                        isSaving = false,
                        adjustmentErrorMessage = error.message ?: "No se pudo registrar el ajuste.",
                    )
                }
            }
        }
    }

    fun openInitialBalanceDialog() {
        val currentInitial = uiState.value.account?.initialBalanceMinor ?: 0L
        val formatted = if (currentInitial < 0L) {
            "-${NumericKeypadHandler.fromMinorAmount(currentInitial)}"
        } else {
            NumericKeypadHandler.fromMinorAmount(currentInitial)
        }
        dialogState.update {
            it.copy(
                isInitialBalanceDialogOpen = true,
                initialBalanceInput = formatted,
                initialBalanceErrorMessage = null,
            )
        }
    }

    fun dismissInitialBalanceDialog() {
        dialogState.update { it.copy(isInitialBalanceDialogOpen = false, initialBalanceErrorMessage = null) }
    }

    fun onInitialBalanceChanged(input: String) {
        val filtered = sanitizeDecimalInput(input, allowNegative = true)
        dialogState.update { it.copy(initialBalanceInput = filtered, initialBalanceErrorMessage = null) }
    }

    fun saveInitialBalance() {
        val raw = dialogState.value.initialBalanceInput.trim()
        if (raw.isEmpty()) {
            dialogState.update { it.copy(initialBalanceErrorMessage = "Introduce un saldo inicial válido.") }
            return
        }
        val isNegative = raw.startsWith("-")
        val cleanUnsigned = raw.removePrefix("-")
        if (cleanUnsigned.isEmpty()) {
            dialogState.update { it.copy(initialBalanceErrorMessage = "Introduce un saldo inicial válido.") }
            return
        }
        val unsignedMinor = NumericKeypadHandler.toMinorAmount(cleanUnsigned)
        val signedMinor = if (isNegative) -unsignedMinor else unsignedMinor

        viewModelScope.launch {
            dialogState.update { it.copy(isSaving = true, initialBalanceErrorMessage = null) }
            runCatching {
                accountRepository.updateInitialBalance(accountId, signedMinor)
            }.onSuccess {
                dialogState.update {
                    it.copy(
                        isSaving = false,
                        isInitialBalanceDialogOpen = false,
                    )
                }
                events.emit(AccountDetailEvent.Message("Saldo inicial actualizado"))
            }.onFailure { error ->
                dialogState.update {
                    it.copy(
                        isSaving = false,
                        initialBalanceErrorMessage = error.message ?: "Error al actualizar saldo inicial.",
                    )
                }
            }
        }
    }

    fun undoTransfer(transferId: String) {
        viewModelScope.launch {
            transferRepository.deleteTransfer(transferId)
        }
    }

    private fun sanitizeDecimalInput(input: String, allowNegative: Boolean): String {
        val sb = StringBuilder()
        var hasDecimal = false
        var decimalsCount = 0
        input.forEachIndexed { index, ch ->
            when {
                ch == '-' && allowNegative && index == 0 -> sb.append('-')
                ch in '0'..'9' -> {
                    if (!hasDecimal) {
                        sb.append(ch)
                    } else if (decimalsCount < 2) {
                        sb.append(ch)
                        decimalsCount++
                    }
                }
                (ch == ',' || ch == '.') && !hasDecimal -> {
                    hasDecimal = true
                    if (sb.isEmpty() || (sb.length == 1 && sb[0] == '-')) {
                        sb.append('0')
                    }
                    sb.append(',')
                }
            }
        }
        return sb.toString()
    }

    companion object {
        const val ACCOUNT_ID_ARG = "accountId"
    }
}

private data class AccountDetailDialogState(
    val isAdjustmentDialogOpen: Boolean = false,
    val adjustmentIsPositive: Boolean = true,
    val adjustmentAmountInput: String = "",
    val adjustmentReasonInput: String = "",
    val adjustmentErrorMessage: String? = null,
    val isInitialBalanceDialogOpen: Boolean = false,
    val initialBalanceInput: String = "",
    val initialBalanceErrorMessage: String? = null,
    val isSaving: Boolean = false,
)

data class AccountDetailUiState(
    val account: AccountEntity? = null,
    val cashBalanceMinor: Long = 0L,
    val netDebtMinor: Long = 0L,
    val isAdjustmentDialogOpen: Boolean = false,
    val adjustmentIsPositive: Boolean = true,
    val adjustmentAmountInput: String = "",
    val adjustmentReasonInput: String = "",
    val adjustmentErrorMessage: String? = null,
    val isInitialBalanceDialogOpen: Boolean = false,
    val initialBalanceInput: String = "",
    val initialBalanceErrorMessage: String? = null,
    val isSaving: Boolean = false,
    val privacyModeEnabled: Boolean = false,
)

sealed interface AccountDetailEvent {
    data class Message(val text: String) : AccountDetailEvent
}
