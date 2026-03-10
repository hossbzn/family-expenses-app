package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.data.repository.DashboardRepository
import com.familyexpenses.app.domain.model.DashboardSummary
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetDashboardSummaryUseCase @Inject constructor(
    private val dashboardRepository: DashboardRepository,
) {
    operator fun invoke(): Flow<DashboardSummary> = dashboardRepository.observeSummary()
}
