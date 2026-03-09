package com.familyexpenses.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.familyexpenses.app.data.worker.DatabaseSeedWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class FamilyExpensesApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        val seedRequest = OneTimeWorkRequestBuilder<DatabaseSeedWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            DatabaseSeedWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            seedRequest,
        )
    }
}
