package com.familyexpenses.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyexpenses.app.domain.usecase.GenerateRecurringTransactionsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class GenerateRecurringTransactionsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val generateRecurringTransactionsUseCase: GenerateRecurringTransactionsUseCase,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        generateRecurringTransactionsUseCase()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "generate_recurring_transactions"
    }
}
