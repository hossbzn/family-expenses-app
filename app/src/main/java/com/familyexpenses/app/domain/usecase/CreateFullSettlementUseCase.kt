package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.TransferRepository
import javax.inject.Inject
import kotlin.math.abs

class CreateFullSettlementUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val transferRepository: TransferRepository,
    private val computeNetDebtUseCase: ComputeNetDebtUseCase,
    private val executeTransferUseCase: ExecuteTransferUseCase,
) {
    suspend operator fun invoke(settledAt: Long = System.currentTimeMillis()): String {
        val accounts: List<AccountEntity> = accountRepository.getAllAccounts()
        val transactions = expenseRepository.getAllTransactions()
        val transfers = transferRepository.getAllTransfers()

        val netDebt = computeNetDebtUseCase.compute(
            accounts = accounts,
            transactions = transactions,
            transfers = transfers,
        )

        require(netDebt != 0L) { "No hay deuda pendiente que liquidar." }

        val personalAccount = accounts.first { it.type == AccountType.PERSONAL }
        val familyAccount = accounts.first { it.type == AccountType.FAMILY }

        val sourceId: String
        val destId: String

        if (netDebt > 0L) {
            // Familia debe a Personal -> paga Familia a Personal
            sourceId = familyAccount.id
            destId = personalAccount.id
        } else {
            // Personal debe a Familia -> paga Personal a Familia
            sourceId = personalAccount.id
            destId = familyAccount.id
        }

        return executeTransferUseCase(
            type = TransferType.REIMBURSEMENT,
            sourceAccountId = sourceId,
            destinationAccountId = destId,
            amountMinor = abs(netDebt),
            note = "Liquidación total de adelantos",
            occurredAt = settledAt,
        )
    }
}
