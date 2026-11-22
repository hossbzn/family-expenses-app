package com.familyexpenses.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyexpenses.app.core.notification.NotificationHelper
import com.familyexpenses.app.data.local.dao.RecurrenceOccurrenceDao
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.domain.usecase.ProcessRecurringRulesUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class GenerateRecurringTransactionsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val processRecurringRulesUseCase: ProcessRecurringRulesUseCase,
    private val recurrenceOccurrenceDao: RecurrenceOccurrenceDao,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        processRecurringRulesUseCase()

        val notificationsEnabled = userPreferencesRepository.observeNotificationsEnabled().first()
        val duePending = recurrenceOccurrenceDao.getPendingOccurrences()
        val currentFingerprint = duePending.map { it.id }.sorted().joinToString(",")
        val lastNotifiedFingerprint = userPreferencesRepository.getLastNotifiedPendingFingerprint()

        if (duePending.isEmpty()) {
            if (lastNotifiedFingerprint.isNotEmpty()) {
                userPreferencesRepository.setLastNotifiedPendingFingerprint("")
            }
        } else if (notificationsEnabled && currentFingerprint != lastNotifiedFingerprint) {
            val shown = notificationHelper.showPendingRecurrencesNotification(duePending.size)
            if (shown) {
                userPreferencesRepository.setLastNotifiedPendingFingerprint(currentFingerprint)
            }
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "generate_recurring_transactions"
    }
}
