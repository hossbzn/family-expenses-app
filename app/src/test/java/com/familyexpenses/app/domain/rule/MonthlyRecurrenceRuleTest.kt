package com.familyexpenses.app.domain.rule

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthlyRecurrenceRuleTest {

    private val zoneId = ZoneId.of("Europe/Madrid")
    private val rule = MonthlyRecurrenceRule()

    @Test
    fun `returns next month first day`() {
        val marchFirst = LocalDate.of(2026, 3, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val result = rule.nextOccurrenceAfter(marchFirst, zoneId)

        val expected = LocalDate.of(2026, 4, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `collects delayed monthly occurrences`() {
        val januaryFirst = LocalDate.of(2026, 1, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val marchTwentieth = LocalDate.of(2026, 3, 20)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val result = rule.collectDueOccurrences(januaryFirst, marchTwentieth, zoneId = zoneId)

        assertEquals(3, result.dueOccurrences.size)
        val expectedNext = LocalDate.of(2026, 4, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedNext, result.nextOccurrenceAt)
        assertFalse(result.hasReachedEnd)
    }

    @Test
    fun `first occurrence is next month when start date is not day one`() {
        val startAt = LocalDate.of(2026, 9, 15)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val result = rule.firstOccurrenceOnOrAfter(startAt, zoneId)

        val expected = LocalDate.of(2026, 10, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `collects due occurrences only until end date`() {
        val septemberFirst = LocalDate.of(2026, 9, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val endAt = LocalDate.of(2027, 6, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val julyTenth = LocalDate.of(2027, 7, 10)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val result = rule.collectDueOccurrences(
            nextOccurrenceAt = septemberFirst,
            now = julyTenth,
            endAt = endAt,
            zoneId = zoneId,
        )

        assertEquals(10, result.dueOccurrences.size)
        val expectedNext = LocalDate.of(2027, 7, 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        assertEquals(expectedNext, result.nextOccurrenceAt)
        assertTrue(result.hasReachedEnd)
    }
}
