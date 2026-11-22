package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputeNetDebtUseCaseTest {

    private val useCase = ComputeNetDebtUseCase()

    private val personalAccount = AccountEntity(
        id = "acc-personal",
        name = "Personal",
        type = AccountType.PERSONAL,
        currencyCode = "EUR",
        initialBalanceMinor = 100000L,
        createdAt = 0L,
    )

    private val familyAccount = AccountEntity(
        id = "acc-family",
        name = "Familia",
        type = AccountType.FAMILY,
        currencyCode = "EUR",
        initialBalanceMinor = 100000L,
        createdAt = 0L,
    )

    private val accounts = listOf(personalAccount, familyAccount)

    @Test
    fun `zero debt when no advances exist`() {
        val netDebt = useCase.compute(
            accounts = accounts,
            transactions = emptyList(),
            transfers = emptyList(),
        )
        assertEquals(0L, netDebt)
    }

    @Test
    fun `mandatory test - personal pays 80 EUR of family expense results in plus 80 EUR net debt`() {
        // Personal paga 80 € de compra familiar (Personal -80 €, Familia 0 €, Deuda +80 €)
        val expense = TransactionEntity(
            id = "tx-1",
            type = TransactionType.EXPENSE,
            amountMinor = 8000L, // 80.00 EUR
            payingAccountId = personalAccount.id,
            responsibleAccountId = familyAccount.id,
            categoryId = "cat-groceries",
            occurredAt = 1000L,
            createdAt = 1000L,
        )

        val netDebt = useCase.compute(
            accounts = accounts,
            transactions = listOf(expense),
            transfers = emptyList(),
        )
        // Positivo = Familia debe a Personal 80.00 EUR
        assertEquals(8000L, netDebt)
    }

    @Test
    fun `mandatory test - partial reimbursement of 30 EUR from family reduces net debt to plus 50 EUR`() {
        // Personal paga 80 €
        val expense = TransactionEntity(
            id = "tx-1",
            type = TransactionType.EXPENSE,
            amountMinor = 8000L,
            payingAccountId = personalAccount.id,
            responsibleAccountId = familyAccount.id,
            categoryId = "cat-groceries",
            occurredAt = 1000L,
            createdAt = 1000L,
        )
        // Familia devuelve 30 € a Personal
        val reimbursement = TransferEntity(
            id = "tr-1",
            type = TransferType.REIMBURSEMENT,
            sourceAccountId = familyAccount.id,
            destinationAccountId = personalAccount.id,
            amountMinor = 3000L, // 30.00 EUR
            occurredAt = 2000L,
            createdAt = 2000L,
        )

        val netDebt = useCase.compute(
            accounts = accounts,
            transactions = listOf(expense),
            transfers = listOf(reimbursement),
        )
        // 80.00 - 30.00 = 50.00 EUR pendiente (5000 céntimos)
        assertEquals(5000L, netDebt)
    }

    @Test
    fun `full reimbursement brings net debt back to zero`() {
        val expense = TransactionEntity(
            id = "tx-1",
            type = TransactionType.EXPENSE,
            amountMinor = 8000L,
            payingAccountId = personalAccount.id,
            responsibleAccountId = familyAccount.id,
            categoryId = "cat-groceries",
            occurredAt = 1000L,
            createdAt = 1000L,
        )
        val reimbursement = TransferEntity(
            id = "tr-1",
            type = TransferType.REIMBURSEMENT,
            sourceAccountId = familyAccount.id,
            destinationAccountId = personalAccount.id,
            amountMinor = 8000L,
            occurredAt = 2000L,
            createdAt = 2000L,
        )

        val netDebt = useCase.compute(
            accounts = accounts,
            transactions = listOf(expense),
            transfers = listOf(reimbursement),
        )
        assertEquals(0L, netDebt)
    }

    @Test
    fun `reverse advance - family pays personal expense generates negative net debt`() {
        // Familia paga 45 € de un gasto Personal
        val expense = TransactionEntity(
            id = "tx-2",
            type = TransactionType.EXPENSE,
            amountMinor = 4500L, // 45.00 EUR
            payingAccountId = familyAccount.id,
            responsibleAccountId = personalAccount.id,
            categoryId = "cat-clothing",
            occurredAt = 1000L,
            createdAt = 1000L,
        )

        val netDebt = useCase.compute(
            accounts = accounts,
            transactions = listOf(expense),
            transfers = emptyList(),
        )
        // Negativo = Personal debe a Familia 45.00 EUR
        assertEquals(-4500L, netDebt)
    }

    @Test
    fun `normal transfer does not affect net debt`() {
        val expense = TransactionEntity(
            id = "tx-1",
            type = TransactionType.EXPENSE,
            amountMinor = 8000L,
            payingAccountId = personalAccount.id,
            responsibleAccountId = familyAccount.id,
            categoryId = "cat-groceries",
            occurredAt = 1000L,
            createdAt = 1000L,
        )
        // Transferencia NORMAL de 100 EUR de Familia a Personal
        val normalTransfer = TransferEntity(
            id = "tr-normal",
            type = TransferType.NORMAL,
            sourceAccountId = familyAccount.id,
            destinationAccountId = personalAccount.id,
            amountMinor = 10000L,
            occurredAt = 2000L,
            createdAt = 2000L,
        )

        val netDebt = useCase.compute(
            accounts = accounts,
            transactions = listOf(expense),
            transfers = listOf(normalTransfer),
        )
        // La deuda sigue siendo de 80.00 EUR, intacta
        assertEquals(8000L, netDebt)
    }

    @Test
    fun `same-account expenses do not generate advances or affect debt`() {
        val personalSelfExpense = TransactionEntity(
            id = "tx-p",
            type = TransactionType.EXPENSE,
            amountMinor = 2500L,
            payingAccountId = personalAccount.id,
            responsibleAccountId = personalAccount.id,
            categoryId = "cat-restaurants",
            occurredAt = 1000L,
            createdAt = 1000L,
        )
        val familySelfExpense = TransactionEntity(
            id = "tx-f",
            type = TransactionType.EXPENSE,
            amountMinor = 6000L,
            payingAccountId = familyAccount.id,
            responsibleAccountId = familyAccount.id,
            categoryId = "cat-bills",
            occurredAt = 1100L,
            createdAt = 1100L,
        )

        val netDebt = useCase.compute(
            accounts = accounts,
            transactions = listOf(personalSelfExpense, familySelfExpense),
            transfers = emptyList(),
        )
        assertEquals(0L, netDebt)
    }
}
