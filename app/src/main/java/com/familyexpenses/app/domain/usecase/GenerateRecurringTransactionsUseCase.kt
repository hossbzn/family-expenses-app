package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.RecurringRepository
import com.familyexpenses.app.data.seed.DatabaseSeeder
import com.familyexpenses.app.domain.model.RecurringGenerationResult
import com.familyexpenses.app.domain.rule.MonthlyRecurrenceRule
import java.util.UUID
import javax.inject.Inject

class GenerateRecurringTransactionsUseCase @Inject constructor(
    private val recurringRepository: RecurringRepository,
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val monthlyRecurrenceRule: MonthlyRecurrenceRule,
) {

    suspend operator fun invoke(now: Long = System.currentTimeMillis()): RecurringGenerationResult {
        val activeRules = recurringRepository.getActiveRules()
        var generatedCount = 0

        for (rule in activeRules) {
            val occurrences = monthlyRecurrenceRule.collectDueOccurrences(
                nextOccurrenceAt = rule.nextOccurrenceAt,
                now = now,
                endAt = rule.endAt,
            )

            if (occurrences.dueOccurrences.isNotEmpty()) {
                val account = accountRepository.getAccountById(rule.accountId) ?: continue

                occurrences.dueOccurrences.forEach { occurrenceAt ->
                    val transactionId = UUID.randomUUID().toString()
                    expenseRepository.addExpense(
                        transaction = TransactionEntity(
                            id = transactionId,
                            accountId = rule.accountId,
                            categoryId = rule.categoryId,
                            type = rule.type,
                            amountMinor = rule.amountMinor,
                            paidFromPersonal = rule.type == TransactionType.EXPENSE && rule.paidFromPersonal,
                            note = rule.note,
                            source = TransactionSource.AUTO_RECURRENT,
                            recurrenceRuleId = rule.id,
                            occurredAt = occurrenceAt,
                            createdAt = now,
                        ),
                        createReimbursementLedger = rule.type == TransactionType.EXPENSE &&
                            account.type == AccountType.FAMILY &&
                            rule.paidFromPersonal,
                        familyAccountId = DatabaseSeeder.FAMILY_ACCOUNT_ID,
                        personalAccountId = DatabaseSeeder.PERSONAL_ACCOUNT_ID,
                    )
                    generatedCount++
                }

                recurringRepository.updateNextOccurrence(
                    ruleId = rule.id,
                    nextOccurrenceAt = occurrences.nextOccurrenceAt,
                )
            }

            if (occurrences.hasReachedEnd) {
                recurringRepository.setActive(rule.id, false)
            }
        }

        return RecurringGenerationResult(generatedCount = generatedCount)
    }
}
