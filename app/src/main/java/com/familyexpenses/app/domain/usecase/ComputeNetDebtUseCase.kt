package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import javax.inject.Inject

class ComputeNetDebtUseCase @Inject constructor() {

    /**
     * Calcula la deuda neta firmada entre Familia y Personal:
     * - Positivo (> 0): Familia debe a Personal.
     * - Negativo (< 0): Personal debe a Familia.
     * - Cero (= 0): Sin deudas pendientes.
     *
     * Fórmula:
     * Adelantos Personal->Familia - Adelantos Familia->Personal - Devoluciones Familia->Personal + Devoluciones Personal->Familia
     */
    fun compute(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        transfers: List<TransferEntity>,
    ): Long {
        val accountTypesById = accounts.associate { it.id to it.type }

        var advancesPersonalToFamily = 0L
        var advancesFamilyToPersonal = 0L

        for (transaction in transactions) {
            if (transaction.type == TransactionType.EXPENSE && transaction.payingAccountId != transaction.responsibleAccountId) {
                val payingType = accountTypesById[transaction.payingAccountId]
                val responsibleType = accountTypesById[transaction.responsibleAccountId]

                if (payingType == AccountType.PERSONAL && responsibleType == AccountType.FAMILY) {
                    advancesPersonalToFamily += transaction.amountMinor
                } else if (payingType == AccountType.FAMILY && responsibleType == AccountType.PERSONAL) {
                    advancesFamilyToPersonal += transaction.amountMinor
                }
            }
        }

        var reimbursementsFamilyToPersonal = 0L
        var reimbursementsPersonalToFamily = 0L

        for (transfer in transfers) {
            if (transfer.type == TransferType.REIMBURSEMENT) {
                val sourceType = accountTypesById[transfer.sourceAccountId]
                val destinationType = accountTypesById[transfer.destinationAccountId]

                if (sourceType == AccountType.FAMILY && destinationType == AccountType.PERSONAL) {
                    reimbursementsFamilyToPersonal += transfer.amountMinor
                } else if (sourceType == AccountType.PERSONAL && destinationType == AccountType.FAMILY) {
                    reimbursementsPersonalToFamily += transfer.amountMinor
                }
            }
        }

        return advancesPersonalToFamily - advancesFamilyToPersonal - reimbursementsFamilyToPersonal + reimbursementsPersonalToFamily
    }
}
