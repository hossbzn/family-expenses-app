package com.familyexpenses.app.domain.rule

import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

class RecurrenceCalculator @Inject constructor() {

    fun calculateNextOccurrence(
        rule: RecurrenceRuleEntity,
        afterTimestamp: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val afterDate = Instant.ofEpochMilli(afterTimestamp).atZone(zoneId).toLocalDate()
        val startDate = Instant.ofEpochMilli(rule.startAt).atZone(zoneId).toLocalDate()
        val endDate = rule.endAt?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }

        val candidate = computeNextDate(rule, startDate, afterDate) ?: return null

        if (endDate != null && candidate.isAfter(endDate)) {
            return null
        }

        return candidate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun calculateFirstOccurrence(
        rule: RecurrenceRuleEntity,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        val startDate = Instant.ofEpochMilli(rule.startAt).atZone(zoneId).toLocalDate()
        val endDate = rule.endAt?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate() }

        val candidate = computeFirstDateOnOrAfter(rule, startDate) ?: return null

        if (endDate != null && candidate.isAfter(endDate)) {
            return null
        }

        return candidate.atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    private fun computeNextDate(
        rule: RecurrenceRuleEntity,
        startDate: LocalDate,
        afterDate: LocalDate,
    ): LocalDate? {
        return when (rule.frequency) {
            RecurrenceFrequency.WEEKLY -> {
                val targetDay = rule.dayOfWeek?.let { DayOfWeek.of(it) } ?: startDate.dayOfWeek
                var next = afterDate.plusDays(1)
                while (next.dayOfWeek != targetDay) {
                    next = next.plusDays(1)
                }
                next
            }
            RecurrenceFrequency.MONTHLY -> {
                val nominalDay = rule.dayOfMonth ?: startDate.dayOfMonth
                var nextMonth = afterDate.toYearMonth().plusMonths(1)
                clampDayOfMonth(nextMonth, nominalDay)
            }
            RecurrenceFrequency.MULTI_MONTH -> {
                val interval = (rule.intervalCount ?: 1).coerceAtLeast(1)
                val nominalDay = rule.dayOfMonth ?: startDate.dayOfMonth
                val startYM = startDate.toYearMonth()

                var currentYM = afterDate.toYearMonth()
                if (currentYM.isBefore(startYM)) {
                    currentYM = startYM
                }

                var monthsDiff = (currentYM.year - startYM.year) * 12 + (currentYM.monthValue - startYM.monthValue)
                var remainder = monthsDiff % interval
                if (remainder < 0) remainder += interval

                var nextYM = currentYM.plusMonths((interval - remainder).toLong())
                var candidate = clampDayOfMonth(nextYM, nominalDay)

                if (!candidate.isAfter(afterDate)) {
                    nextYM = nextYM.plusMonths(interval.toLong())
                    candidate = clampDayOfMonth(nextYM, nominalDay)
                }
                candidate
            }
            RecurrenceFrequency.YEARLY -> {
                val nominalDay = rule.dayOfMonth ?: startDate.dayOfMonth
                val nominalMonth = rule.monthOfYear ?: startDate.monthValue
                var candidateYear = afterDate.year

                var candidate = clampYearlyDate(candidateYear, nominalMonth, nominalDay)
                if (!candidate.isAfter(afterDate)) {
                    candidateYear++
                    candidate = clampYearlyDate(candidateYear, nominalMonth, nominalDay)
                }
                candidate
            }
        }
    }

    private fun computeFirstDateOnOrAfter(
        rule: RecurrenceRuleEntity,
        startDate: LocalDate,
    ): LocalDate? {
        return when (rule.frequency) {
            RecurrenceFrequency.WEEKLY -> {
                val targetDay = rule.dayOfWeek?.let { DayOfWeek.of(it) } ?: startDate.dayOfWeek
                var date = startDate
                while (date.dayOfWeek != targetDay) {
                    date = date.plusDays(1)
                }
                date
            }
            RecurrenceFrequency.MONTHLY -> {
                val nominalDay = rule.dayOfMonth ?: startDate.dayOfMonth
                val startClamped = clampDayOfMonth(startDate.toYearMonth(), nominalDay)
                if (!startClamped.isBefore(startDate)) {
                    startClamped
                } else {
                    clampDayOfMonth(startDate.toYearMonth().plusMonths(1), nominalDay)
                }
            }
            RecurrenceFrequency.MULTI_MONTH -> {
                val nominalDay = rule.dayOfMonth ?: startDate.dayOfMonth
                val startClamped = clampDayOfMonth(startDate.toYearMonth(), nominalDay)
                if (!startClamped.isBefore(startDate)) {
                    startClamped
                } else {
                    val interval = (rule.intervalCount ?: 1).coerceAtLeast(1)
                    clampDayOfMonth(startDate.toYearMonth().plusMonths(interval.toLong()), nominalDay)
                }
            }
            RecurrenceFrequency.YEARLY -> {
                val nominalDay = rule.dayOfMonth ?: startDate.dayOfMonth
                val nominalMonth = rule.monthOfYear ?: startDate.monthValue
                val startClamped = clampYearlyDate(startDate.year, nominalMonth, nominalDay)
                if (!startClamped.isBefore(startDate)) {
                    startClamped
                } else {
                    clampYearlyDate(startDate.year + 1, nominalMonth, nominalDay)
                }
            }
        }
    }

    private fun clampDayOfMonth(yearMonth: YearMonth, nominalDay: Int): LocalDate {
        val maxDay = yearMonth.lengthOfMonth()
        val actualDay = nominalDay.coerceIn(1, maxDay)
        return yearMonth.atDay(actualDay)
    }

    private fun clampYearlyDate(year: Int, month: Int, nominalDay: Int): LocalDate {
        val yearMonth = YearMonth.of(year, month)
        return clampDayOfMonth(yearMonth, nominalDay)
    }

    private fun LocalDate.toYearMonth(): YearMonth = YearMonth.of(year, monthValue)
}
