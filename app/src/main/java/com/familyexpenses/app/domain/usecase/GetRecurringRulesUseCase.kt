package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.data.repository.RecurringRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetRecurringRulesUseCase @Inject constructor(
    private val recurringRepository: RecurringRepository,
) {
    operator fun invoke(): Flow<List<RecurrenceRuleEntity>> = recurringRepository.observeRules()
}
