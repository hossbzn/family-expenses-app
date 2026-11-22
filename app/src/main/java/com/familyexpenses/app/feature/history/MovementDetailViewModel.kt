package com.familyexpenses.app.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.TransferRepository
import com.familyexpenses.app.domain.model.MovementKind
import com.familyexpenses.app.domain.usecase.UpdateTransactionUseCase
import com.familyexpenses.app.domain.usecase.UpdateTransferUseCase
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

@HiltViewModel
class MovementDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    private val transferRepository: TransferRepository,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    private val updateTransferUseCase: UpdateTransferUseCase,
) : ViewModel() {

    private val movementKind: MovementKind = savedStateHandle.get<String>(MOVEMENT_KIND_ARG)
        ?.let { runCatching { MovementKind.valueOf(it) }.getOrNull() }
        ?: MovementKind.EXPENSE

    private val movementId: String = savedStateHandle.get<String>(MOVEMENT_ID_ARG) ?: ""

    private val formState = MutableStateFlow(MovementDetailFormState(kind = movementKind))
    private val events = MutableSharedFlow<MovementDetailEvent>()
    val uiEvents = events.asSharedFlow()

    init {
        loadMovement()
    }

    private fun loadMovement() {
        viewModelScope.launch {
            if (movementId.isBlank()) {
                formState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            when (movementKind) {
                MovementKind.EXPENSE, MovementKind.INCOME -> {
                    val tx = expenseRepository.getTransactionById(movementId)
                    if (tx == null) {
                        formState.update { it.copy(isLoading = false, notFound = true) }
                    } else {
                        val loadedKind = when (tx.type) {
                            TransactionType.EXPENSE -> MovementKind.EXPENSE
                            TransactionType.INCOME -> MovementKind.INCOME
                            TransactionType.ADJUSTMENT -> MovementKind.EXPENSE
                        }
                        formState.update {
                            it.copy(
                                isLoading = false,
                                notFound = false,
                                kind = loadedKind,
                                amountInput = NumericKeypadHandler.fromMinorAmount(tx.amountMinor),
                                payingAccountId = tx.payingAccountId,
                                responsibleAccountId = tx.responsibleAccountId,
                                categoryId = tx.categoryId,
                                noteInput = tx.note ?: "",
                                occurredAt = tx.occurredAt,
                            )
                        }
                    }
                }
                MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> {
                    val tr = transferRepository.getTransferById(movementId)
                    if (tr == null) {
                        formState.update { it.copy(isLoading = false, notFound = true) }
                    } else {
                        val loadedKind = when (tr.type) {
                            TransferType.NORMAL -> MovementKind.NORMAL_TRANSFER
                            TransferType.REIMBURSEMENT -> MovementKind.REIMBURSEMENT
                        }
                        formState.update {
                            it.copy(
                                isLoading = false,
                                notFound = false,
                                kind = loadedKind,
                                transferType = tr.type,
                                amountInput = NumericKeypadHandler.fromMinorAmount(tr.amountMinor),
                                payingAccountId = tr.sourceAccountId,
                                responsibleAccountId = tr.destinationAccountId,
                                noteInput = tr.note ?: "",
                                occurredAt = tr.occurredAt,
                            )
                        }
                    }
                }
            }
        }
    }

    val uiState: StateFlow<MovementDetailUiState> = combine(
        accountRepository.observeAccounts(),
        categoryRepository.observeCategories(),
        formState,
    ) { accounts, categories, form ->
        val activeAccounts = accounts.filter { !it.isArchived }
        val relevantCategories = when (form.kind) {
            MovementKind.EXPENSE -> categories.filter { it.type == CategoryType.EXPENSE }
            MovementKind.INCOME -> categories.filter { it.type == CategoryType.INCOME }
            MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> emptyList()
        }

        val amountMinor = NumericKeypadHandler.toMinorAmount(form.amountInput)
        val isAdvance = form.kind == MovementKind.EXPENSE &&
            form.payingAccountId.isNotBlank() &&
            form.responsibleAccountId.isNotBlank() &&
            form.payingAccountId != form.responsibleAccountId

        val isSaveEnabled = !form.isLoading && !form.notFound && !form.isSaving && amountMinor > 0L && when (form.kind) {
            MovementKind.EXPENSE ->
                form.payingAccountId.isNotBlank() &&
                    form.responsibleAccountId.isNotBlank() &&
                    form.categoryId.isNotBlank()
            MovementKind.INCOME ->
                form.payingAccountId.isNotBlank() && form.categoryId.isNotBlank()
            MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT ->
                form.payingAccountId.isNotBlank() &&
                    form.responsibleAccountId.isNotBlank() &&
                    form.payingAccountId != form.responsibleAccountId
        }

        MovementDetailUiState(
            isLoading = form.isLoading,
            notFound = form.notFound,
            kind = form.kind,
            transferType = form.transferType,
            amountInput = form.amountInput,
            amountMinor = amountMinor,
            accounts = activeAccounts,
            categories = relevantCategories,
            payingAccountId = form.payingAccountId,
            responsibleAccountId = form.responsibleAccountId,
            categoryId = form.categoryId,
            noteInput = form.noteInput,
            occurredAt = form.occurredAt,
            isAdvance = isAdvance,
            showDeleteDialog = form.showDeleteDialog,
            isSaving = form.isSaving,
            errorMessage = form.errorMessage,
            isSaveEnabled = isSaveEnabled,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MovementDetailUiState(kind = movementKind),
    )

    fun onAmountChanged(input: String) {
        val sanitized = sanitizeDecimalInput(input)
        formState.update { it.copy(amountInput = sanitized, errorMessage = null) }
    }

    fun onPayingAccountSelected(accountId: String) {
        formState.update { state ->
            when (state.kind) {
                MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> {
                    val other = uiState.value.accounts.firstOrNull { it.id != accountId }?.id ?: state.responsibleAccountId
                    state.copy(
                        payingAccountId = accountId,
                        responsibleAccountId = if (state.responsibleAccountId == accountId) other else state.responsibleAccountId,
                        errorMessage = null,
                    )
                }
                MovementKind.INCOME -> {
                    state.copy(
                        payingAccountId = accountId,
                        responsibleAccountId = accountId,
                        errorMessage = null,
                    )
                }
                MovementKind.EXPENSE -> {
                    state.copy(payingAccountId = accountId, errorMessage = null)
                }
            }
        }
    }

    fun onResponsibleAccountSelected(accountId: String) {
        formState.update { state ->
            when (state.kind) {
                MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> {
                    val other = uiState.value.accounts.firstOrNull { it.id != accountId }?.id ?: state.payingAccountId
                    state.copy(
                        responsibleAccountId = accountId,
                        payingAccountId = if (state.payingAccountId == accountId) other else state.payingAccountId,
                        errorMessage = null,
                    )
                }
                MovementKind.EXPENSE -> {
                    state.copy(responsibleAccountId = accountId, errorMessage = null)
                }
                MovementKind.INCOME -> {
                    state.copy(payingAccountId = accountId, responsibleAccountId = accountId, errorMessage = null)
                }
            }
        }
    }

    fun onTransferTypeSelected(type: TransferType) {
        formState.update { state ->
            state.copy(
                transferType = type,
                kind = if (type == TransferType.REIMBURSEMENT) MovementKind.REIMBURSEMENT else MovementKind.NORMAL_TRANSFER,
                errorMessage = null,
            )
        }
    }

    fun onCategorySelected(categoryId: String) {
        formState.update { it.copy(categoryId = categoryId, errorMessage = null) }
    }

    fun onNoteChanged(note: String) {
        formState.update { it.copy(noteInput = note, errorMessage = null) }
    }

    fun onDateSelected(timestamp: Long) {
        formState.update { it.copy(occurredAt = timestamp, errorMessage = null) }
    }

    fun saveChanges() {
        val snapshot = uiState.value
        if (!snapshot.isSaveEnabled) return

        viewModelScope.launch {
            formState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                when (snapshot.kind) {
                    MovementKind.EXPENSE, MovementKind.INCOME -> {
                        updateTransactionUseCase(
                            transactionId = movementId,
                            amountMinor = snapshot.amountMinor,
                            payingAccountId = snapshot.payingAccountId,
                            responsibleAccountId = snapshot.responsibleAccountId,
                            categoryId = snapshot.categoryId,
                            note = snapshot.noteInput,
                            occurredAt = snapshot.occurredAt,
                        )
                    }
                    MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> {
                        updateTransferUseCase(
                            transferId = movementId,
                            type = snapshot.transferType,
                            sourceAccountId = snapshot.payingAccountId,
                            destinationAccountId = snapshot.responsibleAccountId,
                            amountMinor = snapshot.amountMinor,
                            note = snapshot.noteInput,
                            occurredAt = snapshot.occurredAt,
                        )
                    }
                }
            }.onSuccess {
                formState.update { it.copy(isSaving = false) }
                events.emit(MovementDetailEvent.Saved)
            }.onFailure { error ->
                formState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Error al guardar cambios.",
                    )
                }
            }
        }
    }

    fun onDeleteClicked() {
        formState.update { it.copy(showDeleteDialog = true) }
    }

    fun onDismissDeleteDialog() {
        formState.update { it.copy(showDeleteDialog = false) }
    }

    fun onConfirmDelete() {
        viewModelScope.launch {
            formState.update { it.copy(showDeleteDialog = false, isSaving = true, errorMessage = null) }
            runCatching {
                when (movementKind) {
                    MovementKind.EXPENSE, MovementKind.INCOME ->
                        expenseRepository.deleteTransaction(movementId)
                    MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT ->
                        transferRepository.deleteTransfer(movementId)
                }
            }.onSuccess {
                formState.update { it.copy(isSaving = false) }
                events.emit(MovementDetailEvent.Deleted)
            }.onFailure { error ->
                formState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "No se pudo eliminar el movimiento.",
                    )
                }
            }
        }
    }

    private fun sanitizeDecimalInput(input: String): String {
        val sb = StringBuilder()
        var hasDecimal = false
        var decimalsCount = 0
        for (ch in input) {
            when {
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
                    if (sb.isEmpty()) sb.append('0')
                    sb.append(',')
                }
            }
        }
        return sb.toString()
    }

    companion object {
        const val MOVEMENT_KIND_ARG = "kind"
        const val MOVEMENT_ID_ARG = "movementId"
    }
}

private data class MovementDetailFormState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val kind: MovementKind = MovementKind.EXPENSE,
    val transferType: TransferType = TransferType.NORMAL,
    val amountInput: String = "",
    val payingAccountId: String = "",
    val responsibleAccountId: String = "",
    val categoryId: String = "",
    val noteInput: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val showDeleteDialog: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

data class MovementDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val kind: MovementKind = MovementKind.EXPENSE,
    val transferType: TransferType = TransferType.NORMAL,
    val amountInput: String = "",
    val amountMinor: Long = 0L,
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val payingAccountId: String = "",
    val responsibleAccountId: String = "",
    val categoryId: String = "",
    val noteInput: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isAdvance: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isSaveEnabled: Boolean = false,
)

sealed interface MovementDetailEvent {
    data object Saved : MovementDetailEvent
    data object Deleted : MovementDetailEvent
}
