package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import javax.inject.Inject

class UpdateTransactionUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
) {

    suspend operator fun invoke(
        transactionId: String,
        amountMinor: Long,
        payingAccountId: String,
        responsibleAccountId: String,
        categoryId: String,
        note: String?,
        occurredAt: Long,
    ) {
        require(amountMinor > 0L) { "El importe debe ser mayor que cero." }

        val existing = expenseRepository.getTransactionById(transactionId)
            ?: throw IllegalArgumentException("El movimiento no existe.")

        accountRepository.getAccountById(payingAccountId)
            ?: throw IllegalArgumentException("La cuenta pagadora/destinataria no existe.")
        accountRepository.getAccountById(responsibleAccountId)
            ?: throw IllegalArgumentException("La cuenta responsable no existe.")

        val category = categoryRepository.getCategoryById(categoryId)
            ?: throw IllegalArgumentException("La categoría seleccionada no existe.")

        val expectedCategoryType = when (existing.type) {
            TransactionType.EXPENSE -> CategoryType.EXPENSE
            TransactionType.INCOME -> CategoryType.INCOME
            TransactionType.ADJUSTMENT -> throw IllegalArgumentException("Tipo de movimiento no editable.")
        }
        require(category.type == expectedCategoryType) {
            "La categoría no corresponde al tipo de movimiento."
        }

        val effectiveResponsibleAccountId = when (existing.type) {
            TransactionType.EXPENSE -> responsibleAccountId
            TransactionType.INCOME -> payingAccountId
            TransactionType.ADJUSTMENT -> responsibleAccountId
        }

        expenseRepository.addTransaction(
            existing.copy(
                amountMinor = amountMinor,
                payingAccountId = payingAccountId,
                responsibleAccountId = effectiveResponsibleAccountId,
                categoryId = categoryId,
                occurredAt = occurredAt,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }
}
