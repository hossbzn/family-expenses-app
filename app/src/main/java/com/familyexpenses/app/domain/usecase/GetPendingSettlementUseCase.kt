package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.data.repository.SettlementRepository
import com.familyexpenses.app.domain.model.PendingSettlementSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetPendingSettlementUseCase @Inject constructor(
    private val settlementRepository: SettlementRepository,
) {
    operator fun invoke(): Flow<PendingSettlementSummary> = settlementRepository.observePendingSummary()
}
