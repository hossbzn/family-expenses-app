package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.OccurrenceStatus
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.TransactionRunner
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.RecurrenceOccurrenceDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ManagePendingOccurrencesUseCaseTest {

    private val zoneId = ZoneId.of("UTC")
    private lateinit var occurrenceDao: FakePendingOccurrenceDao
    private val transactions = mutableListOf<TransactionEntity>()
    private val accounts = mutableListOf(
        AccountEntity("acc-p", "Personal", AccountType.PERSONAL, "EUR", 100000L, createdAt = 0L),
        AccountEntity("acc-f", "Familia", AccountType.FAMILY, "EUR", 100000L, createdAt = 0L),
    )
    private val categories = mutableListOf(
        CategoryEntity("cat-1", "Super", icon = "cart", type = CategoryType.EXPENSE, createdAt = 0L),
    )

    private val fakeAccountDao = object : AccountDao {
        override suspend fun upsert(account: AccountEntity) { accounts.add(account) }
        override suspend fun upsertAll(accounts: List<AccountEntity>) { this@ManagePendingOccurrencesUseCaseTest.accounts.addAll(accounts) }
        override fun observeAccounts(): Flow<List<AccountEntity>> = flowOf(accounts)
        override suspend fun getAllAccounts(): List<AccountEntity> = accounts
        override suspend fun getAccountById(accountId: String): AccountEntity? = accounts.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accounts.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {}
    }

    private val fakeCategoryDao = object : CategoryDao {
        override suspend fun upsert(category: CategoryEntity) { categories.add(category) }
        override suspend fun upsertAll(categories: List<CategoryEntity>) { this@ManagePendingOccurrencesUseCaseTest.categories.addAll(categories) }
        override fun observeCategories(): Flow<List<CategoryEntity>> = flowOf(categories)
        override fun observeActiveCategories(): Flow<List<CategoryEntity>> = flowOf(categories.filter { !it.isArchived })
        override fun observeActiveCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> = flowOf(categories.filter { !it.isArchived && it.type == type })
        override fun observeFavoriteExpenseCategories(): Flow<List<CategoryEntity>> = flowOf(categories.filter { !it.isArchived && it.isFavorite })
        override suspend fun getCategoryById(categoryId: String): CategoryEntity? = categories.find { it.id == categoryId }
        override suspend fun countCategories(): Int = categories.size
    }

    private val fakeTransactionDao = object : TransactionDao {
        override suspend fun insertTransaction(transaction: TransactionEntity) { transactions.add(transaction) }
        override fun observeTransactions(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override suspend fun getAllTransactions(): List<TransactionEntity> = transactions
        override suspend fun getTransactionById(transactionId: String): TransactionEntity? = transactions.find { it.id == transactionId }
        override suspend fun deleteTransaction(transactionId: String) { transactions.removeAll { it.id == transactionId } }
    }

    private lateinit var useCase: ManagePendingOccurrencesUseCase

    @Before
    fun setUp() {
        occurrenceDao = FakePendingOccurrenceDao()
        transactions.clear()

        val accountRepo = AccountRepository(fakeAccountDao)
        val categoryRepo = CategoryRepository(fakeCategoryDao)
        val expenseRepo = ExpenseRepository(fakeTransactionDao)

        val addExpenseUseCase = AddExpenseUseCase(accountRepo, categoryRepo, expenseRepo)
        val addIncomeUseCase = AddIncomeUseCase(accountRepo, categoryRepo, expenseRepo)
        val runner = object : TransactionRunner {
            override suspend fun <R> invoke(block: suspend () -> R): R = block()
        }

        useCase = ManagePendingOccurrencesUseCase(
            transactionRunner = runner,
            occurrenceDao = occurrenceDao,
            addExpenseUseCase = addExpenseUseCase,
            addIncomeUseCase = addIncomeUseCase,
        )
    }

    @Test
    fun testConfirmOccurrenceRegistersTransactionAndUpdatesStatus() = runTest {
        val scheduledAt = LocalDate.of(2026, 11, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val occ = RecurrenceOccurrenceEntity(
            id = "occ-1",
            ruleId = "rule-1",
            status = OccurrenceStatus.PENDING,
            scheduledAt = scheduledAt,
            amountMinor = 4500L,
            type = TransactionType.EXPENSE,
            payingAccountId = "acc-p",
            responsibleAccountId = "acc-f", // Advance!
            categoryId = "cat-1",
            note = "Cena",
            transactionId = null,
            createdAt = scheduledAt,
        )
        occurrenceDao.upsert(occ)

        useCase.confirmOccurrence("occ-1")

        val updated = occurrenceDao.getOccurrenceById("occ-1")
        assertEquals(OccurrenceStatus.CONFIRMED, updated?.status)
        assertNotNull(updated?.transactionId)

        assertEquals(1, transactions.size)
        val tx = transactions.first()
        assertEquals(4500L, tx.amountMinor)
        assertEquals("acc-p", tx.payingAccountId)
        assertEquals("acc-f", tx.responsibleAccountId)
    }

    @Test
    fun testSkipOccurrenceDoesNotRegisterTransaction() = runTest {
        val scheduledAt = LocalDate.of(2026, 11, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val occ = RecurrenceOccurrenceEntity(
            id = "occ-2",
            ruleId = "rule-1",
            status = OccurrenceStatus.PENDING,
            scheduledAt = scheduledAt,
            amountMinor = 4500L,
            type = TransactionType.EXPENSE,
            payingAccountId = "acc-p",
            responsibleAccountId = "acc-p",
            categoryId = "cat-1",
            note = "Omitir esta",
            transactionId = null,
            createdAt = scheduledAt,
        )
        occurrenceDao.upsert(occ)

        useCase.skipOccurrence("occ-2")

        val updated = occurrenceDao.getOccurrenceById("occ-2")
        assertEquals(OccurrenceStatus.SKIPPED, updated?.status)
        assertNull(updated?.transactionId)

        assertEquals(0, transactions.size)
    }

    @Test
    fun testConfirmMultipleOccurrencesAtomically() = runTest {
        val date1 = LocalDate.of(2026, 10, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val date2 = LocalDate.of(2026, 11, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val occ1 = RecurrenceOccurrenceEntity("occ-1", "rule-1", OccurrenceStatus.PENDING, date1, 2000L, TransactionType.EXPENSE, "acc-p", "acc-p", "cat-1", null, null, date1)
        val occ2 = RecurrenceOccurrenceEntity("occ-2", "rule-1", OccurrenceStatus.PENDING, date2, 2000L, TransactionType.EXPENSE, "acc-p", "acc-p", "cat-1", null, null, date2)

        occurrenceDao.upsert(occ1)
        occurrenceDao.upsert(occ2)

        useCase.confirmMultipleOccurrences(listOf("occ-1", "occ-2"))

        assertEquals(OccurrenceStatus.CONFIRMED, occurrenceDao.getOccurrenceById("occ-1")?.status)
        assertEquals(OccurrenceStatus.CONFIRMED, occurrenceDao.getOccurrenceById("occ-2")?.status)
        assertEquals(2, transactions.size)
    }

    @Test
    fun testPostponeOccurrenceUpdatesReviewAtWithoutChangingScheduledAtAndFiltersPendingBeforeReviewAt() = runTest {
        val now = LocalDate.of(2026, 11, 22).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val scheduledAt = LocalDate.of(2026, 11, 20).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val newReviewAt = LocalDate.of(2026, 11, 29).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val occ = RecurrenceOccurrenceEntity(
            id = "occ-postpone-1",
            ruleId = "rule-1",
            status = OccurrenceStatus.PENDING,
            scheduledAt = scheduledAt,
            amountMinor = 3000L,
            type = TransactionType.EXPENSE,
            payingAccountId = "acc-p",
            responsibleAccountId = "acc-p",
            categoryId = "cat-1",
            note = "Posponer me",
            transactionId = null,
            createdAt = scheduledAt,
        )
        occurrenceDao.upsert(occ)

        useCase.postponeOccurrence("occ-postpone-1", newReviewAt)

        val updated = occurrenceDao.getOccurrenceById("occ-postpone-1")
        assertNotNull(updated)
        assertEquals(scheduledAt, updated?.scheduledAt) // scheduledAt economic date is preserved
        assertEquals(newReviewAt, updated?.reviewAt) // reviewAt is updated

        // Filtered out from pending before newReviewAt
        val pendingListBeforeReview = occurrenceDao.getPendingOccurrences(now)
        assertTrue(pendingListBeforeReview.none { it.id == "occ-postpone-1" })

        // Included after reviewAt
        val pendingListAfterReview = occurrenceDao.getPendingOccurrences(newReviewAt + 1)
        assertTrue(pendingListAfterReview.any { it.id == "occ-postpone-1" })
    }
}

private class FakePendingOccurrenceDao : RecurrenceOccurrenceDao {
    private val occurrences = mutableMapOf<String, RecurrenceOccurrenceEntity>()

    override suspend fun upsert(occurrence: RecurrenceOccurrenceEntity) {
        occurrences[occurrence.id] = occurrence
    }

    override suspend fun upsertAll(occurrences: List<RecurrenceOccurrenceEntity>) {
        occurrences.forEach { upsert(it) }
    }

    override suspend fun getOccurrenceById(id: String): RecurrenceOccurrenceEntity? = occurrences[id]

    override fun observeOccurrencesByRule(ruleId: String): Flow<List<RecurrenceOccurrenceEntity>> {
        return flowOf(occurrences.values.filter { it.ruleId == ruleId })
    }

    override fun observePendingOccurrences(now: Long): Flow<List<RecurrenceOccurrenceEntity>> {
        return flowOf(occurrences.values.filter { it.status == OccurrenceStatus.PENDING && (it.reviewAt == null || it.reviewAt!! <= now) })
    }

    override suspend fun getPendingOccurrences(now: Long): List<RecurrenceOccurrenceEntity> {
        return occurrences.values.filter { it.status == OccurrenceStatus.PENDING && (it.reviewAt == null || it.reviewAt!! <= now) }
    }

    override suspend fun getAllOccurrences(): List<RecurrenceOccurrenceEntity> {
        return occurrences.values.toList()
    }

    override suspend fun updateStatus(id: String, status: OccurrenceStatus, transactionId: String?) {
        occurrences[id]?.let {
            occurrences[id] = it.copy(status = status, transactionId = transactionId)
        }
    }

    override suspend fun hasOccurrenceForDate(ruleId: String, scheduledAt: Long): Boolean {
        return occurrences.values.any { it.ruleId == ruleId && it.scheduledAt == scheduledAt }
    }
}
