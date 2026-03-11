package com.familyexpenses.app.domain.rule

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class MonthlyRecurrenceRule @Inject constructor() {

    fun nextOccurrenceAfter(occurrenceAt: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val nextDate = Instant.ofEpochMilli(occurrenceAt)
            .atZone(zoneId)
            .toLocalDate()
            .withDayOfMonth(1)
            .plusMonths(1)
        return nextDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun firstOccurrenceFromToday(today: LocalDate = LocalDate.now(), zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val firstDayThisMonth = today.withDayOfMonth(1)
        val target = if (today.dayOfMonth == 1) firstDayThisMonth else firstDayThisMonth.plusMonths(1)
        return target.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun firstOccurrenceOnOrAfter(startAt: Long, zoneId: ZoneId = ZoneId.systemDefault()): Long {
        val startDate = Instant.ofEpochMilli(startAt).atZone(zoneId).toLocalDate()
        val candidate = startDate.withDayOfMonth(1)
        val target = if (startDate.dayOfMonth == 1) candidate else candidate.plusMonths(1)
        return target.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun collectDueOccurrences(
        nextOccurrenceAt: Long,
        now: Long,
        endAt: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): DueOccurrences {
        val dueOccurrences = mutableListOf<Long>()
        var cursor = nextOccurrenceAt
        while (cursor <= now && (endAt == null || cursor <= endAt)) {
            dueOccurrences += cursor
            cursor = nextOccurrenceAfter(cursor, zoneId)
        }
        return DueOccurrences(
            dueOccurrences = dueOccurrences,
            nextOccurrenceAt = cursor,
            hasReachedEnd = endAt != null && cursor > endAt,
        )
    }
}

data class DueOccurrences(
    val dueOccurrences: List<Long>,
    val nextOccurrenceAt: Long,
    val hasReachedEnd: Boolean,
)
