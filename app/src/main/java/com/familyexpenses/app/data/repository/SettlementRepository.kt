package com.familyexpenses.app.data.repository

import androidx.room.withTransaction
import com.familyexpenses.app.core.model.LedgerStatus
import com.familyexpenses.app.data.local.AppDatabase
import com.familyexpenses.app.data.local.dao.ReimbursementLedgerDao
import com.familyexpenses.app.data.local.dao.SettlementDao
import com.familyexpenses.app.data.local.entity.SettlementEntity
import com.familyexpenses.app.data.local.entity.SettlementItemEntity
import com.familyexpenses.app.domain.model.PendingSettlementSummary
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SettlementRepository @Inject constructor(
    private val database: AppDatabase,
    private val reimbursementLedgerDao: ReimbursementLedgerDao,
    private val settlementDao: SettlementDao,
) {

    fun observePendingSummary(): Flow<PendingSettlementSummary> = reimbursementLedgerDao.observeOpenEntries()
        .map { openEntries ->
            PendingSettlementSummary(
                pendingAmountMinor = openEntries.sumOf { it.amountMinor },
                pendingEntriesCount = openEntries.size,
            )
        }

    suspend fun createFullSettlement(settledAt: Long = System.currentTimeMillis()): String {
        return database.withTransaction {
            val openEntries = reimbursementLedgerDao.getOpenEntries()
            require(openEntries.isNotEmpty()) { "No hay pendiente por liquidar." }

            val settlementId = UUID.randomUUID().toString()
            settlementDao.upsertSettlement(
                SettlementEntity(
                    id = settlementId,
                    amountMinor = openEntries.sumOf { it.amountMinor },
                    settledAt = settledAt,
                    createdAt = settledAt,
                ),
            )
            settlementDao.upsertSettlementItems(
                openEntries.map { entry ->
                    SettlementItemEntity(
                        id = UUID.randomUUID().toString(),
                        settlementId = settlementId,
                        ledgerId = entry.id,
                        amountMinor = entry.amountMinor,
                    )
                },
            )
            reimbursementLedgerDao.markEntriesAsSettled(
                ledgerIds = openEntries.map { it.id },
                settledAt = settledAt,
            )
            settlementId
        }
    }
}
