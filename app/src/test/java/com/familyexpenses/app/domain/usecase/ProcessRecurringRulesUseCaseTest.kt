package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.OccurrenceStatus
import com.familyexpenses.app.core.model.RecurrenceExecutionMode
import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.RecurrenceRuleStatus
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.TransactionRunner
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.RecurrenceOccurrenceDao
import com.familyexpenses.app.data.local.dao.RecurrenceRuleDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.domain.rule.RecurrenceCalculator
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ProcessRecurringRulesUseCaseTest {

    private val zoneId = ZoneId.of("UTC")
    private lateinit var transactionRunner: TransactionRunner
    private lateinit var ruleDao: FakeProcessRuleDao
    private lateinit var occurrenceDao: FakeProcessOccurrenceDao
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
        override suspend fun upsertAll(accounts: List<AccountEntity>) { this@ProcessRecurringRulesUseCaseTest.accounts.addAll(accounts) }
        override fun observeAccounts(): Flow<List<AccountEntity>> = flowOf(accounts)
        override suspend fun getAllAccounts(): List<AccountEntity> = accounts
        override suspend fun getAccountById(accountId: String): AccountEntity? = accounts.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accounts.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {}
    }

    private val fakeCategoryDao = object : CategoryDao {
        override suspend fun upsert(category: CategoryEntity) { categories.add(category) }
        override suspend fun upsertAll(categories: List<CategoryEntity>) { this@ProcessRecurringRulesUseCaseTest.categories.addAll(categories) }
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

    private lateinit var addExpenseUseCase: AddExpenseUseCase
    private lateinit var addIncomeUseCase: AddIncomeUseCase
    private lateinit var calculator: RecurrenceCalculator
    private lateinit var processUseCase: ProcessRecurringRulesUseCase

    @Before
    fun setUp() {
        transactionRunner = object : TransactionRunner {
            override suspend fun <R> invoke(block: suspend () -> R): R = block()
        }
        ruleDao = FakeProcessRuleDao()
        occurrenceDao = FakeProcessOccurrenceDao()
        transactions.clear()

        val accountRepo = AccountRepository(fakeAccountDao)
        val categoryRepo = CategoryRepository(fakeCategoryDao)
        val expenseRepo = ExpenseRepository(fakeTransactionDao)

        addExpenseUseCase = AddExpenseUseCase(accountRepo, categoryRepo, expenseRepo)
        addIncomeUseCase = AddIncomeUseCase(accountRepo, categoryRepo, expenseRepo)
        calculator = RecurrenceCalculator()

        processUseCase = ProcessRecurringRulesUseCase(
            transactionRunner = transactionRunner,
            recurrenceRuleDao = ruleDao,
            recurrenceOccurrenceDao = occurrenceDao,
            addExpenseUseCase = addExpenseUseCase,
            addIncomeUseCase = addIncomeUseCase,
            recurrenceCalculator = calculator,
        )
    }

    @Test
    fun testAutomaticRuleGeneratesTransactionAndConfirmedOccurrence() = runTest {
        val startMillis = LocalDate.of(2026, 11, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nowMillis = LocalDate.of(2026, 11, 2).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rule = RecurrenceRuleEntity(
            id = "rule-auto",
            type = TransactionType.EXPENSE,
            payingAccountId = "acc-p",
            responsibleAccountId = "acc-p",
            categoryId = "cat-1",
            amountMinor = 3000L,
            note = "Luz",
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 1,
            mode = RecurrenceExecutionMode.AUTOMATIC,
            status = RecurrenceRuleStatus.ACTIVE,
            startAt = startMillis,
            nextOccurrenceAt = startMillis,
            createdAt = startMillis,
        )
        ruleDao.upsert(rule)

        processUseCase(now = nowMillis, zoneId = zoneId)

        val occurrences = occurrenceDao.getOccurrences()
        assertEquals(1, occurrences.size)
        val occ = occurrences.first()
        assertEquals(OccurrenceStatus.CONFIRMED, occ.status)
        assertNotNull(occ.transactionId)

        assertEquals(1, transactions.size)
        assertEquals(3000L, transactions.first().amountMinor)
    }

    @Test
    fun testPendingRuleGeneratesPendingOccurrenceWithoutTransaction() = runTest {
        val startMillis = LocalDate.of(2026, 11, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val nowMillis = LocalDate.of(2026, 11, 2).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rule = RecurrenceRuleEntity(
            id = "rule-pending",
            type = TransactionType.EXPENSE,
            payingAccountId = "acc-p",
            responsibleAccountId = "acc-p",
            categoryId = "cat-1",
            amountMinor = 3000L,
            note = "Agua",
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 1,
            mode = RecurrenceExecutionMode.PENDING,
            status = RecurrenceRuleStatus.ACTIVE,
            startAt = startMillis,
            nextOccurrenceAt = startMillis,
            createdAt = startMillis,
        )
        ruleDao.upsert(rule)

        processUseCase(now = nowMillis, zoneId = zoneId)

        val occurrences = occurrenceDao.getOccurrences()
        assertEquals(1, occurrences.size)
        val occ = occurrences.first()
        assertEquals(OccurrenceStatus.PENDING, occ.status)
        assertNull(occ.transactionId)

        assertEquals(0, transactions.size) // Pending occurrence does NOT create a transaction
    }
}

private class FakeProcessRuleDao : RecurrenceRuleDao {
    private val rules = mutableMapOf<String, RecurrenceRuleEntity>()

    override suspend fun upsert(rule: RecurrenceRuleEntity) {
        rules[rule.id] = rule
    }

    override suspend fun getActiveRules(): List<RecurrenceRuleEntity> {
        return rules.values.filter { it.status == RecurrenceRuleStatus.ACTIVE }
    }

    override fun observeRules(): Flow<List<RecurrenceRuleEntity>> = flowOf(rules.values.toList())

    override fun observeRulesByStatus(status: RecurrenceRuleStatus): Flow<List<RecurrenceRuleEntity>> {
        return flowOf(rules.values.filter { it.status == status })
    }

    override suspend fun getRuleById(ruleId: String): RecurrenceRuleEntity? = rules[ruleId]

    override suspend fun updateNextOccurrence(ruleId: String, nextOccurrenceAt: Long) {
        rules[ruleId]?.let { rules[ruleId] = it.copy(nextOccurrenceAt = nextOccurrenceAt) }
    }

    override suspend fun updateStatus(ruleId: String, status: RecurrenceRuleStatus) {
        rules[ruleId]?.let { rules[ruleId] = it.copy(status = status) }
    }
}

private class FakeProcessOccurrenceDao : RecurrenceOccurrenceDao {
    private val occurrences = mutableMapOf<String, RecurrenceOccurrenceEntity>()

    fun getOccurrences(): List<RecurrenceOccurrenceEntity> = occurrences.values.toList()

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

    override fun observePendingOccurrences(): Flow<List<RecurrenceOccurrenceEntity>> {
        return flowOf(occurrences.values.filter { it.status == OccurrenceStatus.PENDING })
    }

    override suspend fun getPendingOccurrences(): List<RecurrenceOccurrenceEntity> {
        return occurrences.values.filter { it.status == OccurrenceStatus.PENDING }
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
