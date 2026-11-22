package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.BalanceAdjustmentRepository
import java.util.UUID
import javax.inject.Inject

class CreateBalanceAdjustmentUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val balanceAdjustmentRepository: BalanceAdjustmentRepository,
) {

    suspend operator fun invoke(
        accountId: String,
        amountDeltaMinor: Long,
        reason: String,
        occurredAt: Long = System.currentTimeMillis(),
    ): String {
        require(amountDeltaMinor != 0L) { "El ajuste de saldo debe tener un delta distinto de cero." }
        require(reason.isNotBlank()) { "El motivo del ajuste es obligatorio para garantizar la trazabilidad." }

        accountRepository.getAccountById(accountId)
            ?: throw IllegalArgumentException("La cuenta seleccionada no existe.")

        val adjustmentId = UUID.randomUUID().toString()
        balanceAdjustmentRepository.addAdjustment(
            BalanceAdjustmentEntity(
                id = adjustmentId,
                accountId = accountId,
                amountDeltaMinor = amountDeltaMinor,
                reason = reason.trim(),
                occurredAt = occurredAt,
                createdAt = System.currentTimeMillis(),
            ),
        )

        return adjustmentId
    }
}
