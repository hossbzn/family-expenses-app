package com.familyexpenses.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.familyexpenses.app.data.seed.DatabaseSeeder
import com.familyexpenses.app.data.worker.GenerateRecurringTransactionsWorker
import com.familyexpenses.app.domain.usecase.GenerateRecurringTransactionsUseCase
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class FamilyExpensesApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    @Inject
    lateinit var generateRecurringTransactionsUseCase: GenerateRecurringTransactionsUseCase

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        runBlocking {
            databaseSeeder.seedDefaultsIfNeeded()
            generateRecurringTransactionsUseCase()
        }
        scheduleRecurringGeneration()
    }

    private fun scheduleRecurringGeneration() {
        val request = PeriodicWorkRequestBuilder<GenerateRecurringTransactionsWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            GenerateRecurringTransactionsWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
