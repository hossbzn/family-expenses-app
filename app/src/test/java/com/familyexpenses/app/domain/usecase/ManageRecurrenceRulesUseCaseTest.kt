package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.RecurrenceExecutionMode
import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.RecurrenceRuleStatus
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.dao.RecurrenceRuleDao
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.domain.rule.RecurrenceCalculator
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ManageRecurrenceRulesUseCaseTest {

    private lateinit var dao: FakeRecurrenceRuleDao
    private lateinit var calculator: RecurrenceCalculator
    private lateinit var useCase: ManageRecurrenceRulesUseCase
    private val zoneId = ZoneId.of("UTC")

    @Before
    fun setUp() {
        dao = FakeRecurrenceRuleDao()
        calculator = RecurrenceCalculator()
        useCase = ManageRecurrenceRulesUseCase(dao, calculator)
    }

    @Test
    fun testCreateRuleCalculatesFirstOccurrenceAndStatus() = runTest {
        val startMillis = LocalDate.of(2026, 11, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val rule = RecurrenceRuleEntity(
            id = "rule-1",
            type = TransactionType.EXPENSE,
            payingAccountId = "p",
            responsibleAccountId = "r",
            categoryId = "c",
            amountMinor = 5000L,
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 1,
            mode = RecurrenceExecutionMode.AUTOMATIC,
            status = RecurrenceRuleStatus.ACTIVE,
            startAt = startMillis,
            nextOccurrenceAt = 0L,
            createdAt = startMillis,
        )

        val id = useCase.createRule(rule, zoneId)
        val created = dao.getRuleById(id)

        assertEquals("rule-1", created?.id)
        assertEquals(RecurrenceRuleStatus.ACTIVE, created?.status)
        assertEquals(startMillis, created?.nextOccurrenceAt)
    }

    @Test
    fun testPauseAndResumeRule() = runTest {
        val startMillis = LocalDate.of(2026, 11, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val rule = RecurrenceRuleEntity(
            id = "rule-1",
            type = TransactionType.EXPENSE,
            payingAccountId = "p",
            responsibleAccountId = "r",
            categoryId = "c",
            amountMinor = 5000L,
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 1,
            mode = RecurrenceExecutionMode.AUTOMATIC,
            status = RecurrenceRuleStatus.ACTIVE,
            startAt = startMillis,
            nextOccurrenceAt = startMillis,
            createdAt = startMillis,
        )
        dao.upsert(rule)

        useCase.pauseRule("rule-1")
        assertEquals(RecurrenceRuleStatus.PAUSED, dao.getRuleById("rule-1")?.status)

        useCase.resumeRule("rule-1", zoneId)
        assertEquals(RecurrenceRuleStatus.ACTIVE, dao.getRuleById("rule-1")?.status)
    }

    @Test
    fun testArchiveAndRestoreRule() = runTest {
        val startMillis = LocalDate.of(2026, 11, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        val rule = RecurrenceRuleEntity(
            id = "rule-1",
            type = TransactionType.EXPENSE,
            payingAccountId = "p",
            responsibleAccountId = "r",
            categoryId = "c",
            amountMinor = 5000L,
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 1,
            mode = RecurrenceExecutionMode.AUTOMATIC,
            status = RecurrenceRuleStatus.ACTIVE,
            startAt = startMillis,
            nextOccurrenceAt = startMillis,
            createdAt = startMillis,
        )
        dao.upsert(rule)

        useCase.archiveRule("rule-1")
        assertEquals(RecurrenceRuleStatus.ARCHIVED, dao.getRuleById("rule-1")?.status)

        val newStartMillis = LocalDate.of(2026, 12, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        useCase.restoreRule("rule-1", newStartMillis, zoneId)

        val restored = dao.getRuleById("rule-1")
        assertEquals(RecurrenceRuleStatus.ACTIVE, restored?.status)
        assertEquals(newStartMillis, restored?.startAt)
    }
}

private class FakeRecurrenceRuleDao : RecurrenceRuleDao {
    private val rules = mutableMapOf<String, RecurrenceRuleEntity>()

    override suspend fun upsert(rule: RecurrenceRuleEntity) {
        rules[rule.id] = rule
    }

    override suspend fun getActiveRules(): List<RecurrenceRuleEntity> {
        return rules.values.filter { it.status == RecurrenceRuleStatus.ACTIVE }
    }

    override fun observeRules(): Flow<List<RecurrenceRuleEntity>> {
        return flowOf(rules.values.toList())
    }

    override fun observeRulesByStatus(status: RecurrenceRuleStatus): Flow<List<RecurrenceRuleEntity>> {
        return flowOf(rules.values.filter { it.status == status })
    }

    override suspend fun getRuleById(ruleId: String): RecurrenceRuleEntity? {
        return rules[ruleId]
    }

    override suspend fun updateNextOccurrence(ruleId: String, nextOccurrenceAt: Long) {
        rules[ruleId]?.let {
            rules[ruleId] = it.copy(nextOccurrenceAt = nextOccurrenceAt)
        }
    }

    override suspend fun updateStatus(ruleId: String, status: RecurrenceRuleStatus) {
        rules[ruleId]?.let {
            rules[ruleId] = it.copy(status = status)
        }
    }
}
