package com.familyexpenses.app.domain.rule

import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RecurrenceCalculatorTest {

    private val calculator = RecurrenceCalculator()
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun testWeeklyNextOccurrence() {
        val startDate = LocalDate.of(2026, 11, 23) // A Monday
        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rule = RecurrenceRuleEntity(
            id = "rule-weekly",
            type = TransactionType.EXPENSE,
            payingAccountId = "p",
            responsibleAccountId = "r",
            categoryId = "c",
            amountMinor = 1000L,
            frequency = RecurrenceFrequency.WEEKLY,
            dayOfWeek = DayOfWeek.FRIDAY.value, // 5
            startAt = startMillis,
            nextOccurrenceAt = startMillis,
            createdAt = startMillis,
        )

        val first = calculator.calculateFirstOccurrence(rule, zoneId)
        assertNotNull(first)
        val firstDate = Instant.ofEpochMilli(first!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 11, 27), firstDate) // First Friday on or after Nov 23 is Nov 27

        val next = calculator.calculateNextOccurrence(rule, first, zoneId)
        assertNotNull(next)
        val nextDate = Instant.ofEpochMilli(next!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 12, 4), nextDate) // Next Friday
    }

    @Test
    fun testMonthlyEndOfMonthClamping() {
        // Nominal day 31
        val startDate = LocalDate.of(2026, 1, 31)
        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rule = RecurrenceRuleEntity(
            id = "rule-monthly",
            type = TransactionType.EXPENSE,
            payingAccountId = "p",
            responsibleAccountId = "r",
            categoryId = "c",
            amountMinor = 1000L,
            frequency = RecurrenceFrequency.MONTHLY,
            dayOfMonth = 31,
            startAt = startMillis,
            nextOccurrenceAt = startMillis,
            createdAt = startMillis,
        )

        val febMillis = calculator.calculateNextOccurrence(rule, startMillis, zoneId)
        assertNotNull(febMillis)
        val febDate = Instant.ofEpochMilli(febMillis!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 2, 28), febDate) // Clamped to Feb 28 in non-leap year

        val marMillis = calculator.calculateNextOccurrence(rule, febMillis, zoneId)
        assertNotNull(marMillis)
        val marDate = Instant.ofEpochMilli(marMillis!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 3, 31), marDate) // Returns back to day 31 in March!
    }

    @Test
    fun testMultiMonthRecurrence() {
        val startDate = LocalDate.of(2026, 1, 15)
        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rule = RecurrenceRuleEntity(
            id = "rule-multi",
            type = TransactionType.EXPENSE,
            payingAccountId = "p",
            responsibleAccountId = "r",
            categoryId = "c",
            amountMinor = 1000L,
            frequency = RecurrenceFrequency.MULTI_MONTH,
            intervalCount = 3, // Every 3 months (quarterly)
            dayOfMonth = 15,
            startAt = startMillis,
            nextOccurrenceAt = startMillis,
            createdAt = startMillis,
        )

        val first = calculator.calculateFirstOccurrence(rule, zoneId)
        assertNotNull(first)
        val firstDate = Instant.ofEpochMilli(first!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 1, 15), firstDate)

        val next1 = calculator.calculateNextOccurrence(rule, first, zoneId)
        assertNotNull(next1)
        val next1Date = Instant.ofEpochMilli(next1!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 4, 15), next1Date)

        val next2 = calculator.calculateNextOccurrence(rule, next1, zoneId)
        assertNotNull(next2)
        val next2Date = Instant.ofEpochMilli(next2!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2026, 7, 15), next2Date)
    }

    @Test
    fun testYearlyLeapYearClamping() {
        // Feb 29 on leap year 2024
        val startDate = LocalDate.of(2024, 2, 29)
        val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()

        val rule = RecurrenceRuleEntity(
            id = "rule-yearly",
            type = TransactionType.EXPENSE,
            payingAccountId = "p",
            responsibleAccountId = "r",
            categoryId = "c",
            amountMinor = 1000L,
            frequency = RecurrenceFrequency.YEARLY,
            monthOfYear = 2,
            dayOfMonth = 29,
            startAt = startMillis,
            nextOccurrenceAt = startMillis,
            createdAt = startMillis,
        )

        val next2025 = calculator.calculateNextOccurrence(rule, startMillis, zoneId)
        assertNotNull(next2025)
        val date2025 = Instant.ofEpochMilli(next2025!!).atZone(zoneId).toLocalDate()
        assertEquals(LocalDate.of(2025, 2, 28), date2025) // Clamped to Feb 28 in 2025
    }
}
