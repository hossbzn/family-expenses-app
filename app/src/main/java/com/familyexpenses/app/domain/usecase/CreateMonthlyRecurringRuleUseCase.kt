package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.RecurringRepository
import com.familyexpenses.app.domain.model.RecurringRuleDraft
import com.familyexpenses.app.domain.rule.MonthlyRecurrenceRule
import javax.inject.Inject

class CreateMonthlyRecurringRuleUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    private val monthlyRecurrenceRule: MonthlyRecurrenceRule,
) {

    suspend operator fun invoke(draft: RecurringRuleDraft): String {
        require(draft.amountMinor > 0L) { "El importe debe ser mayor que cero." }
        require(draft.categoryId.isNotBlank()) { "La categoría es obligatoria." }
        require(draft.endAt == null || draft.endAt >= draft.startAt) {
            "La fecha fin no puede ser anterior a la fecha inicio."
        }

        accountRepository.getAccountById(draft.payingAccountId)
            ?: throw IllegalArgumentException("La cuenta pagadora seleccionada no existe.")

        accountRepository.getAccountById(draft.responsibleAccountId)
            ?: throw IllegalArgumentException("La cuenta responsable seleccionada no existe.")

        categoryRepository.getCategoryById(draft.categoryId)
            ?: throw IllegalArgumentException("La categoría seleccionada no existe.")

        val firstOccurrenceAt = monthlyRecurrenceRule.firstOccurrenceOnOrAfter(draft.startAt)
        require(draft.endAt == null || firstOccurrenceAt <= draft.endAt) {
            "La fecha fin debe permitir al menos una generación el día 1."
        }

        return recurringRepository.createMonthlyRule(
            draft = draft,
            firstOccurrenceAt = firstOccurrenceAt,
        )
    }
}
