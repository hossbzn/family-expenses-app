package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.data.repository.HistoryRepository
import com.familyexpenses.app.domain.model.HistoryItem
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetHistoryUseCase @Inject constructor(
    private val historyRepository: HistoryRepository,
) {
    operator fun invoke(): Flow<List<HistoryItem>> = historyRepository.observeHistory()
}
