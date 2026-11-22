package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.TransferDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class ExecuteTransferUseCaseTest {

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

    private val accounts = mutableListOf(personalAccount, familyAccount)
    private val transactions = mutableListOf<TransactionEntity>()
    private val transfers = mutableListOf<TransferEntity>()

    private val fakeAccountDao = object : AccountDao {
        override suspend fun upsert(account: AccountEntity) { accounts.add(account) }
        override suspend fun upsertAll(accounts: List<AccountEntity>) { this@ExecuteTransferUseCaseTest.accounts.addAll(accounts) }
        override fun observeAccounts(): Flow<List<AccountEntity>> = flowOf(accounts)
        override suspend fun getAllAccounts(): List<AccountEntity> = accounts
        override suspend fun getAccountById(accountId: String): AccountEntity? = accounts.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accounts.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {}
    }

    private val fakeTransactionDao = object : TransactionDao {
        override suspend fun insertTransaction(transaction: TransactionEntity) { transactions.add(transaction) }
        override fun observeTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override suspend fun getAllTransactions(): List<TransactionEntity> = transactions
        override suspend fun getTransactionById(transactionId: String): TransactionEntity? =
            transactions.find { it.id == transactionId }
        override suspend fun deleteTransaction(transactionId: String) {
            transactions.removeAll { it.id == transactionId }
        }
    }

    private val fakeTransferDao = object : TransferDao {
        override suspend fun insertTransfer(transfer: TransferEntity) { transfers.add(transfer) }
        override fun observeTransfers(): Flow<List<TransferEntity>> = flowOf(transfers)
        override suspend fun getAllTransfers(): List<TransferEntity> = transfers
        override suspend fun getTransferById(transferId: String): TransferEntity? =
            transfers.find { it.id == transferId }
        override suspend fun deleteTransfer(transferId: String) {
            transfers.removeAll { it.id == transferId }
        }
    }

    private val accountRepository = AccountRepository(fakeAccountDao)
    private val expenseRepository = ExpenseRepository(fakeTransactionDao)
    private val transferRepository = TransferRepository(fakeTransferDao)
    private val computeNetDebtUseCase = ComputeNetDebtUseCase()

    private lateinit var executeTransferUseCase: ExecuteTransferUseCase

    @Before
    fun setUp() {
        transactions.clear()
        transfers.clear()
        executeTransferUseCase = ExecuteTransferUseCase(
            accountRepository = accountRepository,
            expenseRepository = expenseRepository,
            transferRepository = transferRepository,
            computeNetDebtUseCase = computeNetDebtUseCase,
        )
    }

    @Test
    fun `normal transfer succeeds between different accounts with any positive amount`() {
        runBlocking {
            val transferId = executeTransferUseCase(
                type = TransferType.NORMAL,
                sourceAccountId = personalAccount.id,
                destinationAccountId = familyAccount.id,
                amountMinor = 5000L,
                note = "Traspaso mensual",
            )

            assertNotNull(transferId)
            assertEquals(1, transfers.size)
            val created = transfers.first()
            assertEquals(TransferType.NORMAL, created.type)
            assertEquals(5000L, created.amountMinor)
            assertEquals(personalAccount.id, created.sourceAccountId)
            assertEquals(familyAccount.id, created.destinationAccountId)
        }
    }

    @Test
    fun `reimbursement transfer succeeds when debt exists and amount is less or equal to pending debt`() {
        runBlocking {
            // Personal paid 80 EUR for family -> Family owes 80 EUR to personal
            transactions.add(
                TransactionEntity(
                    id = "tx-1",
                    type = TransactionType.EXPENSE,
                    amountMinor = 8000L,
                    payingAccountId = personalAccount.id,
                    responsibleAccountId = familyAccount.id,
                    categoryId = "cat-groceries",
                    occurredAt = 1000L,
                    createdAt = 1000L,
                ),
            )

            // Partial reimbursement of 30 EUR from Family to Personal
            val transferId = executeTransferUseCase(
                type = TransferType.REIMBURSEMENT,
                sourceAccountId = familyAccount.id,
                destinationAccountId = personalAccount.id,
                amountMinor = 3000L,
                note = "Devolución parcial",
            )

            assertNotNull(transferId)
            assertEquals(1, transfers.size)
            val created = transfers.first()
            assertEquals(TransferType.REIMBURSEMENT, created.type)
            assertEquals(3000L, created.amountMinor)

            // Remaining debt should be 50 EUR
            val netDebt = computeNetDebtUseCase.compute(accounts, transactions, transfers)
            assertEquals(5000L, netDebt)
        }
    }

    @Test
    fun `reimbursement transfer fails if amount exceeds pending debt`() {
        runBlocking {
            // Debt is 80 EUR
            transactions.add(
                TransactionEntity(
                    id = "tx-1",
                    type = TransactionType.EXPENSE,
                    amountMinor = 8000L,
                    payingAccountId = personalAccount.id,
                    responsibleAccountId = familyAccount.id,
                    categoryId = "cat-groceries",
                    occurredAt = 1000L,
                    createdAt = 1000L,
                ),
            )

            // Attempting to reimburse 90 EUR (exceeds 80 EUR)
            val exception = assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    executeTransferUseCase(
                        type = TransferType.REIMBURSEMENT,
                        sourceAccountId = familyAccount.id,
                        destinationAccountId = personalAccount.id,
                        amountMinor = 9000L,
                    )
                }
            }
            assertNotNull(exception.message)
        }
    }

    @Test
    fun `reimbursement transfer fails if direction is opposite to debtor to creditor`() {
        runBlocking {
            // Debt: Family owes Personal 80 EUR
            transactions.add(
                TransactionEntity(
                    id = "tx-1",
                    type = TransactionType.EXPENSE,
                    amountMinor = 8000L,
                    payingAccountId = personalAccount.id,
                    responsibleAccountId = familyAccount.id,
                    categoryId = "cat-groceries",
                    occurredAt = 1000L,
                    createdAt = 1000L,
                ),
            )

            // Wrong direction: Personal trying to reimburse Family
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    executeTransferUseCase(
                        type = TransferType.REIMBURSEMENT,
                        sourceAccountId = personalAccount.id,
                        destinationAccountId = familyAccount.id,
                        amountMinor = 3000L,
                    )
                }
            }
        }
    }

    @Test
    fun `reimbursement transfer fails when there is zero pending debt`() {
        runBlocking {
            // No advances exist -> debt is 0
            assertThrows(IllegalStateException::class.java) {
                runBlocking {
                    executeTransferUseCase(
                        type = TransferType.REIMBURSEMENT,
                        sourceAccountId = familyAccount.id,
                        destinationAccountId = personalAccount.id,
                        amountMinor = 1000L,
                    )
                }
            }
        }
    }

    @Test
    fun `transfer rejects non-positive amount or same source and destination`() {
        runBlocking {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    executeTransferUseCase(
                        type = TransferType.NORMAL,
                        sourceAccountId = personalAccount.id,
                        destinationAccountId = personalAccount.id,
                        amountMinor = 1000L,
                    )
                }
            }

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    executeTransferUseCase(
                        type = TransferType.NORMAL,
                        sourceAccountId = personalAccount.id,
                        destinationAccountId = familyAccount.id,
                        amountMinor = 0L,
                    )
                }
            }
        }
    }
}
