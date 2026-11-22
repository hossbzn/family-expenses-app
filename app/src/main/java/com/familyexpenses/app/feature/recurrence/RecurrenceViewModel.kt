package com.familyexpenses.app.feature.recurrence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.RecurrenceExecutionMode
import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.RecurrenceRuleStatus
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.dao.RecurrenceRuleDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.seed.DatabaseSeeder
import com.familyexpenses.app.domain.rule.RecurrenceCalculator
import com.familyexpenses.app.domain.usecase.ManagePendingOccurrencesUseCase
import com.familyexpenses.app.domain.usecase.ManageRecurrenceRulesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.UUID
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
    recurrenceRuleDao: RecurrenceRuleDao,
    private val managePendingOccurrencesUseCase: ManagePendingOccurrencesUseCase,
    private val manageRecurrenceRulesUseCase: ManageRecurrenceRulesUseCase,
    private val recurrenceCalculator: RecurrenceCalculator,
) : ViewModel() {

    private val formState = MutableStateFlow(RecurrenceFormState())

    val uiState: StateFlow<RecurrenceUiState> = combine(
        accountRepository.observeAccounts(),
        categoryRepository.observeCategories(),
        recurrenceRuleDao.observeRules(),
        managePendingOccurrencesUseCase.observePendingOccurrences(),
        formState,
    ) { accounts, categories, rules, pendingOccurrences, form ->
        val activeAccounts = accounts.filter { !it.isArchived }
        val filteredCategories = categories.filter { category ->
            when (form.selectedType) {
                TransactionType.EXPENSE -> category.type == CategoryType.EXPENSE
                TransactionType.INCOME -> category.type == CategoryType.INCOME
                TransactionType.ADJUSTMENT -> false
            }
        }

        val payingAccountId = form.selectedPayingAccountId
            ?: activeAccounts.firstOrNull { it.id == DatabaseSeeder.PERSONAL_ACCOUNT_ID }?.id
            ?: activeAccounts.firstOrNull()?.id
            ?: ""

        val responsibleAccountId = form.selectedResponsibleAccountId
            ?: activeAccounts.firstOrNull { it.id == DatabaseSeeder.FAMILY_ACCOUNT_ID }?.id
            ?: activeAccounts.firstOrNull()?.id
            ?: ""

        val categoryId = form.selectedCategoryId
            ?.takeIf { id -> filteredCategories.any { it.id == id } }
            ?: filteredCategories.firstOrNull()?.id
            ?: ""

        val previewRule = buildDraftRule(form, payingAccountId, responsibleAccountId, categoryId)
        val previewNextExecutionAt = recurrenceCalculator.calculateFirstOccurrence(previewRule)

        RecurrenceUiState(
            editingRuleId = form.editingRuleId,
            selectedType = form.selectedType,
            selectedPayingAccountId = payingAccountId,
            selectedResponsibleAccountId = responsibleAccountId,
            selectedCategoryId = categoryId,
            amountInput = form.amountInput,
            noteInput = form.noteInput,
            frequency = form.frequency,
            intervalCount = form.intervalCount,
            dayOfWeek = form.dayOfWeek,
            dayOfMonth = form.dayOfMonth,
            monthOfYear = form.monthOfYear,
            mode = form.mode,
            startDateInput = form.startDateInput,
            endDateInput = form.endDateInput,
            nextExecutionPreviewAt = previewNextExecutionAt,
            accountOptions = activeAccounts,
            categoryOptions = filteredCategories,
            rules = rules,
            pendingOccurrences = pendingOccurrences,
            isSaving = form.isSaving,
            errorMessage = form.errorMessage,
            amountError = form.amountError,
            startDateError = form.startDateError,
            endDateError = form.endDateError,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecurrenceUiState(),
    )

    fun onTypeSelected(type: TransactionType) {
        formState.update { it.copy(selectedType = type, selectedCategoryId = null, errorMessage = null) }
    }

    fun onPayingAccountSelected(accountId: String) {
        formState.update { it.copy(selectedPayingAccountId = accountId, errorMessage = null) }
    }

    fun onResponsibleAccountSelected(accountId: String) {
        formState.update { it.copy(selectedResponsibleAccountId = accountId, errorMessage = null) }
    }

    fun onCategorySelected(categoryId: String) {
        formState.update { it.copy(selectedCategoryId = categoryId, errorMessage = null) }
    }

    fun onAmountChanged(value: String) {
        formState.update {
            it.copy(
                amountInput = value.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' },
                amountError = null,
                errorMessage = null,
            )
        }
    }

    fun onNoteChanged(value: String) {
        formState.update { it.copy(noteInput = value, errorMessage = null) }
    }

    fun onFrequencySelected(frequency: RecurrenceFrequency) {
        formState.update { it.copy(frequency = frequency, errorMessage = null) }
    }

    fun onIntervalCountSelected(count: Int) {
        formState.update { it.copy(intervalCount = count, errorMessage = null) }
    }

    fun onDayOfWeekSelected(day: Int) {
        formState.update { it.copy(dayOfWeek = day, errorMessage = null) }
    }

    fun onDayOfMonthSelected(day: Int) {
        formState.update { it.copy(dayOfMonth = day, errorMessage = null) }
    }

    fun onMonthOfYearSelected(month: Int) {
        formState.update { it.copy(monthOfYear = month, errorMessage = null) }
    }

    fun onModeSelected(mode: RecurrenceExecutionMode) {
        formState.update { it.copy(mode = mode, errorMessage = null) }
    }

    fun onStartDateChanged(value: String) {
        formState.update { it.copy(startDateInput = value.filterDateInput(), startDateError = null, errorMessage = null) }
    }

    fun onEndDateChanged(value: String) {
        formState.update { it.copy(endDateInput = value.filterDateInput(), endDateError = null, errorMessage = null) }
    }

    fun startEditRule(rule: RecurrenceRuleEntity) {
        formState.update {
            RecurrenceFormState(
                editingRuleId = rule.id,
                selectedType = rule.type,
                selectedPayingAccountId = rule.payingAccountId,
                selectedResponsibleAccountId = rule.responsibleAccountId,
                selectedCategoryId = rule.categoryId,
                amountInput = (rule.amountMinor / 100.0).toString().replace('.', ','),
                noteInput = rule.note.orEmpty(),
                frequency = rule.frequency,
                intervalCount = rule.intervalCount ?: 1,
                dayOfWeek = rule.dayOfWeek ?: 1,
                dayOfMonth = rule.dayOfMonth ?: 1,
                monthOfYear = rule.monthOfYear ?: 1,
                mode = rule.mode,
                startDateInput = rule.startAt.toIsoDateString(),
                endDateInput = rule.endAt?.toIsoDateString().orEmpty(),
            )
        }
    }

    fun resetForm() {
        formState.value = RecurrenceFormState()
    }

    fun saveRule() {
        val snapshot = uiState.value
        var isValid = true

        val amountMinor = snapshot.amountInput.toMinorAmountOrNull()
        if (amountMinor == null || amountMinor <= 0L) {
            formState.update { it.copy(amountError = "Introduce un importe válido mayor que cero.") }
            isValid = false
        }

        val startAt = snapshot.startDateInput.toEpochMillisAtStartOfDayOrNull()
        if (startAt == null) {
            formState.update { it.copy(startDateError = "Formato de fecha inválido (AAAA-MM-DD).") }
            isValid = false
        }

        val endAt = if (snapshot.endDateInput.isBlank()) {
            null
        } else {
            val parsed = snapshot.endDateInput.toEpochMillisAtStartOfDayOrNull()
            if (parsed == null) {
                formState.update { it.copy(endDateError = "Formato de fecha inválido (AAAA-MM-DD).") }
                isValid = false
                null
            } else if (startAt != null && parsed < startAt) {
                formState.update { it.copy(endDateError = "La fecha fin debe ser posterior a la de inicio.") }
                isValid = false
                null
            } else {
                parsed
            }
        }

        if (!isValid) return

        val isEditing = snapshot.editingRuleId != null
        val ruleId = snapshot.editingRuleId ?: UUID.randomUUID().toString()

        val rule = RecurrenceRuleEntity(
            id = ruleId,
            type = snapshot.selectedType,
            payingAccountId = snapshot.selectedPayingAccountId,
            responsibleAccountId = if (snapshot.selectedType == TransactionType.EXPENSE) snapshot.selectedResponsibleAccountId else snapshot.selectedPayingAccountId,
            categoryId = snapshot.selectedCategoryId,
            amountMinor = amountMinor!!,
            note = snapshot.noteInput.takeIf { it.isNotBlank() },
            frequency = snapshot.frequency,
            intervalCount = snapshot.intervalCount,
            dayOfWeek = snapshot.dayOfWeek,
            dayOfMonth = snapshot.dayOfMonth,
            monthOfYear = snapshot.monthOfYear,
            mode = snapshot.mode,
            status = RecurrenceRuleStatus.ACTIVE,
            startAt = startAt!!,
            endAt = endAt,
            nextOccurrenceAt = startAt,
            createdAt = System.currentTimeMillis(),
        )

        viewModelScope.launch {
            formState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                if (isEditing) {
                    manageRecurrenceRulesUseCase.updateRule(rule)
                } else {
                    manageRecurrenceRulesUseCase.createRule(rule)
                }
            }.onSuccess {
                resetForm()
            }.onFailure { error ->
                formState.update {
                    it.copy(isSaving = false, errorMessage = error.message ?: "No se pudo guardar la regla.")
                }
            }
        }
    }

    fun pauseRule(ruleId: String) {
        viewModelScope.launch {
            runCatching { manageRecurrenceRulesUseCase.pauseRule(ruleId) }
                .onFailure { err -> formState.update { it.copy(errorMessage = err.message) } }
        }
    }

    fun resumeRule(ruleId: String) {
        viewModelScope.launch {
            runCatching { manageRecurrenceRulesUseCase.resumeRule(ruleId) }
                .onFailure { err -> formState.update { it.copy(errorMessage = err.message) } }
        }
    }

    fun archiveRule(ruleId: String) {
        viewModelScope.launch {
            runCatching { manageRecurrenceRulesUseCase.archiveRule(ruleId) }
                .onFailure { err -> formState.update { it.copy(errorMessage = err.message) } }
        }
    }

    fun restoreRule(ruleId: String) {
        viewModelScope.launch {
            runCatching { manageRecurrenceRulesUseCase.restoreRule(ruleId) }
                .onFailure { err -> formState.update { it.copy(errorMessage = err.message) } }
        }
    }

    fun confirmOccurrence(occurrenceId: String, editedAmountMinor: Long? = null, editedScheduledAt: Long? = null) {
        viewModelScope.launch {
            runCatching { managePendingOccurrencesUseCase.confirmOccurrence(occurrenceId, editedAmountMinor, editedScheduledAt) }
                .onFailure { err -> formState.update { it.copy(errorMessage = err.message) } }
        }
    }

    fun confirmAllPendingOccurrences() {
        val ids = uiState.value.pendingOccurrences.map { it.id }
        viewModelScope.launch {
            runCatching { managePendingOccurrencesUseCase.confirmMultipleOccurrences(ids) }
                .onFailure { err -> formState.update { it.copy(errorMessage = err.message) } }
        }
    }

    fun skipOccurrence(occurrenceId: String) {
        viewModelScope.launch {
            runCatching { managePendingOccurrencesUseCase.skipOccurrence(occurrenceId) }
                .onFailure { err -> formState.update { it.copy(errorMessage = err.message) } }
        }
    }

    fun postponeOccurrence(occurrenceId: String, newScheduledAt: Long) {
        viewModelScope.launch {
            runCatching { managePendingOccurrencesUseCase.postponeOccurrence(occurrenceId, newScheduledAt) }
                .onFailure { err -> formState.update { it.copy(errorMessage = err.message) } }
        }
    }

    private fun buildDraftRule(
        form: RecurrenceFormState,
        payingAccountId: String,
        responsibleAccountId: String,
        categoryId: String,
    ): RecurrenceRuleEntity {
        val startAt = form.startDateInput.toEpochMillisAtStartOfDayOrNull() ?: System.currentTimeMillis()
        val endAt = form.endDateInput.toEpochMillisAtStartOfDayOrNull()
        val amountMinor = form.amountInput.toMinorAmountOrNull() ?: 1000L

        return RecurrenceRuleEntity(
            id = form.editingRuleId ?: "draft",
            type = form.selectedType,
            payingAccountId = payingAccountId,
            responsibleAccountId = if (form.selectedType == TransactionType.EXPENSE) responsibleAccountId else payingAccountId,
            categoryId = categoryId,
            amountMinor = amountMinor,
            note = form.noteInput.takeIf { it.isNotBlank() },
            frequency = form.frequency,
            intervalCount = form.intervalCount,
            dayOfWeek = form.dayOfWeek,
            dayOfMonth = form.dayOfMonth,
            monthOfYear = form.monthOfYear,
            mode = form.mode,
            status = RecurrenceRuleStatus.ACTIVE,
            startAt = startAt,
            endAt = endAt,
            nextOccurrenceAt = startAt,
            createdAt = System.currentTimeMillis(),
        )
    }
}

data class RecurrenceUiState(
    val editingRuleId: String? = null,
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedPayingAccountId: String = "",
    val selectedResponsibleAccountId: String = "",
    val selectedCategoryId: String = "",
    val amountInput: String = "",
    val noteInput: String = "",
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val intervalCount: Int = 1,
    val dayOfWeek: Int = 1,
    val dayOfMonth: Int = 1,
    val monthOfYear: Int = 1,
    val mode: RecurrenceExecutionMode = RecurrenceExecutionMode.AUTOMATIC,
    val startDateInput: String = defaultStartDateInput(),
    val endDateInput: String = "",
    val nextExecutionPreviewAt: Long? = null,
    val accountOptions: List<AccountEntity> = emptyList(),
    val categoryOptions: List<CategoryEntity> = emptyList(),
    val rules: List<RecurrenceRuleEntity> = emptyList(),
    val pendingOccurrences: List<RecurrenceOccurrenceEntity> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val amountError: String? = null,
    val startDateError: String? = null,
    val endDateError: String? = null,
)

private data class RecurrenceFormState(
    val editingRuleId: String? = null,
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedPayingAccountId: String? = null,
    val selectedResponsibleAccountId: String? = null,
    val selectedCategoryId: String? = null,
    val amountInput: String = "",
    val noteInput: String = "",
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val intervalCount: Int = 1,
    val dayOfWeek: Int = 1,
    val dayOfMonth: Int = 1,
    val monthOfYear: Int = 1,
    val mode: RecurrenceExecutionMode = RecurrenceExecutionMode.AUTOMATIC,
    val startDateInput: String = defaultStartDateInput(),
    val endDateInput: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val amountError: String? = null,
    val startDateError: String? = null,
    val endDateError: String? = null,
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

private fun Long.toIsoDateString(zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun defaultStartDateInput(zoneId: ZoneId = ZoneId.systemDefault()): String {
    val today = LocalDate.now(zoneId)
    return today.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
