package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.TransferRepository
import java.util.UUID
import javax.inject.Inject
import kotlin.math.abs

class ExecuteTransferUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val transferRepository: TransferRepository,
    private val computeNetDebtUseCase: ComputeNetDebtUseCase,
) {

    suspend operator fun invoke(
        type: TransferType,
        sourceAccountId: String,
        destinationAccountId: String,
        amountMinor: Long,
        note: String? = null,
        occurredAt: Long = System.currentTimeMillis(),
    ): String {
        require(amountMinor > 0L) { "El importe debe ser mayor que cero." }
        require(sourceAccountId != destinationAccountId) { "La cuenta origen y destino deben ser diferentes." }

        val sourceAccount = accountRepository.getAccountById(sourceAccountId)
            ?: throw IllegalArgumentException("La cuenta origen no existe.")
        val destinationAccount = accountRepository.getAccountById(destinationAccountId)
            ?: throw IllegalArgumentException("La cuenta destino no existe.")

        if (type == TransferType.REIMBURSEMENT) {
            val accounts = listOf(sourceAccount, destinationAccount)
            val transactions = expenseRepository.getAllTransactions()
            val transfers = transferRepository.getAllTransfers()

            val netDebt = computeNetDebtUseCase.compute(
                accounts = accounts,
                transactions = transactions,
                transfers = transfers,
            )

            if (netDebt > 0L) {
                // Familia debe a Personal -> La devolución debe ser de Familia a Personal
                require(sourceAccount.type == AccountType.FAMILY && destinationAccount.type == AccountType.PERSONAL) {
                    "La devolución debe ser de Familia hacia Personal para amortizar la deuda pendiente."
                }
                require(amountMinor <= netDebt) {
                    "El importe de la devolución ($amountMinor céntimos) no puede superar la deuda pendiente ($netDebt céntimos)."
                }
            } else if (netDebt < 0L) {
                // Personal debe a Familia -> La devolución debe ser de Personal a Familia
                val pendingDebt = abs(netDebt)
                require(sourceAccount.type == AccountType.PERSONAL && destinationAccount.type == AccountType.FAMILY) {
                    "La devolución debe ser de Personal hacia Familia para amortizar la deuda pendiente."
                }
                require(amountMinor <= pendingDebt) {
                    "El importe de la devolución ($amountMinor céntimos) no puede superar la deuda pendiente ($pendingDebt céntimos)."
                }
            } else {
                throw IllegalStateException("No hay deuda pendiente que devolver.")
            }
        }

        val transferId = UUID.randomUUID().toString()
        transferRepository.addTransfer(
            TransferEntity(
                id = transferId,
                type = type,
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                amountMinor = amountMinor,
                occurredAt = occurredAt,
                note = note?.takeIf { it.isNotBlank() },
                createdAt = System.currentTimeMillis(),
            ),
        )

        return transferId
    }
}
