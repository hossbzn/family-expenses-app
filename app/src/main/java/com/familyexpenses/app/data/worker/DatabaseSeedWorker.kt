package com.familyexpenses.app.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.familyexpenses.app.data.seed.DatabaseSeeder
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DatabaseSeedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val databaseSeeder: DatabaseSeeder,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        databaseSeeder.seedDefaultsIfNeeded()
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "database_seed"
    }
}
