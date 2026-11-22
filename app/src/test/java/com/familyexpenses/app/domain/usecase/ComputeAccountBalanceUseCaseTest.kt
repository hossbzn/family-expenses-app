package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeAccountBalanceUseCaseTest {

    private val useCase = ComputeAccountBalanceUseCase()

    private val personalAccount = AccountEntity(
        id = "acc-personal",
        name = "Personal",
        type = AccountType.PERSONAL,
        currencyCode = "EUR",
        initialBalanceMinor = 10000L, // 100.00 EUR
        createdAt = 0L,
    )

    private val familyAccount = AccountEntity(
        id = "acc-family",
        name = "Familia",
        type = AccountType.FAMILY,
        currencyCode = "EUR",
        initialBalanceMinor = 20000L, // 200.00 EUR
        createdAt = 0L,
    )

    @Test
    fun `initial balance is returned when no transactions, transfers, or adjustments exist`() {
        val balance = useCase.compute(
            account = personalAccount,
            transactions = emptyList(),
            transfers = emptyList(),
            adjustments = emptyList(),
        )
        assertEquals(10000L, balance)
    }

    @Test
    fun `income increases cash balance of destination account`() {
        val income = TransactionEntity(
            id = "tx-1",
            type = TransactionType.INCOME,
            amountMinor = 150000L, // 1500.00 EUR
            payingAccountId = personalAccount.id,
            responsibleAccountId = personalAccount.id,
            categoryId = "cat-salary",
            occurredAt = 1000L,
            createdAt = 1000L,
        )

        val personalBalance = useCase.compute(
            account = personalAccount,
            transactions = listOf(income),
            transfers = emptyList(),
            adjustments = emptyList(),
        )
        // 100.00 + 1500.00 = 1600.00
        assertEquals(160000L, personalBalance)

        val familyBalance = useCase.compute(
            account = familyAccount,
            transactions = listOf(income),
            transfers = emptyList(),
            adjustments = emptyList(),
        )
        // Family account is untouched
        assertEquals(20000L, familyBalance)
    }

    @Test
    fun `expense deducts cash strictly from paying account and never from responsible account`() {
        // Personal pays 80 EUR for a Family expense
        val sharedExpense = TransactionEntity(
            id = "tx-2",
            type = TransactionType.EXPENSE,
            amountMinor = 8000L, // 80.00 EUR
            payingAccountId = personalAccount.id,
            responsibleAccountId = familyAccount.id,
            categoryId = "cat-groceries",
            occurredAt = 2000L,
            createdAt = 2000L,
        )

        val personalBalance = useCase.compute(
            account = personalAccount,
            transactions = listOf(sharedExpense),
            transfers = emptyList(),
            adjustments = emptyList(),
        )
        // 100.00 - 80.00 = 20.00 EUR (2000 céntimos)
        assertEquals(2000L, personalBalance)

        val familyBalance = useCase.compute(
            account = familyAccount,
            transactions = listOf(sharedExpense),
            transfers = emptyList(),
            adjustments = emptyList(),
        )
        // Family cash balance remains strictly untouched (200.00 EUR)
        assertEquals(20000L, familyBalance)
    }

    @Test
    fun `transfers move cash between source and destination accounts`() {
        val transfer = TransferEntity(
            id = "tr-1",
            type = TransferType.NORMAL,
            sourceAccountId = personalAccount.id,
            destinationAccountId = familyAccount.id,
            amountMinor = 5000L, // 50.00 EUR
            occurredAt = 3000L,
            createdAt = 3000L,
        )

        val personalBalance = useCase.compute(
            account = personalAccount,
            transactions = emptyList(),
            transfers = listOf(transfer),
            adjustments = emptyList(),
        )
        // 100.00 - 50.00 = 50.00 EUR
        assertEquals(5000L, personalBalance)

        val familyBalance = useCase.compute(
            account = familyAccount,
            transactions = emptyList(),
            transfers = listOf(transfer),
            adjustments = emptyList(),
        )
        // 200.00 + 50.00 = 250.00 EUR
        assertEquals(25000L, familyBalance)
    }

    @Test
    fun `balance adjustments apply signed delta directly to cash balance`() {
        val positiveAdj = BalanceAdjustmentEntity(
            id = "adj-1",
            accountId = personalAccount.id,
            amountDeltaMinor = 1500L, // +15.00 EUR
            reason = "Intereses",
            occurredAt = 4000L,
            createdAt = 4000L,
        )
        val negativeAdj = BalanceAdjustmentEntity(
            id = "adj-2",
            accountId = personalAccount.id,
            amountDeltaMinor = -500L, // -5.00 EUR
            reason = "Comisión de mantenimiento",
            occurredAt = 5000L,
            createdAt = 5000L,
        )

        val balance = useCase.compute(
            account = personalAccount,
            transactions = emptyList(),
            transfers = emptyList(),
            adjustments = listOf(positiveAdj, negativeAdj),
        )
        // 10000 + 1500 - 500 = 11000 céntimos (110.00 EUR)
        assertEquals(11000L, balance)
    }
}
