package com.familyexpenses.app.feature.recurrence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.seed.DatabaseSeeder
import com.familyexpenses.app.domain.model.RecurringRuleDraft
import com.familyexpenses.app.domain.usecase.CreateMonthlyRecurringRuleUseCase
import com.familyexpenses.app.domain.usecase.GetRecurringRulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RecurrenceViewModel @Inject constructor(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    getRecurringRulesUseCase: GetRecurringRulesUseCase,
    private val createMonthlyRecurringRuleUseCase: CreateMonthlyRecurringRuleUseCase,
) : ViewModel() {

    private val formState = MutableStateFlow(RecurrenceFormState())

    val uiState: StateFlow<RecurrenceUiState> = combine(
        accountRepository.observeAccounts(),
        categoryRepository.observeCategories(),
        getRecurringRulesUseCase(),
        formState,
    ) { accounts, categories, rules, form ->
        val filteredCategories = categories.filter { category ->
            when (form.selectedType) {
                TransactionType.EXPENSE -> category.type == CategoryType.EXPENSE
                TransactionType.INCOME -> category.type == CategoryType.INCOME
                TransactionType.ADJUSTMENT -> false
            }
        }
        val selectedAccountId = resolveSelectedAccountId(accounts, form)
        val selectedCategoryId = form.selectedCategoryId
            ?.takeIf { selected -> filteredCategories.any { it.id == selected } }
            ?: filteredCategories.firstOrNull()?.id

        RecurrenceUiState(
            selectedType = form.selectedType,
            selectedAccountId = selectedAccountId,
            selectedCategoryId = selectedCategoryId,
            amountInput = form.amountInput,
            noteInput = form.noteInput,
            startDateInput = form.startDateInput,
            endDateInput = form.endDateInput,
            paidFromPersonal = form.selectedType == TransactionType.EXPENSE && form.paidFromPersonal,
            accountOptions = accounts.filter { !it.isArchived },
            categoryOptions = filteredCategories,
            rules = rules,
            isSaving = form.isSaving,
            errorMessage = form.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecurrenceUiState(),
    )

    fun onTypeSelected(type: TransactionType) {
        formState.update {
            it.copy(
                selectedType = type,
                paidFromPersonal = if (type == TransactionType.EXPENSE) it.paidFromPersonal else false,
                selectedCategoryId = null,
                errorMessage = null,
            )
        }
    }

    fun onAccountSelected(accountId: String) {
        formState.update {
            if (it.paidFromPersonal) it else it.copy(selectedAccountId = accountId, errorMessage = null)
        }
    }

    fun onCategorySelected(categoryId: String) {
        formState.update { it.copy(selectedCategoryId = categoryId, errorMessage = null) }
    }

    fun onAmountChanged(value: String) {
        formState.update { it.copy(amountInput = value.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }, errorMessage = null) }
    }

    fun onNoteChanged(value: String) {
        formState.update { it.copy(noteInput = value, errorMessage = null) }
    }

    fun onStartDateChanged(value: String) {
        formState.update { it.copy(startDateInput = value.filterDateInput(), errorMessage = null) }
    }

    fun onEndDateChanged(value: String) {
        formState.update { it.copy(endDateInput = value.filterDateInput(), errorMessage = null) }
    }

    fun onPaidFromPersonalChanged(enabled: Boolean) {
        formState.update {
            it.copy(
                paidFromPersonal = enabled && it.selectedType == TransactionType.EXPENSE,
                selectedAccountId = if (enabled) DatabaseSeeder.FAMILY_ACCOUNT_ID else it.selectedAccountId,
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
        val accountId = snapshot.selectedAccountId
        if (accountId.isNullOrBlank()) {
            formState.update { it.copy(errorMessage = "Selecciona una cuenta.") }
            return
        }
        val categoryId = snapshot.selectedCategoryId
        if (categoryId.isNullOrBlank()) {
            formState.update { it.copy(errorMessage = "Selecciona una categoria.") }
            return
        }
        val startAt = snapshot.startDateInput.toEpochMillisAtStartOfDayOrNull()
        if (startAt == null) {
            formState.update { it.copy(errorMessage = "La fecha inicio debe tener formato AAAA-MM-DD.") }
            return
        }
        val endAt = if (snapshot.endDateInput.isBlank()) {
            null
        } else {
            snapshot.endDateInput.toEpochMillisAtStartOfDayOrNull() ?: run {
                formState.update { it.copy(errorMessage = "La fecha fin debe tener formato AAAA-MM-DD.") }
                return
            }
        }

        viewModelScope.launch {
            formState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                createMonthlyRecurringRuleUseCase(
                    RecurringRuleDraft(
                        type = snapshot.selectedType,
                        accountId = accountId,
                        categoryId = categoryId,
                        amountMinor = amountMinor,
                        paidFromPersonal = snapshot.paidFromPersonal,
                        note = snapshot.noteInput,
                        startAt = startAt,
                        endAt = endAt,
                    ),
                )
            }.onSuccess {
                formState.value = RecurrenceFormState(selectedType = snapshot.selectedType)
            }.onFailure { error ->
                formState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "No se pudo guardar la regla recurrente.",
                    )
                }
            }
        }
    }

    private fun resolveSelectedAccountId(accounts: List<AccountEntity>, form: RecurrenceFormState): String? {
        if (form.paidFromPersonal && form.selectedType == TransactionType.EXPENSE) {
            return accounts.firstOrNull { it.id == DatabaseSeeder.FAMILY_ACCOUNT_ID }?.id
                ?: accounts.firstOrNull { it.type == AccountType.FAMILY }?.id
        }
        return form.selectedAccountId
            ?: accounts.firstOrNull { it.id == DatabaseSeeder.PERSONAL_ACCOUNT_ID }?.id
            ?: accounts.firstOrNull { it.type == AccountType.PERSONAL }?.id
            ?: accounts.firstOrNull()?.id
    }
}

data class RecurrenceUiState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val amountInput: String = "",
    val noteInput: String = "",
    val startDateInput: String = defaultStartDateInput(),
    val endDateInput: String = "",
    val paidFromPersonal: Boolean = false,
    val accountOptions: List<AccountEntity> = emptyList(),
    val categoryOptions: List<CategoryEntity> = emptyList(),
    val rules: List<RecurrenceRuleEntity> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

private data class RecurrenceFormState(
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val amountInput: String = "",
    val noteInput: String = "",
    val startDateInput: String = defaultStartDateInput(),
    val endDateInput: String = "",
    val paidFromPersonal: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

private fun String.toMinorAmountOrNull(): Long? {
    val normalized = replace(',', '.').trim()
    if (normalized.isBlank()) return null
    val decimal = normalized.toBigDecimalOrNull() ?: return null
    return decimal.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
}

private fun String.filterDateInput(): String = filter { it.isDigit() || it == '-' }.take(10)

private fun String.toEpochMillisAtStartOfDayOrNull(zoneId: ZoneId = ZoneId.systemDefault()): Long? =
    try {
        LocalDate.parse(trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }

private fun defaultStartDateInput(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val today = LocalDate.now(zoneId)
    val target = if (today.dayOfMonth == 1) today else today.withDayOfMonth(1).plusMonths(1)
    return target.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
