package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

enum class AnalysisSeriesType {
    BALANCE,
    INCOMES,
    EXPENSES,
}

data class AnalysisCategorySlice(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val amountMinor: Long,
    val percentage: Float,
    val transactionCount: Int,
)

data class AnalysisEvolutionBucket(
    val label: String,
    val fullLabel: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val amountMinor: Long?, // null => periodo futuro vacío (no cero ficticio)
    val transactionCount: Int,
) {
    val isFuture: Boolean get() = amountMinor == null
}

data class AnalysisResult(
    val totalMinor: Long,
    val averagePerBucketMinor: Long,
    val transactionCount: Int,
    val categorySlices: List<AnalysisCategorySlice>,
    val evolutionBuckets: List<AnalysisEvolutionBucket>,
    val totalIncomesMinor: Long = 0L,
    val totalExpensesMinor: Long = 0L,
)

class ComputeAnalysisUseCase @Inject constructor(
    private val filterMovementsUseCase: FilterMovementsUseCase,
) {

    fun compute(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        accountFilter: MovementAccountFilter,
        periodFilter: MovementPeriodFilter,
        seriesType: AnalysisSeriesType,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): AnalysisResult {
        val (periodStart, periodEnd) = filterMovementsUseCase.resolveDateRange(periodFilter)

        // Excluir TransactionType.ADJUSTMENT; transferencias y ajustes de saldo no existen aquí
        val matchingTransactions = transactions.filter { tx ->
            val matchesType = when (seriesType) {
                AnalysisSeriesType.EXPENSES -> tx.type == TransactionType.EXPENSE
                AnalysisSeriesType.INCOMES -> tx.type == TransactionType.INCOME
                AnalysisSeriesType.BALANCE -> tx.type == TransactionType.INCOME || tx.type == TransactionType.EXPENSE
            }
            if (!matchesType) return@filter false
            val matchesAccount = when (accountFilter) {
                MovementAccountFilter.All -> true
                is MovementAccountFilter.Account -> tx.responsibleAccountId == accountFilter.accountId
            }
            if (!matchesAccount) return@filter false
            val txDate = Instant.ofEpochMilli(tx.occurredAt).atZone(zoneId).toLocalDate()
            !txDate.isBefore(periodStart) && !txDate.isAfter(periodEnd)
        }

        val totalIncomesMinor = matchingTransactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amountMinor }
        val totalExpensesMinor = matchingTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amountMinor }

        val totalMinor = when (seriesType) {
            AnalysisSeriesType.EXPENSES -> totalExpensesMinor
            AnalysisSeriesType.INCOMES -> totalIncomesMinor
            AnalysisSeriesType.BALANCE -> totalIncomesMinor - totalExpensesMinor
        }
        val grossTotalMinor = if (seriesType == AnalysisSeriesType.BALANCE) {
            totalIncomesMinor + totalExpensesMinor
        } else {
            totalMinor
        }
        val categoryMap = categories.associateBy { it.id }

        val categorySlices = matchingTransactions
            .groupBy { it.categoryId }
            .map { (categoryId, txs) ->
                val cat = categoryMap[categoryId]
                val sum = if (seriesType == AnalysisSeriesType.BALANCE) {
                    txs.sumOf { if (it.type == TransactionType.INCOME) it.amountMinor else -it.amountMinor }
                } else {
                    txs.sumOf { it.amountMinor }
                }
                val pct = if (grossTotalMinor > 0L) {
                    (abs(sum).toDouble() * 100.0 / grossTotalMinor.toDouble()).toFloat()
                } else {
                    0f
                }
                AnalysisCategorySlice(
                    categoryId = categoryId,
                    categoryName = cat?.name ?: "Sin categoría",
                    categoryIcon = cat?.icon ?: "category",
                    amountMinor = sum,
                    percentage = pct,
                    transactionCount = txs.size,
                )
            }
            .sortedWith(
                compareByDescending<AnalysisCategorySlice> { abs(it.amountMinor) }
                    .thenBy { it.categoryName },
            )

        val bucketsSpec = buildBuckets(periodFilter, periodStart, periodEnd)
        val txsWithDate = matchingTransactions.map { tx ->
            tx to Instant.ofEpochMilli(tx.occurredAt).atZone(zoneId).toLocalDate()
        }

        val evolutionBuckets = bucketsSpec.map { spec ->
            if (spec.startDate.isAfter(today)) {
                AnalysisEvolutionBucket(
                    label = spec.label,
                    fullLabel = spec.fullLabel,
                    startDate = spec.startDate,
                    endDate = spec.endDate,
                    amountMinor = null,
                    transactionCount = 0,
                )
            } else {
                val bucketTxs = txsWithDate.filter { (_, date) ->
                    !date.isBefore(spec.startDate) && !date.isAfter(spec.endDate)
                }
                val bucketAmount = if (seriesType == AnalysisSeriesType.BALANCE) {
                    bucketTxs.sumOf { (tx, _) ->
                        if (tx.type == TransactionType.INCOME) tx.amountMinor else -tx.amountMinor
                    }
                } else {
                    bucketTxs.sumOf { (tx, _) -> tx.amountMinor }
                }
                AnalysisEvolutionBucket(
                    label = spec.label,
                    fullLabel = spec.fullLabel,
                    startDate = spec.startDate,
                    endDate = spec.endDate,
                    amountMinor = bucketAmount,
                    transactionCount = bucketTxs.size,
                )
            }
        }

        val elapsedBuckets = evolutionBuckets.filter { !it.isFuture }
        val averageMinor = if (elapsedBuckets.isNotEmpty()) {
            totalMinor / elapsedBuckets.size
        } else {
            0L
        }

        return AnalysisResult(
            totalMinor = totalMinor,
            averagePerBucketMinor = averageMinor,
            transactionCount = matchingTransactions.size,
            categorySlices = categorySlices,
            evolutionBuckets = evolutionBuckets,
            totalIncomesMinor = totalIncomesMinor,
            totalExpensesMinor = totalExpensesMinor,
        )
    }

    private data class BucketSpec(
        val label: String,
        val fullLabel: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
    )

    private fun buildBuckets(
        periodFilter: MovementPeriodFilter,
        periodStart: LocalDate,
        periodEnd: LocalDate,
    ): List<BucketSpec> {
        val esLocale = Locale("es", "ES")
        val shortMonthFormatter = DateTimeFormatter.ofPattern("MMM", esLocale)
        val fullMonthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", esLocale)
        val shortDateFormatter = DateTimeFormatter.ofPattern("d/M", esLocale)
        val fullDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", esLocale)

        return when (periodFilter) {
            is MovementPeriodFilter.Month -> {
                val days = periodFilter.yearMonth.lengthOfMonth()
                (1..days).map { day ->
                    val date = periodFilter.yearMonth.atDay(day)
                    BucketSpec(
                        label = day.toString(),
                        fullLabel = date.format(fullDateFormatter),
                        startDate = date,
                        endDate = date,
                    )
                }
            }
            is MovementPeriodFilter.Last12Months -> {
                val endMonth = YearMonth.from(periodFilter.referenceDate)
                val startMonth = endMonth.minusMonths(11)
                (0..11).map { offset ->
                    val ym = startMonth.plusMonths(offset.toLong())
                    val start = ym.atDay(1)
                    val end = minOf(ym.atEndOfMonth(), periodFilter.referenceDate)
                    BucketSpec(
                        label = ym.format(shortMonthFormatter).replace(".", "").replaceFirstChar { it.uppercase() },
                        fullLabel = ym.format(fullMonthFormatter).replaceFirstChar { it.uppercase() },
                        startDate = start,
                        endDate = end,
                    )
                }
            }
            is MovementPeriodFilter.Year -> {
                (1..12).map { month ->
                    val ym = YearMonth.of(periodFilter.year, month)
                    BucketSpec(
                        label = ym.format(shortMonthFormatter).replace(".", "").replaceFirstChar { it.uppercase() },
                        fullLabel = ym.format(fullMonthFormatter).replaceFirstChar { it.uppercase() },
                        startDate = ym.atDay(1),
                        endDate = ym.atEndOfMonth(),
                    )
                }
            }
            is MovementPeriodFilter.Custom -> {
                val spanDays = ChronoUnit.DAYS.between(periodStart, periodEnd) + 1
                when {
                    spanDays <= 31L -> {
                        (0 until spanDays.toInt()).map { offset ->
                            val date = periodStart.plusDays(offset.toLong())
                            BucketSpec(
                                label = date.format(shortDateFormatter),
                                fullLabel = date.format(fullDateFormatter),
                                startDate = date,
                                endDate = date,
                            )
                        }
                    }
                    spanDays <= 120L -> {
                        val buckets = mutableListOf<BucketSpec>()
                        var cursor = periodStart
                        while (!cursor.isAfter(periodEnd)) {
                            val chunkEnd = minOf(cursor.plusDays(6), periodEnd)
                            buckets += BucketSpec(
                                label = cursor.format(shortDateFormatter),
                                fullLabel = "${cursor.format(fullDateFormatter)} – ${chunkEnd.format(fullDateFormatter)}",
                                startDate = cursor,
                                endDate = chunkEnd,
                            )
                            cursor = chunkEnd.plusDays(1)
                        }
                        buckets
                    }
                    spanDays <= 730L -> {
                        val startMonth = YearMonth.from(periodStart)
                        val endMonth = YearMonth.from(periodEnd)
                        val buckets = mutableListOf<BucketSpec>()
                        var ym = startMonth
                        val monthYearShort = DateTimeFormatter.ofPattern("MMM yy", esLocale)
                        while (!ym.isAfter(endMonth)) {
                            val bStart = maxOf(ym.atDay(1), periodStart)
                            val bEnd = minOf(ym.atEndOfMonth(), periodEnd)
                            buckets += BucketSpec(
                                label = ym.format(monthYearShort).replace(".", "").replaceFirstChar { it.uppercase() },
                                fullLabel = ym.format(fullMonthFormatter).replaceFirstChar { it.uppercase() },
                                startDate = bStart,
                                endDate = bEnd,
                            )
                            ym = ym.plusMonths(1)
                        }
                        buckets
                    }
                    else -> {
                        (periodStart.year..periodEnd.year).map { yr ->
                            val bStart = maxOf(LocalDate.of(yr, 1, 1), periodStart)
                            val bEnd = minOf(LocalDate.of(yr, 12, 31), periodEnd)
                            BucketSpec(
                                label = yr.toString(),
                                fullLabel = "Año $yr",
                                startDate = bStart,
                                endDate = bEnd,
                            )
                        }
                    }
                }
            }
        }
    }
}
