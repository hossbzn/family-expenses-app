package com.familyexpenses.app.di

import android.content.Context
import androidx.room.Room
import com.familyexpenses.app.data.local.AppDatabase
import com.familyexpenses.app.data.local.RoomTransactionRunner
import com.familyexpenses.app.data.local.TransactionRunner
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.BalanceAdjustmentDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.RecurrenceOccurrenceDao
import com.familyexpenses.app.data.local.dao.RecurrenceRuleDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.TransferDao
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "family-expenses.db",
        )
            .fallbackToDestructiveMigrationFrom(1, 2, 3, 4)
            .build()

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideTransferDao(database: AppDatabase): TransferDao = database.transferDao()

    @Provides
    fun provideBalanceAdjustmentDao(database: AppDatabase): BalanceAdjustmentDao = database.balanceAdjustmentDao()

    @Provides
    fun provideRecurrenceRuleDao(database: AppDatabase): RecurrenceRuleDao = database.recurrenceRuleDao()

    @Provides
    fun provideRecurrenceOccurrenceDao(database: AppDatabase): RecurrenceOccurrenceDao = database.recurrenceOccurrenceDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseBindsModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRunner(impl: RoomTransactionRunner): TransactionRunner
}
