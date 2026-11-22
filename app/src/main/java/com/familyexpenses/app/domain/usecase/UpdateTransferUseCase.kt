package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.TransferRepository
import javax.inject.Inject
import kotlin.math.abs

class UpdateTransferUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val expenseRepository: ExpenseRepository,
    private val transferRepository: TransferRepository,
    private val computeNetDebtUseCase: ComputeNetDebtUseCase,
) {

    suspend operator fun invoke(
        transferId: String,
        type: TransferType,
        sourceAccountId: String,
        destinationAccountId: String,
        amountMinor: Long,
        note: String?,
        occurredAt: Long,
    ) {
        require(amountMinor > 0L) { "El importe debe ser mayor que cero." }
        require(sourceAccountId != destinationAccountId) { "La cuenta origen y destino deben ser diferentes." }

        val existing = transferRepository.getTransferById(transferId)
            ?: throw IllegalArgumentException("La transferencia no existe.")

        val sourceAccount = accountRepository.getAccountById(sourceAccountId)
            ?: throw IllegalArgumentException("La cuenta origen no existe.")
        val destinationAccount = accountRepository.getAccountById(destinationAccountId)
            ?: throw IllegalArgumentException("La cuenta destino no existe.")

        if (type == TransferType.REIMBURSEMENT) {
            val accounts = listOf(sourceAccount, destinationAccount)
            val transactions = expenseRepository.getAllTransactions()
            val otherTransfers = transferRepository.getAllTransfers().filterNot { it.id == transferId }

            val netDebtWithoutThis = computeNetDebtUseCase.compute(
                accounts = accounts,
                transactions = transactions,
                transfers = otherTransfers,
            )

            if (netDebtWithoutThis > 0L) {
                require(sourceAccount.type == AccountType.FAMILY && destinationAccount.type == AccountType.PERSONAL) {
                    "La devolución debe ser de Familia hacia Personal para amortizar la deuda pendiente."
                }
                require(amountMinor <= netDebtWithoutThis) {
                    "El importe de la devolución ($amountMinor céntimos) no puede superar la deuda pendiente ($netDebtWithoutThis céntimos)."
                }
            } else if (netDebtWithoutThis < 0L) {
                val pendingDebt = abs(netDebtWithoutThis)
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

        transferRepository.addTransfer(
            existing.copy(
                type = type,
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                amountMinor = amountMinor,
                occurredAt = occurredAt,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }
}
