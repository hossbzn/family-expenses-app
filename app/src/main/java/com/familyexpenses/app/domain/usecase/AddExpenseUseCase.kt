package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.domain.model.AddExpenseRequest
import com.familyexpenses.app.domain.model.AddExpenseResult
import java.util.UUID
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
) {

    suspend operator fun invoke(request: AddExpenseRequest): AddExpenseResult {
        require(request.amountMinor > 0L) { "El importe debe ser mayor que cero." }
        require(request.categoryId.isNotBlank()) { "La categoria es obligatoria." }

        accountRepository.getAccountById(request.payingAccountId)
            ?: throw IllegalArgumentException("La cuenta pagadora seleccionada no existe.")

        accountRepository.getAccountById(request.responsibleAccountId)
            ?: throw IllegalArgumentException("La cuenta responsable seleccionada no existe.")

        categoryRepository.getCategoryById(request.categoryId)
            ?: throw IllegalArgumentException("La categoría seleccionada no existe.")

        val transactionId = UUID.randomUUID().toString()
        val isAdvance = request.payingAccountId != request.responsibleAccountId

        expenseRepository.addTransaction(
            transaction = TransactionEntity(
                id = transactionId,
                type = request.type,
                amountMinor = request.amountMinor,
                payingAccountId = request.payingAccountId,
                responsibleAccountId = request.responsibleAccountId,
                categoryId = request.categoryId,
                note = request.note?.takeIf { it.isNotBlank() },
                source = TransactionSource.MANUAL,
                recurrenceRuleId = null,
                occurredAt = request.occurredAt,
                createdAt = System.currentTimeMillis(),
            ),
        )

        return AddExpenseResult(
            transactionId = transactionId,
            isAdvance = isAdvance,
        )
    }
}
