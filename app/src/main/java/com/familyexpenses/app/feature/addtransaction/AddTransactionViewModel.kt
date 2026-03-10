package com.familyexpenses.app.feature.addtransaction

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.seed.DatabaseSeeder
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
    private val initialAccountId = savedStateHandle.get<String>(INITIAL_ACCOUNT_ID_ARG)
    private val initialType = savedStateHandle.get<String>(INITIAL_TYPE_ARG)
        ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
        ?: TransactionType.EXPENSE

    private val formState = MutableStateFlow(
        AddTransactionFormState(
            selectedType = initialType,
            selectedAccountId = initialAccountId,
            paidFromPersonal = preselectFamilyPaidFromPersonal,
        ),
    )

    private val events = MutableSharedFlow<AddTransactionEvent>()
    val uiEvents = events.asSharedFlow()

    val uiState: StateFlow<AddTransactionUiState> = combine(
        accountRepository.observeAccounts(),
        categoryRepository.observeCategories(),
        formState,
    ) { accounts, categories, form ->
        val selectedType = form.selectedType
        val filteredCategories = categories.filter { category ->
            when (selectedType) {
                TransactionType.EXPENSE -> category.type == CategoryType.EXPENSE
                TransactionType.INCOME -> category.type == CategoryType.INCOME
                TransactionType.ADJUSTMENT -> false
            }
        }
        val selectedAccountId = resolveSelectedAccountId(
            accounts = accounts,
            selectedAccountId = form.selectedAccountId,
            paidFromPersonal = form.paidFromPersonal && selectedType == TransactionType.EXPENSE,
        )
        val selectedCategoryId = form.selectedCategoryId
            ?.takeIf { selectedId -> filteredCategories.any { it.id == selectedId } }
            ?: filteredCategories.firstOrNull()?.id

        AddTransactionUiState(
            selectedType = selectedType,
            amountInput = form.amountInput,
            noteInput = form.noteInput,
            paidFromPersonal = form.paidFromPersonal && selectedType == TransactionType.EXPENSE,
            selectedAccountId = selectedAccountId,
            selectedCategoryId = selectedCategoryId,
            accountOptions = accounts.filter { !it.isArchived },
            categoryOptions = filteredCategories,
            isSaving = form.isSaving,
            errorMessage = form.errorMessage,
            title = if (preselectFamilyPaidFromPersonal) {
                "Gasto familiar pagado con personal"
            } else if (selectedType == TransactionType.INCOME) {
                "Anadir ingreso"
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

    fun onTypeSelected(type: TransactionType) {
        formState.update { state ->
            val paidFromPersonal = if (type == TransactionType.INCOME) false else state.paidFromPersonal
            state.copy(
                selectedType = type,
                paidFromPersonal = paidFromPersonal,
                selectedCategoryId = null,
                errorMessage = null,
            )
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
                paidFromPersonal = if (state.selectedType == TransactionType.EXPENSE) enabled else false,
                selectedAccountId = if (enabled) DatabaseSeeder.FAMILY_ACCOUNT_ID else state.selectedAccountId,
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
                        type = snapshot.selectedType,
                        amountMinor = amountMinor,
                        accountId = accountId,
                        categoryId = categoryId,
                        note = snapshot.noteInput,
                        paidFromPersonal = snapshot.selectedType == TransactionType.EXPENSE && snapshot.paidFromPersonal,
                        occurredAt = System.currentTimeMillis(),
                    ),
                )
            }.onSuccess {
                formState.update {
                    AddTransactionFormState(
                        selectedType = if (preselectFamilyPaidFromPersonal) {
                            TransactionType.EXPENSE
                        } else {
                            TransactionType.EXPENSE
                        },
                        paidFromPersonal = preselectFamilyPaidFromPersonal,
                    )
                }
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
            return accounts.firstOrNull { it.id == DatabaseSeeder.FAMILY_ACCOUNT_ID }?.id
                ?: accounts.firstOrNull { it.type == AccountType.FAMILY }?.id
        }

        return selectedAccountId
            ?: accounts.firstOrNull { it.id == DatabaseSeeder.PERSONAL_ACCOUNT_ID }?.id
            ?: accounts.firstOrNull { it.type == AccountType.PERSONAL }?.id
            ?: accounts.firstOrNull()?.id
    }

    companion object {
        const val FAMILY_PAID_FROM_PERSONAL_ARG = "familyPaidFromPersonal"
        const val INITIAL_ACCOUNT_ID_ARG = "initialAccountId"
        const val INITIAL_TYPE_ARG = "initialType"
    }
}

data class AddTransactionUiState(
    val title: String = "Anadir gasto",
    val selectedType: TransactionType = TransactionType.EXPENSE,
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
    val selectedType: TransactionType = TransactionType.EXPENSE,
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
