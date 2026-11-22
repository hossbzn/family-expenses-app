package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.domain.model.AddIncomeRequest
import java.util.UUID
import javax.inject.Inject

class AddIncomeUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
) {

    suspend operator fun invoke(request: AddIncomeRequest): String {
        require(request.amountMinor > 0L) { "El importe debe ser mayor que cero." }
        require(request.categoryId.isNotBlank()) { "La categoría es obligatoria." }

        accountRepository.getAccountById(request.destinationAccountId)
            ?: throw IllegalArgumentException("La cuenta destinataria seleccionada no existe.")

        categoryRepository.getCategoryById(request.categoryId)
            ?: throw IllegalArgumentException("La categoría seleccionada no existe.")

        val transactionId = UUID.randomUUID().toString()

        expenseRepository.addTransaction(
            transaction = TransactionEntity(
                id = transactionId,
                type = TransactionType.INCOME,
                amountMinor = request.amountMinor,
                payingAccountId = request.destinationAccountId,
                responsibleAccountId = request.destinationAccountId,
                categoryId = request.categoryId,
                note = request.note?.takeIf { it.isNotBlank() },
                source = TransactionSource.MANUAL,
                recurrenceRuleId = null,
                occurredAt = request.occurredAt,
                createdAt = System.currentTimeMillis(),
            ),
        )

        return transactionId
    }
}
