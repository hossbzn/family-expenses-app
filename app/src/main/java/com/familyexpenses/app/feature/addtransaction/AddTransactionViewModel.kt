package com.familyexpenses.app.feature.addtransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.worker.DatabaseSeedWorker
import com.familyexpenses.app.domain.model.AddExpenseRequest
import com.familyexpenses.app.domain.usecase.AddExpenseUseCase
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
class AddTransactionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val addExpenseUseCase: AddExpenseUseCase,
) : ViewModel() {

    private val preselectFamilyPaidFromPersonal = savedStateHandle
        .get<Boolean>(FAMILY_PAID_FROM_PERSONAL_ARG) ?: false

    private val formState = MutableStateFlow(
        AddTransactionFormState(
            paidFromPersonal = preselectFamilyPaidFromPersonal,
        ),
    )

    private val events = MutableSharedFlow<AddTransactionEvent>()
    val uiEvents = events.asSharedFlow()

    val uiState: StateFlow<AddTransactionUiState> = combine(
        accountRepository.observeAccounts(),
        categoryRepository.observeExpenseCategories(),
        formState,
    ) { accounts, categories, form ->
        val selectedAccountId = resolveSelectedAccountId(
            accounts = accounts,
            selectedAccountId = form.selectedAccountId,
            paidFromPersonal = form.paidFromPersonal,
        )
        val selectedCategoryId = form.selectedCategoryId ?: categories.firstOrNull()?.id

        AddTransactionUiState(
            amountInput = form.amountInput,
            noteInput = form.noteInput,
            paidFromPersonal = form.paidFromPersonal,
            selectedAccountId = selectedAccountId,
            selectedCategoryId = selectedCategoryId,
            accountOptions = accounts.filter { !it.isArchived },
            categoryOptions = categories,
            isSaving = form.isSaving,
            errorMessage = form.errorMessage,
            title = if (preselectFamilyPaidFromPersonal) {
                "Gasto familiar pagado con personal"
            } else {
                "Anadir gasto"
            },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AddTransactionUiState(),
    )

    fun onAmountChanged(value: String) {
        formState.update { state ->
            state.copy(amountInput = value.filter { it.isDigit() || it == ',' || it == '.' }, errorMessage = null)
        }
    }

    fun onNoteChanged(value: String) {
        formState.update { state ->
            state.copy(noteInput = value, errorMessage = null)
        }
    }

    fun onCategorySelected(categoryId: String) {
        formState.update { state ->
            state.copy(selectedCategoryId = categoryId, errorMessage = null)
        }
    }

    fun onAccountSelected(accountId: String) {
        formState.update { state ->
            if (state.paidFromPersonal) {
                state
            } else {
                state.copy(selectedAccountId = accountId, errorMessage = null)
            }
        }
    }

    fun onPaidFromPersonalChanged(enabled: Boolean) {
        formState.update { state ->
            state.copy(
                paidFromPersonal = enabled,
                selectedAccountId = if (enabled) DatabaseSeedWorker.FAMILY_ACCOUNT_ID else state.selectedAccountId,
                errorMessage = null,
            )
        }
    }

    fun save() {
        val snapshot = uiState.value
        val amountMinor = snapshot.amountInput.toMinorAmountOrNull()
        if (amountMinor == null || amountMinor <= 0L) {
            formState.update { it.copy(errorMessage = "Introduce un importe valido mayor que cero.") }
            return
        }
        val categoryId = snapshot.selectedCategoryId
        if (categoryId.isNullOrBlank()) {
            formState.update { it.copy(errorMessage = "Selecciona una categoria.") }
            return
        }
        val accountId = snapshot.selectedAccountId
        if (accountId.isNullOrBlank()) {
            formState.update { it.copy(errorMessage = "Selecciona una cuenta.") }
            return
        }

        viewModelScope.launch {
            formState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                addExpenseUseCase(
                    AddExpenseRequest(
                        amountMinor = amountMinor,
                        accountId = accountId,
                        categoryId = categoryId,
                        note = snapshot.noteInput,
                        paidFromPersonal = snapshot.paidFromPersonal,
                        occurredAt = System.currentTimeMillis(),
                    ),
                )
            }.onSuccess {
                formState.update { AddTransactionFormState(paidFromPersonal = preselectFamilyPaidFromPersonal) }
                events.emit(AddTransactionEvent.Saved)
            }.onFailure { error ->
                formState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "No se pudo guardar el gasto.",
                    )
                }
            }
        }
    }

    private fun resolveSelectedAccountId(
        accounts: List<AccountEntity>,
        selectedAccountId: String?,
        paidFromPersonal: Boolean,
    ): String? {
        if (paidFromPersonal) {
            return accounts.firstOrNull { it.id == DatabaseSeedWorker.FAMILY_ACCOUNT_ID }?.id
                ?: accounts.firstOrNull { it.type == AccountType.FAMILY }?.id
        }

        return selectedAccountId
            ?: accounts.firstOrNull { it.id == DatabaseSeedWorker.PERSONAL_ACCOUNT_ID }?.id
            ?: accounts.firstOrNull { it.type == AccountType.PERSONAL }?.id
            ?: accounts.firstOrNull()?.id
    }

    companion object {
        const val FAMILY_PAID_FROM_PERSONAL_ARG = "familyPaidFromPersonal"
    }
}

data class AddTransactionUiState(
    val title: String = "Anadir gasto",
    val amountInput: String = "",
    val noteInput: String = "",
    val paidFromPersonal: Boolean = false,
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val accountOptions: List<AccountEntity> = emptyList(),
    val categoryOptions: List<CategoryEntity> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

private data class AddTransactionFormState(
    val amountInput: String = "",
    val noteInput: String = "",
    val paidFromPersonal: Boolean = false,
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface AddTransactionEvent {
    data object Saved : AddTransactionEvent
}

private fun String.toMinorAmountOrNull(): Long? {
    val normalized = replace(',', '.').trim()
    if (normalized.isBlank()) return null
    val decimal = normalized.toBigDecimalOrNull() ?: return null
    return decimal.movePointRight(2).longValueExact()
}
