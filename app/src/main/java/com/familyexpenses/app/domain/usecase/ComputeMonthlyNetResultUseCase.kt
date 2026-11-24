package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

/**
 * Calcula el saldo del mes en curso para una cuenta:
 * ingresos - gastos (imputados a `responsibleAccountId`)
 * + transferencias normales entrantes - transferencias normales salientes del mes.
 * Excluye devoluciones de adelanto, ajustes de saldo y movimientos de otros meses.
 */
class ComputeMonthlyNetResultUseCase @Inject constructor() {

    fun compute(
        accountId: String,
        transactions: List<TransactionEntity>,
        transfers: List<TransferEntity> = emptyList(),
        yearMonth: YearMonth = YearMonth.now(ZoneId.systemDefault()),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Long {
        var netMinor = 0L
        for (tx in transactions) {
            if (tx.responsibleAccountId != accountId) continue
            val txMonth = YearMonth.from(Instant.ofEpochMilli(tx.occurredAt).atZone(zoneId))
            if (txMonth != yearMonth) continue
            when (tx.type) {
                TransactionType.INCOME -> netMinor += tx.amountMinor
                TransactionType.EXPENSE -> netMinor -= tx.amountMinor
                TransactionType.ADJUSTMENT -> Unit
            }
        }
        for (transfer in transfers) {
            if (transfer.type != TransferType.NORMAL) continue
            val transferMonth = YearMonth.from(Instant.ofEpochMilli(transfer.occurredAt).atZone(zoneId))
            if (transferMonth != yearMonth) continue
            if (transfer.destinationAccountId == accountId) {
                netMinor += transfer.amountMinor
            }
            if (transfer.sourceAccountId == accountId) {
                netMinor -= transfer.amountMinor
            }
        }
        return netMinor
    }

    fun formatMonthName(
        yearMonth: YearMonth = YearMonth.now(ZoneId.systemDefault()),
        locale: Locale = Locale("es", "ES"),
    ): String {
        return yearMonth.month.getDisplayName(TextStyle.FULL, locale).lowercase(locale)
    }
}
