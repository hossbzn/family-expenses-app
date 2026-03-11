package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.RecurringRepository
import com.familyexpenses.app.domain.model.RecurringRuleDraft
import com.familyexpenses.app.domain.rule.MonthlyRecurrenceRule
import com.familyexpenses.app.domain.rule.ValidateFamilyPaidFromPersonalRule
import javax.inject.Inject

class CreateMonthlyRecurringRuleUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val recurringRepository: RecurringRepository,
    private val validateFamilyPaidFromPersonalRule: ValidateFamilyPaidFromPersonalRule,
    private val monthlyRecurrenceRule: MonthlyRecurrenceRule,
) {

    suspend operator fun invoke(draft: RecurringRuleDraft): String {
        require(draft.amountMinor > 0L) { "El importe debe ser mayor que cero." }
        require(draft.categoryId.isNotBlank()) { "La categoria es obligatoria." }
        require(draft.endAt == null || draft.endAt >= draft.startAt) {
            "La fecha fin no puede ser anterior a la fecha inicio."
        }

        val account = accountRepository.getAccountById(draft.accountId)
            ?: throw IllegalArgumentException("La cuenta seleccionada no existe.")

        if (draft.type == TransactionType.EXPENSE) {
            validateFamilyPaidFromPersonalRule.validate(
                accountType = account.type,
                paidFromPersonal = draft.paidFromPersonal,
            )
        }

        val firstOccurrenceAt = monthlyRecurrenceRule.firstOccurrenceOnOrAfter(draft.startAt)
        require(draft.endAt == null || firstOccurrenceAt <= draft.endAt) {
            "La fecha fin debe permitir al menos una generacion el dia 1."
        }

        return recurringRepository.createMonthlyRule(
            draft = draft,
            firstOccurrenceAt = firstOccurrenceAt,
        )
    }
}
