package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.repository.BackupRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs

data class CsvExportResult(
    val csvContent: String,
    val rowCount: Int,
    val fileName: String,
)

class ExportCsvUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {

    suspend fun export(
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): CsvExportResult {
        val snapshot = backupRepository.readSnapshot()
        val accountMap = snapshot.accounts.associateBy { it.id }
        val categoryMap = snapshot.categories.associateBy { it.id }
        val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(zoneId)

        val entries = mutableListOf<CsvEntry>()

        snapshot.transactions.forEach { tx ->
            val tipo = when (tx.type) {
                TransactionType.EXPENSE -> "GASTO"
                TransactionType.INCOME -> "INGRESO"
                TransactionType.ADJUSTMENT -> "AJUSTE_TRANSACCION"
            }
            val signedMinor = when (tx.type) {
                TransactionType.EXPENSE -> -tx.amountMinor
                TransactionType.INCOME, TransactionType.ADJUSTMENT -> tx.amountMinor
            }
            val payingName = accountMap[tx.payingAccountId]?.name ?: tx.payingAccountId
            val responsibleName = accountMap[tx.responsibleAccountId]?.name ?: tx.responsibleAccountId
            val categoryName = categoryMap[tx.categoryId]?.name ?: tx.categoryId
            val isAdvance = tx.type == TransactionType.EXPENSE && tx.payingAccountId != tx.responsibleAccountId

            entries += CsvEntry(
                occurredAt = tx.occurredAt,
                createdAt = tx.createdAt,
                columns = listOf(
                    tx.id,
                    tipo,
                    isoFormatter.format(Instant.ofEpochMilli(tx.occurredAt)),
                    formatMinorDecimal(signedMinor),
                    signedMinor.toString(),
                    payingName,
                    responsibleName,
                    categoryName,
                    if (isAdvance) "SI" else "NO",
                    tx.note.orEmpty(),
                    tx.source.name,
                ),
            )
        }

        snapshot.transfers.forEach { tr ->
            val tipo = when (tr.type) {
                TransferType.NORMAL -> "TRANSFERENCIA"
                TransferType.REIMBURSEMENT -> "DEVOLUCION_ADELANTO"
            }
            val sourceName = accountMap[tr.sourceAccountId]?.name ?: tr.sourceAccountId
            val destName = accountMap[tr.destinationAccountId]?.name ?: tr.destinationAccountId

            entries += CsvEntry(
                occurredAt = tr.occurredAt,
                createdAt = tr.createdAt,
                columns = listOf(
                    tr.id,
                    tipo,
                    isoFormatter.format(Instant.ofEpochMilli(tr.occurredAt)),
                    formatMinorDecimal(tr.amountMinor),
                    tr.amountMinor.toString(),
                    sourceName,
                    destName,
                    "",
                    "NO",
                    tr.note.orEmpty(),
                    "MANUAL",
                ),
            )
        }

        snapshot.balanceAdjustments.forEach { adj ->
            val accountName = accountMap[adj.accountId]?.name ?: adj.accountId
            entries += CsvEntry(
                occurredAt = adj.occurredAt,
                createdAt = adj.createdAt,
                columns = listOf(
                    adj.id,
                    "AJUSTE_SALDO",
                    isoFormatter.format(Instant.ofEpochMilli(adj.occurredAt)),
                    formatMinorDecimal(adj.amountDeltaMinor),
                    adj.amountDeltaMinor.toString(),
                    accountName,
                    accountName,
                    "",
                    "NO",
                    adj.reason,
                    "MANUAL",
                ),
            )
        }

        val sorted = entries.sortedWith(
            compareByDescending<CsvEntry> { it.occurredAt }
                .thenByDescending { it.createdAt },
        )

        val csvLines = buildList {
            add(HEADERS.joinToString(",") { escapeCsv(it) })
            sorted.forEach { entry ->
                require(entry.columns.size == HEADERS.size) {
                    "Número de columnas CSV inválido."
                }
                add(entry.columns.joinToString(",") { escapeCsv(it) })
            }
        }

        val content = csvLines.joinToString("\n")
        validateCsvIntegrity(content, expectedDataRows = sorted.size)

        val datePart = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
            .withZone(zoneId)
            .format(Instant.ofEpochMilli(snapshot.exportedAt))

        return CsvExportResult(
            csvContent = content,
            rowCount = sorted.size,
            fileName = "family-expenses-movimientos-$datePart.csv",
        )
    }

    private fun formatMinorDecimal(minor: Long): String {
        val sign = if (minor < 0L) "-" else ""
        val absMinor = abs(minor)
        val whole = absMinor / 100L
        val cents = (absMinor % 100L).toString().padStart(2, '0')
        return "$sign$whole.$cents"
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')
        if (!needsQuotes) return value
        return "\"${value.replace("\"", "\"\"")}\""
    }

    private fun validateCsvIntegrity(csv: String, expectedDataRows: Int) {
        val records = parseCsvRecords(csv)
        check(records.isNotEmpty()) { "El CSV generado está vacío." }
        check(records.first() == HEADERS) { "La cabecera CSV no coincide con el esquema esperado." }
        check(records.size - 1 == expectedDataRows) {
            "Validación CSV fallida: esperadas $expectedDataRows filas, encontradas ${records.size - 1}."
        }
        records.forEachIndexed { idx, row ->
            check(row.size == HEADERS.size) {
                "Fila CSV $idx tiene ${row.size} columnas en lugar de ${HEADERS.size}."
            }
        }
    }

    internal fun parseCsvRecords(csv: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < csv.length) {
            val ch = csv[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < csv.length && csv[i + 1] == '"') {
                        field.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    field.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow += field.toString()
                        field.setLength(0)
                    }
                    '\r' -> {
                        if (i + 1 < csv.length && csv[i + 1] == '\n') i++
                        currentRow += field.toString()
                        field.setLength(0)
                        rows += currentRow.toList()
                        currentRow.clear()
                    }
                    '\n' -> {
                        currentRow += field.toString()
                        field.setLength(0)
                        rows += currentRow.toList()
                        currentRow.clear()
                    }
                    else -> field.append(ch)
                }
            }
            i++
        }
        if (field.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow += field.toString()
            rows += currentRow.toList()
        }
        return rows
    }

    private data class CsvEntry(
        val occurredAt: Long,
        val createdAt: Long,
        val columns: List<String>,
    )

    companion object {
        val HEADERS = listOf(
            "id",
            "tipo",
            "fecha_iso",
            "importe_eur",
            "importe_centimos",
            "cuenta_origen_o_pagadora",
            "cuenta_destino_o_responsable",
            "categoria",
            "es_adelanto",
            "nota_o_motivo",
            "origen",
        )
    }
}
