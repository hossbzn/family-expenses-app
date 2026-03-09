package com.familyexpenses.app.di

import android.content.Context
import androidx.room.Room
import com.familyexpenses.app.data.local.AppDatabase
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.RecurrenceRuleDao
import com.familyexpenses.app.data.local.dao.ReimbursementLedgerDao
import com.familyexpenses.app.data.local.dao.SettlementDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.UserDao
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
        ).build()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideRecurrenceRuleDao(database: AppDatabase): RecurrenceRuleDao = database.recurrenceRuleDao()

    @Provides
    fun provideReimbursementLedgerDao(database: AppDatabase): ReimbursementLedgerDao =
        database.reimbursementLedgerDao()

    @Provides
    fun provideSettlementDao(database: AppDatabase): SettlementDao = database.settlementDao()
}
