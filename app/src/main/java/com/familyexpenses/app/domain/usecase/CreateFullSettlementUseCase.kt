package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.data.repository.SettlementRepository
import javax.inject.Inject

class CreateFullSettlementUseCase @Inject constructor(
    private val settlementRepository: SettlementRepository,
) {
    suspend operator fun invoke(): String = settlementRepository.createFullSettlement()
}
