package com.familyexpenses.app.domain.rule

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.domain.model.MonthlyAccountTotals
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class ComputeMonthlyBalanceRule @Inject constructor() {

    fun compute(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        today: LocalDate,
        zoneId: ZoneId,
    ): MonthlyAccountTotals {
        val accountTypesById = accounts.associate { account ->
            account.id to account.type
        }

        return transactions.fold(MonthlyAccountTotals()) { totals, transaction ->
            if (!transaction.belongsToMonth(today = today, zoneId = zoneId)) {
                return@fold totals
            }

            when (accountTypesById[transaction.accountId]) {
                AccountType.PERSONAL -> totals.copy(
                    personalBalanceMinor = totals.personalBalanceMinor + transaction.personalDelta(),
                )

                AccountType.FAMILY -> totals.copy(
                    familyBalanceMinor = totals.familyBalanceMinor + transaction.familyDelta(),
                )

                else -> totals
            }
        }
    }

    private fun TransactionEntity.belongsToMonth(today: LocalDate, zoneId: ZoneId): Boolean {
        val date = Instant.ofEpochMilli(occurredAt)
            .atZone(zoneId)
            .toLocalDate()
        return date.year == today.year && date.month == today.month
    }

    private fun TransactionEntity.personalDelta(): Long = when (type) {
        TransactionType.INCOME -> amountMinor
        TransactionType.EXPENSE -> -amountMinor
        TransactionType.ADJUSTMENT -> 0L
    }

    private fun TransactionEntity.familyDelta(): Long = when (type) {
        TransactionType.INCOME -> amountMinor
        TransactionType.EXPENSE -> -amountMinor
        TransactionType.ADJUSTMENT -> 0L
    }
}
