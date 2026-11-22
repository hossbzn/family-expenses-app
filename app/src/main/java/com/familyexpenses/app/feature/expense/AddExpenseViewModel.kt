package com.familyexpenses.app.feature.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.data.seed.DatabaseSeeder
import com.familyexpenses.app.domain.model.AddExpenseRequest
import com.familyexpenses.app.domain.usecase.AddExpenseUseCase
import com.familyexpenses.app.domain.usecase.ManageCategoriesUseCase
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
class AddExpenseViewModel @Inject constructor(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val manageCategoriesUseCase: ManageCategoriesUseCase,
) : ViewModel() {

    private val inputState = MutableStateFlow(
        AddExpenseFormState(
            payingAccountId = preferencesRepository.getLastExpensePayingAccountId()
                ?: DatabaseSeeder.PERSONAL_ACCOUNT_ID,
            responsibleAccountId = preferencesRepository.getLastExpenseResponsibleAccountId()
                ?: DatabaseSeeder.FAMILY_ACCOUNT_ID,
            selectedCategoryId = preferencesRepository.getLastExpenseCategoryId(),
        ),
    )

    private val events = MutableSharedFlow<AddExpenseEvent>()
    val uiEvents = events.asSharedFlow()

    val uiState: StateFlow<AddExpenseUiState> = combine(
        accountRepository.observeAccounts(),
        categoryRepository.observeFavoriteExpenseCategories(),
        categoryRepository.observeActiveCategoriesByType(CategoryType.EXPENSE),
        inputState,
    ) { accounts, favoriteCategories, allExpenseCategories, form ->
        val personalAccount = accounts.firstOrNull { it.type == AccountType.PERSONAL }
        val familyAccount = accounts.firstOrNull { it.type == AccountType.FAMILY }

        val activePayingId = form.payingAccountId.takeIf { id -> accounts.any { it.id == id } }
            ?: personalAccount?.id
            ?: ""
        val activeResponsibleId = form.responsibleAccountId.takeIf { id -> accounts.any { it.id == id } }
            ?: familyAccount?.id
            ?: ""

        val activeCategoryId = form.selectedCategoryId?.takeIf { id -> allExpenseCategories.any { it.id == id } }
            ?: favoriteCategories.firstOrNull()?.id
            ?: ""

        val amountMinor = NumericKeypadHandler.toMinorAmount(form.amountInput)

        val advanceNotice = if (activePayingId != activeResponsibleId) {
            val payingAcc = accounts.firstOrNull { it.id == activePayingId }
            val respAcc = accounts.firstOrNull { it.id == activeResponsibleId }
            if (payingAcc?.type == AccountType.PERSONAL && respAcc?.type == AccountType.FAMILY) {
                "Adelanto automático: Familia te deberá este importe"
            } else if (payingAcc?.type == AccountType.FAMILY && respAcc?.type == AccountType.PERSONAL) {
                "Adelanto automático: Deberás este importe a Familia"
            } else {
                "Adelanto automático: ${payingAcc?.name} adelanta a ${respAcc?.name}"
            }
        } else {
            null
        }

        AddExpenseUiState(
            amountInput = form.amountInput,
            amountMinor = amountMinor,
            displayAmount = NumericKeypadHandler.formatDisplay(form.amountInput),
            accounts = accounts.filter { !it.isArchived },
            payingAccountId = activePayingId,
            responsibleAccountId = activeResponsibleId,
            favoriteCategories = favoriteCategories.take(7),
            allExpenseCategories = allExpenseCategories,
            selectedCategoryId = activeCategoryId,
            advanceNotice = advanceNotice,
            noteInput = form.noteInput,
            occurredAt = form.occurredAt,
            isDetailsExpanded = form.isDetailsExpanded,
            isMoreCategoriesSheetOpen = form.isMoreCategoriesSheetOpen,
            isSaving = form.isSaving,
            showDiscardDialog = form.showDiscardDialog,
            errorMessage = form.errorMessage,
            isSaveEnabled = amountMinor > 0L && activeCategoryId.isNotBlank() && !form.isSaving,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddExpenseUiState(),
    )

    fun onDigit(digit: Char) {
        inputState.update { state ->
            val updated = NumericKeypadHandler.onDigit(state.amountInput, digit)
            state.copy(amountInput = updated, errorMessage = null)
        }
    }

    fun onComma() {
        inputState.update { state ->
            val updated = NumericKeypadHandler.onComma(state.amountInput)
            state.copy(amountInput = updated, errorMessage = null)
        }
    }

    fun onClear() {
        inputState.update { state ->
            state.copy(amountInput = NumericKeypadHandler.onClear(), errorMessage = null)
        }
    }

    fun onBackspace() {
        inputState.update { state ->
            val updated = NumericKeypadHandler.onBackspace(state.amountInput)
            state.copy(amountInput = updated, errorMessage = null)
        }
    }

    fun onCategorySelected(categoryId: String) {
        inputState.update { it.copy(selectedCategoryId = categoryId, isMoreCategoriesSheetOpen = false, errorMessage = null) }
    }

    fun onPayingAccountSelected(accountId: String) {
        inputState.update { it.copy(payingAccountId = accountId, errorMessage = null) }
    }

    fun onResponsibleAccountSelected(accountId: String) {
        inputState.update { it.copy(responsibleAccountId = accountId, errorMessage = null) }
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

    fun onOpenMoreCategories() {
        inputState.update { it.copy(isMoreCategoriesSheetOpen = true) }
    }

    fun onDismissMoreCategories() {
        inputState.update { it.copy(isMoreCategoriesSheetOpen = false) }
    }

    fun onCreateQuickExpenseCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching {
                manageCategoriesUseCase.createCategory(
                    name = trimmed,
                    icon = "category",
                    type = CategoryType.EXPENSE,
                )
            }.onSuccess { created ->
                inputState.update {
                    it.copy(
                        selectedCategoryId = created.id,
                        isMoreCategoriesSheetOpen = false,
                        errorMessage = null,
                    )
                }
            }.onFailure { err ->
                inputState.update {
                    it.copy(errorMessage = err.message ?: "No se pudo crear la categoría.")
                }
            }
        }
    }

    fun onBackClicked() {
        val state = uiState.value
        val hasChanges = state.amountMinor > 0L || state.noteInput.isNotBlank()
        if (hasChanges) {
            inputState.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch { events.emit(AddExpenseEvent.NavigateBack) }
        }
    }

    fun onConfirmDiscard() {
        inputState.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch { events.emit(AddExpenseEvent.NavigateBack) }
    }

    fun onDismissDiscardDialog() {
        inputState.update { it.copy(showDiscardDialog = false) }
    }

    fun saveExpense() {
        val snapshot = uiState.value
        if (!snapshot.isSaveEnabled) return

        viewModelScope.launch {
            inputState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                addExpenseUseCase(
                    AddExpenseRequest(
                        type = TransactionType.EXPENSE,
                        amountMinor = snapshot.amountMinor,
                        payingAccountId = snapshot.payingAccountId,
                        responsibleAccountId = snapshot.responsibleAccountId,
                        categoryId = snapshot.selectedCategoryId,
                        note = snapshot.noteInput.takeIf { it.isNotBlank() },
                        occurredAt = snapshot.occurredAt,
                    ),
                )
            }.onSuccess { result ->
                preferencesRepository.setLastExpensePayingAccountId(snapshot.payingAccountId)
                preferencesRepository.setLastExpenseResponsibleAccountId(snapshot.responsibleAccountId)
                preferencesRepository.setLastExpenseCategoryId(snapshot.selectedCategoryId)

                inputState.update { it.copy(isSaving = false) }
                events.emit(AddExpenseEvent.Saved(transactionId = result.transactionId))
            }.onFailure { error ->
                inputState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Error al guardar el gasto.",
                    )
                }
            }
        }
    }
}

private data class AddExpenseFormState(
    val amountInput: String = "0",
    val payingAccountId: String = "",
    val responsibleAccountId: String = "",
    val selectedCategoryId: String? = null,
    val noteInput: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isDetailsExpanded: Boolean = false,
    val isMoreCategoriesSheetOpen: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val errorMessage: String? = null,
)

data class AddExpenseUiState(
    val amountInput: String = "0",
    val amountMinor: Long = 0L,
    val displayAmount: String = "0 €",
    val accounts: List<AccountEntity> = emptyList(),
    val payingAccountId: String = "",
    val responsibleAccountId: String = "",
    val favoriteCategories: List<CategoryEntity> = emptyList(),
    val allExpenseCategories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String = "",
    val advanceNotice: String? = null,
    val noteInput: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isDetailsExpanded: Boolean = false,
    val isMoreCategoriesSheetOpen: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val errorMessage: String? = null,
    val isSaveEnabled: Boolean = false,
)

sealed interface AddExpenseEvent {
    data class Saved(val transactionId: String) : AddExpenseEvent
    data object NavigateBack : AddExpenseEvent
}
