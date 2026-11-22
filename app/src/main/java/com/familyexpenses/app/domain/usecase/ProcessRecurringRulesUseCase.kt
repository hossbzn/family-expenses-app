package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.OccurrenceStatus
import com.familyexpenses.app.core.model.RecurrenceExecutionMode
import com.familyexpenses.app.core.model.RecurrenceRuleStatus
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.TransactionRunner
import com.familyexpenses.app.data.local.dao.RecurrenceOccurrenceDao
import com.familyexpenses.app.data.local.dao.RecurrenceRuleDao
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import com.familyexpenses.app.domain.model.AddExpenseRequest
import com.familyexpenses.app.domain.model.AddIncomeRequest
import com.familyexpenses.app.domain.rule.RecurrenceCalculator
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject

class ProcessRecurringRulesUseCase @Inject constructor(
    private val transactionRunner: TransactionRunner,
    private val recurrenceRuleDao: RecurrenceRuleDao,
    private val recurrenceOccurrenceDao: RecurrenceOccurrenceDao,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
    private val recurrenceCalculator: RecurrenceCalculator,
) {

    suspend operator fun invoke(
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val activeRules = recurrenceRuleDao.getActiveRules()

        for (rule in activeRules) {
            transactionRunner {
                var currentNextAt = rule.nextOccurrenceAt
                var isRuleActive = true

                while (isRuleActive && currentNextAt <= now && (rule.endAt == null || currentNextAt <= rule.endAt)) {
                    val scheduledAt = currentNextAt

                    val alreadyExists = recurrenceOccurrenceDao.hasOccurrenceForDate(rule.id, scheduledAt)
                    if (!alreadyExists) {
                        val occurrenceId = UUID.randomUUID().toString()

                        if (rule.mode == RecurrenceExecutionMode.AUTOMATIC) {
                            val transactionId = if (rule.type == TransactionType.EXPENSE) {
                                val result = addExpenseUseCase(
                                    AddExpenseRequest(
                                        type = TransactionType.EXPENSE,
                                        amountMinor = rule.amountMinor,
                                        payingAccountId = rule.payingAccountId,
                                        responsibleAccountId = rule.responsibleAccountId,
                                        categoryId = rule.categoryId,
                                        note = rule.note,
                                        occurredAt = scheduledAt,
                                    ),
                                )
                                result.transactionId
                            } else {
                                addIncomeUseCase(
                                    AddIncomeRequest(
                                        amountMinor = rule.amountMinor,
                                        destinationAccountId = rule.payingAccountId,
                                        categoryId = rule.categoryId,
                                        note = rule.note,
                                        occurredAt = scheduledAt,
                                    ),
                                )
                            }

                            recurrenceOccurrenceDao.upsert(
                                RecurrenceOccurrenceEntity(
                                    id = occurrenceId,
                                    ruleId = rule.id,
                                    status = OccurrenceStatus.CONFIRMED,
                                    scheduledAt = scheduledAt,
                                    amountMinor = rule.amountMinor,
                                    type = rule.type,
                                    payingAccountId = rule.payingAccountId,
                                    responsibleAccountId = rule.responsibleAccountId,
                                    categoryId = rule.categoryId,
                                    note = rule.note,
                                    transactionId = transactionId,
                                    createdAt = System.currentTimeMillis(),
                                ),
                            )
                        } else {
                            recurrenceOccurrenceDao.upsert(
                                RecurrenceOccurrenceEntity(
                                    id = occurrenceId,
                                    ruleId = rule.id,
                                    status = OccurrenceStatus.PENDING,
                                    scheduledAt = scheduledAt,
                                    amountMinor = rule.amountMinor,
                                    type = rule.type,
                                    payingAccountId = rule.payingAccountId,
                                    responsibleAccountId = rule.responsibleAccountId,
                                    categoryId = rule.categoryId,
                                    note = rule.note,
                                    transactionId = null,
                                    createdAt = System.currentTimeMillis(),
                                ),
                            )
                        }
                    }

                    val nextAt = recurrenceCalculator.calculateNextOccurrence(rule, scheduledAt, zoneId)
                    if (nextAt != null) {
                        currentNextAt = nextAt
                    } else {
                        isRuleActive = false
                    }
                }

                if (!isRuleActive) {
                    recurrenceRuleDao.upsert(
                        rule.copy(
                            status = RecurrenceRuleStatus.COMPLETED,
                            nextOccurrenceAt = currentNextAt,
                        ),
                    )
                } else if (currentNextAt != rule.nextOccurrenceAt) {
                    recurrenceRuleDao.updateNextOccurrence(rule.id, currentNextAt)
                }
            }
        }
    }
}
