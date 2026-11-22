package com.familyexpenses.app.feature.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.data.seed.DatabaseSeeder
import com.familyexpenses.app.domain.model.AddIncomeRequest
import com.familyexpenses.app.domain.usecase.AddIncomeUseCase
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
class AddIncomeViewModel @Inject constructor(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val addIncomeUseCase: AddIncomeUseCase,
) : ViewModel() {

    private val inputState = MutableStateFlow(
        AddIncomeFormState(
            destinationAccountId = preferencesRepository.getLastIncomeAccountId()
                ?: DatabaseSeeder.PERSONAL_ACCOUNT_ID,
            selectedCategoryId = preferencesRepository.getLastIncomeCategoryId(),
        ),
    )

    private val events = MutableSharedFlow<AddIncomeEvent>()
    val uiEvents = events.asSharedFlow()

    val uiState: StateFlow<AddIncomeUiState> = combine(
        accountRepository.observeAccounts(),
        categoryRepository.observeActiveCategoriesByType(CategoryType.INCOME),
        inputState,
    ) { accounts, incomeCategories, form ->
        val personalAccount = accounts.firstOrNull { it.type == AccountType.PERSONAL }
        val activeDestinationId = form.destinationAccountId.takeIf { id -> accounts.any { it.id == id } }
            ?: personalAccount?.id
            ?: ""

        val activeCategoryId = form.selectedCategoryId?.takeIf { id -> incomeCategories.any { it.id == id } }
            ?: incomeCategories.firstOrNull()?.id
            ?: ""

        val amountMinor = NumericKeypadHandler.toMinorAmount(form.amountInput)

        AddIncomeUiState(
            amountInput = form.amountInput,
            amountMinor = amountMinor,
            displayAmount = NumericKeypadHandler.formatDisplay(form.amountInput),
            accounts = accounts.filter { !it.isArchived },
            destinationAccountId = activeDestinationId,
            incomeCategories = incomeCategories,
            selectedCategoryId = activeCategoryId,
            noteInput = form.noteInput,
            occurredAt = form.occurredAt,
            isDetailsExpanded = form.isDetailsExpanded,
            isSaving = form.isSaving,
            showDiscardDialog = form.showDiscardDialog,
            errorMessage = form.errorMessage,
            isSaveEnabled = amountMinor > 0L && activeCategoryId.isNotBlank() && !form.isSaving,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddIncomeUiState(),
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
        inputState.update { it.copy(selectedCategoryId = categoryId, errorMessage = null) }
    }

    fun onDestinationAccountSelected(accountId: String) {
        inputState.update { it.copy(destinationAccountId = accountId, errorMessage = null) }
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
        val state = uiState.value
        val hasChanges = state.amountMinor > 0L || state.noteInput.isNotBlank()
        if (hasChanges) {
            inputState.update { it.copy(showDiscardDialog = true) }
        } else {
            viewModelScope.launch { events.emit(AddIncomeEvent.NavigateBack) }
        }
    }

    fun onConfirmDiscard() {
        inputState.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch { events.emit(AddIncomeEvent.NavigateBack) }
    }

    fun onDismissDiscardDialog() {
        inputState.update { it.copy(showDiscardDialog = false) }
    }

    fun saveIncome() {
        val snapshot = uiState.value
        if (!snapshot.isSaveEnabled) return

        viewModelScope.launch {
            inputState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                addIncomeUseCase(
                    AddIncomeRequest(
                        amountMinor = snapshot.amountMinor,
                        destinationAccountId = snapshot.destinationAccountId,
                        categoryId = snapshot.selectedCategoryId,
                        note = snapshot.noteInput.takeIf { it.isNotBlank() },
                        occurredAt = snapshot.occurredAt,
                    ),
                )
            }.onSuccess { transactionId ->
                preferencesRepository.setLastIncomeAccountId(snapshot.destinationAccountId)
                preferencesRepository.setLastIncomeCategoryId(snapshot.selectedCategoryId)

                inputState.update { it.copy(isSaving = false) }
                events.emit(AddIncomeEvent.Saved(transactionId = transactionId))
            }.onFailure { error ->
                inputState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Error al guardar el ingreso.",
                    )
                }
            }
        }
    }
}

private data class AddIncomeFormState(
    val amountInput: String = "0",
    val destinationAccountId: String = "",
    val selectedCategoryId: String? = null,
    val noteInput: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isDetailsExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val errorMessage: String? = null,
)

data class AddIncomeUiState(
    val amountInput: String = "0",
    val amountMinor: Long = 0L,
    val displayAmount: String = "0 €",
    val accounts: List<AccountEntity> = emptyList(),
    val destinationAccountId: String = "",
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String = "",
    val noteInput: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val isDetailsExpanded: Boolean = false,
    val isSaving: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val errorMessage: String? = null,
    val isSaveEnabled: Boolean = false,
)

sealed interface AddIncomeEvent {
    data class Saved(val transactionId: String) : AddIncomeEvent
    data object NavigateBack : AddIncomeEvent
}
