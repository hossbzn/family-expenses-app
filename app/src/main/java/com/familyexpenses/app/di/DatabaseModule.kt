package com.familyexpenses.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val migration2To3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE transactions ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'",
            )
            database.execSQL(
                "ALTER TABLE transactions ADD COLUMN recurrenceRuleId TEXT",
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recurrence_rules_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    type TEXT NOT NULL,
                    accountId TEXT NOT NULL,
                    categoryId TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    paidFromPersonal INTEGER NOT NULL,
                    note TEXT,
                    nextOccurrenceAt INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE RESTRICT,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_recurrence_rules_new_accountId ON recurrence_rules_new(accountId)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_recurrence_rules_new_categoryId ON recurrence_rules_new(categoryId)",
            )
            database.execSQL(
                """
                INSERT INTO recurrence_rules_new (
                    id, type, accountId, categoryId, amountMinor, paidFromPersonal, note,
                    nextOccurrenceAt, isActive, createdAt
                )
                SELECT
                    rr.id,
                    t.type,
                    t.accountId,
                    t.categoryId,
                    t.amountMinor,
                    t.paidFromPersonal,
                    t.note,
                    rr.nextOccurrenceAt,
                    rr.isActive,
                    rr.createdAt
                FROM recurrence_rules rr
                INNER JOIN transactions t ON t.id = rr.transactionId
                WHERE t.categoryId IS NOT NULL
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE recurrence_rules")
            database.execSQL("ALTER TABLE recurrence_rules_new RENAME TO recurrence_rules")
        }
    }

    private val migration3To4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS recurrence_rules_new (
                    id TEXT NOT NULL PRIMARY KEY,
                    type TEXT NOT NULL,
                    accountId TEXT NOT NULL,
                    categoryId TEXT NOT NULL,
                    amountMinor INTEGER NOT NULL,
                    paidFromPersonal INTEGER NOT NULL,
                    note TEXT,
                    startAt INTEGER NOT NULL,
                    endAt INTEGER,
                    nextOccurrenceAt INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(accountId) REFERENCES accounts(id) ON DELETE RESTRICT,
                    FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE RESTRICT
                )
                """.trimIndent(),
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_recurrence_rules_new_accountId ON recurrence_rules_new(accountId)",
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_recurrence_rules_new_categoryId ON recurrence_rules_new(categoryId)",
            )
            database.execSQL(
                """
                INSERT INTO recurrence_rules_new (
                    id, type, accountId, categoryId, amountMinor, paidFromPersonal, note,
                    startAt, endAt, nextOccurrenceAt, isActive, createdAt
                )
                SELECT
                    id, type, accountId, categoryId, amountMinor, paidFromPersonal, note,
                    nextOccurrenceAt, NULL, nextOccurrenceAt, isActive, createdAt
                FROM recurrence_rules
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE recurrence_rules")
            database.execSQL("ALTER TABLE recurrence_rules_new RENAME TO recurrence_rules")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "family-expenses.db",
        ).addMigrations(migration2To3, migration3To4).build()

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
