package com.familyexpenses.app.domain

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.domain.usecase.ComputeAccountBalanceUseCase
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * End-to-end verification of V6 Accounting Engine Invariants:
 * 1. Independent cash balance vs responsible account.
 * 2. Advance creation on payingAccountId != responsibleAccountId.
 * 3. Bidirectional net debt signed (+ Family owes, - Personal owes, 0 clear).
 * 4. Partial and full reimbursements move real cash and reduce debt.
 * 5. Normal transfers move cash with neutral debt impact and no expense/income stats.
 * 6. Audit adjustments modify cash outside stats.
 * 7. Deletion/modification triggers exact recalculation.
 */
class V6AccountingEngineIntegrationTest {

    private val computeBalanceUseCase = ComputeAccountBalanceUseCase()
    private val computeNetDebtUseCase = ComputeNetDebtUseCase()

    private val personalAccount = AccountEntity(
        id = "acc-personal",
        name = "Personal",
        type = AccountType.PERSONAL,
        currencyCode = "EUR",
        initialBalanceMinor = 100000L, // 1000.00 EUR
        createdAt = 0L,
    )

    private val familyAccount = AccountEntity(
        id = "acc-family",
        name = "Familia",
        type = AccountType.FAMILY,
        currencyCode = "EUR",
        initialBalanceMinor = 100000L, // 1000.00 EUR
        createdAt = 0L,
    )

    private val accounts = listOf(personalAccount, familyAccount)

    private val groceriesCategory = CategoryEntity(
        id = "cat-groceries",
        name = "Supermercado",
        type = CategoryType.EXPENSE,
        createdAt = 0L,
    )

    private val salaryCategory = CategoryEntity(
        id = "cat-salary",
        name = "Nómina",
        type = CategoryType.INCOME,
        createdAt = 0L,
    )

    @Test
    fun `complete accounting cycle scenario matches all V6 rules exactly`() {
        val transactions = mutableListOf<TransactionEntity>()
        val transfers = mutableListOf<TransferEntity>()
        val adjustments = mutableListOf<BalanceAdjustmentEntity>()

        fun currentPersonalCash() = computeBalanceUseCase.compute(personalAccount, transactions, transfers, adjustments)
        fun currentFamilyCash() = computeBalanceUseCase.compute(familyAccount, transactions, transfers, adjustments)
        fun currentNetDebt() = computeNetDebtUseCase.compute(accounts, transactions, transfers)

        // Step 0: Initial state
        assertEquals(100000L, currentPersonalCash()) // 1000.00 €
        assertEquals(100000L, currentFamilyCash())   // 1000.00 €
        assertEquals(0L, currentNetDebt())           // 0.00 €

        // Step 1: Income of 2000 € arrives in Personal account
        val income1 = TransactionEntity(
            id = "tx-inc-1",
            type = TransactionType.INCOME,
            amountMinor = 200000L, // 2000.00 €
            payingAccountId = personalAccount.id,
            responsibleAccountId = personalAccount.id,
            categoryId = salaryCategory.id,
            occurredAt = 500L,
            createdAt = 500L,
        )
        transactions.add(income1)
        assertEquals(300000L, currentPersonalCash()) // 3000.00 €
        assertEquals(100000L, currentFamilyCash())   // 1000.00 €
        assertEquals(0L, currentNetDebt())

        // Step 2: Personal pays 80 EUR for family grocery purchase
        val tx1 = TransactionEntity(
            id = "tx-1",
            type = TransactionType.EXPENSE,
            amountMinor = 8000L, // 80.00 €
            payingAccountId = personalAccount.id,
            responsibleAccountId = familyAccount.id,
            categoryId = groceriesCategory.id,
            occurredAt = 1000L,
            createdAt = 1000L,
        )
        transactions.add(tx1)

        // Verifications:
        // Personal cash down by 80 € (2920 €)
        assertEquals(292000L, currentPersonalCash())
        // Family cash UNTOUCHED (1000 €)
        assertEquals(100000L, currentFamilyCash())
        // Family owes Personal 80 € (+80 €)
        assertEquals(8000L, currentNetDebt())

        // Step 3: Family account pays a genuine family expense of 50 €
        val tx2 = TransactionEntity(
            id = "tx-2",
            type = TransactionType.EXPENSE,
            amountMinor = 5000L, // 50.00 €
            payingAccountId = familyAccount.id,
            responsibleAccountId = familyAccount.id,
            categoryId = groceriesCategory.id,
            occurredAt = 2000L,
            createdAt = 2000L,
        )
        transactions.add(tx2)

        // Family cash down to 950 €, Personal still 2920 €, debt still +80 €
        assertEquals(292000L, currentPersonalCash())
        assertEquals(95000L, currentFamilyCash())
        assertEquals(8000L, currentNetDebt())

        // Step 4: Family reimburses PARTIALLY 30 € to Personal
        val tr1 = TransferEntity(
            id = "tr-reimb-1",
            type = TransferType.REIMBURSEMENT,
            sourceAccountId = familyAccount.id,
            destinationAccountId = personalAccount.id,
            amountMinor = 3000L, // 30.00 €
            occurredAt = 3000L,
            createdAt = 3000L,
        )
        transfers.add(tr1)

        // Verifications:
        // Family cash drops by 30 € -> 920 €
        assertEquals(92000L, currentFamilyCash())
        // Personal cash increases by 30 € -> 2950 €
        assertEquals(295000L, currentPersonalCash())
        // Net debt drops from 80 € to 50 €
        assertEquals(5000L, currentNetDebt())

        // Step 5: Normal transfer of 100 € from Personal to Family
        val tr2 = TransferEntity(
            id = "tr-normal-1",
            type = TransferType.NORMAL,
            sourceAccountId = personalAccount.id,
            destinationAccountId = familyAccount.id,
            amountMinor = 10000L, // 100.00 €
            occurredAt = 4000L,
            createdAt = 4000L,
        )
        transfers.add(tr2)

        // Verifications:
        // Personal: 2950 - 100 = 2850 €
        assertEquals(285000L, currentPersonalCash())
        // Family: 920 + 100 = 1020 €
        assertEquals(102000L, currentFamilyCash())
        // Net debt is completely unchanged by normal transfer -> 50 €
        assertEquals(5000L, currentNetDebt())

        // Step 6: Auditable balance adjustment of +15 € on Personal account
        val adj1 = BalanceAdjustmentEntity(
            id = "adj-1",
            accountId = personalAccount.id,
            amountDeltaMinor = 1500L, // +15.00 €
            reason = "Intereses cuenta corriente",
            occurredAt = 5000L,
            createdAt = 5000L,
        )
        adjustments.add(adj1)

        // Personal cash increases to 2865 €
        assertEquals(286500L, currentPersonalCash())
        assertEquals(102000L, currentFamilyCash())
        assertEquals(5000L, currentNetDebt())

        // Step 7: Full settlement of the remaining 50 € debt from Family to Personal
        val tr3 = TransferEntity(
            id = "tr-reimb-2",
            type = TransferType.REIMBURSEMENT,
            sourceAccountId = familyAccount.id,
            destinationAccountId = personalAccount.id,
            amountMinor = 5000L, // 50.00 €
            occurredAt = 6000L,
            createdAt = 6000L,
        )
        transfers.add(tr3)

        // Verifications:
        // Personal: 2865 + 50 = 2915 €
        assertEquals(291500L, currentPersonalCash())
        // Family: 1020 - 50 = 970 €
        assertEquals(97000L, currentFamilyCash())
        // Net debt is 0 € (clear!)
        assertEquals(0L, currentNetDebt())

        // Step 8: Deletion of the original 80 € expense (tx1)
        transactions.remove(tx1)

        // Without tx1, advances P->F are 0, reimbursements F->P are 30 + 50 = 80 €
        // So Family has over-reimbursed: Personal now owes Family 80 € (net debt = -80 €)
        // And Personal cash had -80 € from tx1 removed: 2915 + 80 = 2995 €
        assertEquals(299500L, currentPersonalCash())
        assertEquals(97000L, currentFamilyCash())
        assertEquals(-8000L, currentNetDebt())
    }
}
