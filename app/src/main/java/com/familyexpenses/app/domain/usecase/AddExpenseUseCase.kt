package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.worker.DatabaseSeedWorker
import com.familyexpenses.app.domain.model.AddExpenseRequest
import com.familyexpenses.app.domain.model.AddExpenseResult
import com.familyexpenses.app.domain.rule.ValidateFamilyPaidFromPersonalRule
import java.util.UUID
import javax.inject.Inject

class AddExpenseUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val validateFamilyPaidFromPersonalRule: ValidateFamilyPaidFromPersonalRule,
) {

    suspend operator fun invoke(request: AddExpenseRequest): AddExpenseResult {
        require(request.amountMinor > 0L) { "El importe debe ser mayor que cero." }
        require(request.categoryId.isNotBlank()) { "La categoria es obligatoria." }

        val account = accountRepository.getAccountById(request.accountId)
            ?: throw IllegalArgumentException("La cuenta seleccionada no existe.")

        validateFamilyPaidFromPersonalRule.validate(
            accountType = account.type,
            paidFromPersonal = request.paidFromPersonal,
        )

        val transactionId = UUID.randomUUID().toString()
        expenseRepository.addExpense(
            transaction = TransactionEntity(
                id = transactionId,
                accountId = request.accountId,
                categoryId = request.categoryId,
                type = TransactionType.EXPENSE,
                amountMinor = request.amountMinor,
                paidFromPersonal = request.paidFromPersonal,
                note = request.note?.takeIf { it.isNotBlank() },
                occurredAt = request.occurredAt,
                createdAt = System.currentTimeMillis(),
            ),
            createReimbursementLedger = account.type == AccountType.FAMILY && request.paidFromPersonal,
            familyAccountId = DatabaseSeedWorker.FAMILY_ACCOUNT_ID,
            personalAccountId = DatabaseSeedWorker.PERSONAL_ACCOUNT_ID,
        )

        return AddExpenseResult(
            transactionId = transactionId,
            createdReimbursementLedger = account.type == AccountType.FAMILY && request.paidFromPersonal,
        )
    }
}
