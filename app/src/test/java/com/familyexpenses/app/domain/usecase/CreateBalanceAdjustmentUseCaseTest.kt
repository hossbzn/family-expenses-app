package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.BalanceAdjustmentDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.BalanceAdjustmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class CreateBalanceAdjustmentUseCaseTest {

    private val personalAccount = AccountEntity(
        id = "acc-personal",
        name = "Personal",
        type = AccountType.PERSONAL,
        currencyCode = "EUR",
        initialBalanceMinor = 100000L,
        createdAt = 0L,
    )

    private val accounts = mutableListOf(personalAccount)
    private val adjustments = mutableListOf<BalanceAdjustmentEntity>()

    private val fakeAccountDao = object : AccountDao {
        override suspend fun upsert(account: AccountEntity) { accounts.add(account) }
        override suspend fun upsertAll(accounts: List<AccountEntity>) { this@CreateBalanceAdjustmentUseCaseTest.accounts.addAll(accounts) }
        override fun observeAccounts(): Flow<List<AccountEntity>> = flowOf(accounts)
        override suspend fun getAllAccounts(): List<AccountEntity> = accounts
        override suspend fun getAccountById(accountId: String): AccountEntity? = accounts.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accounts.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {}
    }

    private val fakeAdjustmentDao = object : BalanceAdjustmentDao {
        override suspend fun insertAdjustment(adjustment: BalanceAdjustmentEntity) { adjustments.add(adjustment) }
        override fun observeAdjustments(): Flow<List<BalanceAdjustmentEntity>> = flowOf(adjustments)
        override fun observeAdjustmentsForAccount(accountId: String): Flow<List<BalanceAdjustmentEntity>> =
            flowOf(adjustments.filter { it.accountId == accountId })
        override suspend fun getAllAdjustments(): List<BalanceAdjustmentEntity> = adjustments
    }

    private val accountRepository = AccountRepository(fakeAccountDao)
    private val balanceAdjustmentRepository = BalanceAdjustmentRepository(fakeAdjustmentDao)

    private lateinit var useCase: CreateBalanceAdjustmentUseCase

    @Before
    fun setUp() {
        adjustments.clear()
        useCase = CreateBalanceAdjustmentUseCase(
            accountRepository = accountRepository,
            balanceAdjustmentRepository = balanceAdjustmentRepository,
        )
    }

    @Test
    fun `creating balance adjustment requires non-zero delta and non-blank reason for auditing`() {
        runBlocking {
            val adjustmentId = useCase(
                accountId = personalAccount.id,
                amountDeltaMinor = 2500L,
                reason = "Ajuste de descuadre inicial",
            )

            assertNotNull(adjustmentId)
            assertEquals(1, adjustments.size)
            val adj = adjustments.first()
            assertEquals(personalAccount.id, adj.accountId)
            assertEquals(2500L, adj.amountDeltaMinor)
            assertEquals("Ajuste de descuadre inicial", adj.reason)
        }
    }

    @Test
    fun `creating balance adjustment rejects zero delta or blank reason`() {
        runBlocking {
            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    useCase(
                        accountId = personalAccount.id,
                        amountDeltaMinor = 0L,
                        reason = "Sin cambio",
                    )
                }
            }

            assertThrows(IllegalArgumentException::class.java) {
                runBlocking {
                    useCase(
                        accountId = personalAccount.id,
                        amountDeltaMinor = 1000L,
                        reason = "   ",
                    )
                }
            }
        }
    }
}
