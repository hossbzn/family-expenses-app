package com.familyexpenses.app.data.local

import com.familyexpenses.app.core.model.OccurrenceStatus
import com.familyexpenses.app.core.model.RecurrenceExecutionMode
import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.RecurrenceRuleStatus
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecurrenceSchemaTest {

    @Test
    fun testRecurrenceRuleEntityCreation() {
        val rule = RecurrenceRuleEntity(
            id = "rule-123",
            type = TransactionType.EXPENSE,
            payingAccountId = "acc-1",
            responsibleAccountId = "acc-2",
            categoryId = "cat-1",
            amountMinor = 15000L,
            note = "Suscripción",
            frequency = RecurrenceFrequency.MULTI_MONTH,
            intervalCount = 3,
            dayOfMonth = 15,
            mode = RecurrenceExecutionMode.PENDING,
            status = RecurrenceRuleStatus.ACTIVE,
            startAt = 1000000000L,
            endAt = null,
            nextOccurrenceAt = 1000000000L,
            createdAt = 1000000000L,
        )

        assertEquals("rule-123", rule.id)
        assertEquals(RecurrenceFrequency.MULTI_MONTH, rule.frequency)
        assertEquals(3, rule.intervalCount)
        assertEquals(15, rule.dayOfMonth)
        assertEquals(RecurrenceExecutionMode.PENDING, rule.mode)
        assertEquals(RecurrenceRuleStatus.ACTIVE, rule.status)
        assertNull(rule.endAt)
    }

    @Test
    fun testRecurrenceOccurrenceEntityCreation() {
        val occurrence = RecurrenceOccurrenceEntity(
            id = "occ-123",
            ruleId = "rule-123",
            status = OccurrenceStatus.PENDING,
            scheduledAt = 1000000000L,
            amountMinor = 15000L,
            type = TransactionType.EXPENSE,
            payingAccountId = "acc-1",
            responsibleAccountId = "acc-2",
            categoryId = "cat-1",
            note = "Suscripción",
            transactionId = null,
            createdAt = 1000000000L,
        )

        assertEquals("occ-123", occurrence.id)
        assertEquals("rule-123", occurrence.ruleId)
        assertEquals(OccurrenceStatus.PENDING, occurrence.status)
        assertNull(occurrence.transactionId)
    }
}
