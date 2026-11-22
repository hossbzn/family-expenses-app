package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import javax.inject.Inject

class ComputeAccountBalanceUseCase @Inject constructor() {

    fun compute(
        account: AccountEntity,
        transactions: List<TransactionEntity>,
        transfers: List<TransferEntity>,
        adjustments: List<BalanceAdjustmentEntity>,
    ): Long {
        var cashBalance = account.initialBalanceMinor

        for (transaction in transactions) {
            when (transaction.type) {
                TransactionType.INCOME -> {
                    // En ingreso, el dinero entra a la cuenta pagadora/destinataria
                    if (transaction.payingAccountId == account.id) {
                        cashBalance += transaction.amountMinor
                    }
                }
                TransactionType.EXPENSE -> {
                    // En gasto, el dinero sale ÚNICAMENTE de la cuenta pagadora
                    if (transaction.payingAccountId == account.id) {
                        cashBalance -= transaction.amountMinor
                    }
                }
                TransactionType.ADJUSTMENT -> {
                    // Los ajustes se gestionan a través de BalanceAdjustmentEntity
                }
            }
        }

        for (transfer in transfers) {
            if (transfer.sourceAccountId == account.id) {
                cashBalance -= transfer.amountMinor
            }
            if (transfer.destinationAccountId == account.id) {
                cashBalance += transfer.amountMinor
            }
        }

        for (adjustment in adjustments) {
            if (adjustment.accountId == account.id) {
                cashBalance += adjustment.amountDeltaMinor
            }
        }

        return cashBalance
    }
}
