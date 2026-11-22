package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.OccurrenceStatus
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.TransactionRunner
import com.familyexpenses.app.data.local.dao.RecurrenceOccurrenceDao
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import com.familyexpenses.app.domain.model.AddExpenseRequest
import com.familyexpenses.app.domain.model.AddIncomeRequest
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ManagePendingOccurrencesUseCase @Inject constructor(
    private val transactionRunner: TransactionRunner,
    private val occurrenceDao: RecurrenceOccurrenceDao,
    private val addExpenseUseCase: AddExpenseUseCase,
    private val addIncomeUseCase: AddIncomeUseCase,
) {

    fun observePendingOccurrences(now: Long = System.currentTimeMillis()): Flow<List<RecurrenceOccurrenceEntity>> {
        return occurrenceDao.observePendingOccurrences().map { list ->
            list.filter { occurrence ->
                occurrence.reviewAt == null || occurrence.reviewAt <= now
            }
        }
    }

    suspend fun confirmOccurrence(
        occurrenceId: String,
        editedAmountMinor: Long? = null,
        editedScheduledAt: Long? = null,
    ) {
        transactionRunner {
            val occurrence = occurrenceDao.getOccurrenceById(occurrenceId)
                ?: throw IllegalArgumentException("La ocurrencia no existe.")

            require(occurrence.status == OccurrenceStatus.PENDING) {
                "Solo se pueden confirmar ocurrencias pendientes."
            }

            val amountToUse = editedAmountMinor ?: occurrence.amountMinor
            val dateToUse = editedScheduledAt ?: occurrence.scheduledAt

            val transactionId = if (occurrence.type == TransactionType.EXPENSE) {
                val result = addExpenseUseCase(
                    AddExpenseRequest(
                        type = TransactionType.EXPENSE,
                        amountMinor = amountToUse,
                        payingAccountId = occurrence.payingAccountId,
                        responsibleAccountId = occurrence.responsibleAccountId,
                        categoryId = occurrence.categoryId,
                        note = occurrence.note,
                        occurredAt = dateToUse,
                    ),
                )
                result.transactionId
            } else {
                addIncomeUseCase(
                    AddIncomeRequest(
                        amountMinor = amountToUse,
                        destinationAccountId = occurrence.payingAccountId,
                        categoryId = occurrence.categoryId,
                        note = occurrence.note,
                        occurredAt = dateToUse,
                    ),
                )
            }

            occurrenceDao.updateStatus(
                id = occurrenceId,
                status = OccurrenceStatus.CONFIRMED,
                transactionId = transactionId,
            )
        }
    }

    suspend fun confirmMultipleOccurrences(occurrenceIds: List<String>) {
        if (occurrenceIds.isEmpty()) return

        transactionRunner {
            for (id in occurrenceIds) {
                val occurrence = occurrenceDao.getOccurrenceById(id)
                    ?: throw IllegalArgumentException("La ocurrencia $id no existe.")

                if (occurrence.status != OccurrenceStatus.PENDING) {
                    continue
                }

                val transactionId = if (occurrence.type == TransactionType.EXPENSE) {
                    val result = addExpenseUseCase(
                        AddExpenseRequest(
                            type = TransactionType.EXPENSE,
                            amountMinor = occurrence.amountMinor,
                            payingAccountId = occurrence.payingAccountId,
                            responsibleAccountId = occurrence.responsibleAccountId,
                            categoryId = occurrence.categoryId,
                            note = occurrence.note,
                            occurredAt = occurrence.scheduledAt,
                        ),
                    )
                    result.transactionId
                } else {
                    addIncomeUseCase(
                        AddIncomeRequest(
                            amountMinor = occurrence.amountMinor,
                            destinationAccountId = occurrence.payingAccountId,
                            categoryId = occurrence.categoryId,
                            note = occurrence.note,
                            occurredAt = occurrence.scheduledAt,
                        ),
                    )
                }

                occurrenceDao.updateStatus(
                    id = id,
                    status = OccurrenceStatus.CONFIRMED,
                    transactionId = transactionId,
                )
            }
        }
    }

    suspend fun skipOccurrence(occurrenceId: String) {
        val occurrence = occurrenceDao.getOccurrenceById(occurrenceId)
            ?: throw IllegalArgumentException("La ocurrencia no existe.")

        require(occurrence.status == OccurrenceStatus.PENDING) {
            "Solo se pueden omitir ocurrencias pendientes."
        }

        occurrenceDao.updateStatus(
            id = occurrenceId,
            status = OccurrenceStatus.SKIPPED,
            transactionId = null,
        )
    }

    suspend fun postponeOccurrence(occurrenceId: String, newReviewAt: Long) {
        val occurrence = occurrenceDao.getOccurrenceById(occurrenceId)
            ?: throw IllegalArgumentException("La ocurrencia no existe.")

        require(occurrence.status == OccurrenceStatus.PENDING) {
            "Solo se pueden posponer ocurrencias pendientes."
        }

        occurrenceDao.upsert(
            occurrence.copy(
                reviewAt = newReviewAt,
            ),
        )
    }
}
