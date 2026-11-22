package com.familyexpenses.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

val MIGRATION_1_5 = object : Migration(1, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recurrence_occurrences` (
                `id` TEXT NOT NULL,
                `ruleId` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `scheduledAt` INTEGER NOT NULL,
                `reviewAt` INTEGER,
                `amountMinor` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `payingAccountId` TEXT NOT NULL,
                `responsibleAccountId` TEXT NOT NULL,
                `categoryId` TEXT NOT NULL,
                `note` TEXT,
                `transactionId` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`ruleId`) REFERENCES `recurrence_rules`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurrence_occurrences_ruleId` ON `recurrence_occurrences` (`ruleId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurrence_occurrences_transactionId` ON `recurrence_occurrences` (`transactionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurrence_occurrences_status` ON `recurrence_occurrences` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurrence_occurrences_scheduledAt` ON `recurrence_occurrences` (`scheduledAt`)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurrence_occurrences_ruleId_scheduledAt` ON `recurrence_occurrences` (`ruleId`, `scheduledAt`)")
    }
}

val MIGRATION_2_5 = object : Migration(2, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Verificar si la tabla de ocurrencias ya existía en la variante v2
        val cursor = db.query("PRAGMA table_info(recurrence_occurrences)")
        val tableExists = cursor.count > 0
        cursor.close()

        if (!tableExists) {
            MIGRATION_1_5.migrate(db)
        } else {
            // Si la tabla existía pero le faltaba reviewAt, añadir la columna
            val reviewAtCursor = db.query("PRAGMA table_info(recurrence_occurrences)")
            var hasReviewAt = false
            while (reviewAtCursor.moveToNext()) {
                val nameIndex = reviewAtCursor.getColumnIndex("name")
                if (nameIndex != -1 && reviewAtCursor.getString(nameIndex) == "reviewAt") {
                    hasReviewAt = true
                    break
                }
            }
            reviewAtCursor.close()

            if (!hasReviewAt) {
                db.execSQL("ALTER TABLE `recurrence_occurrences` ADD COLUMN `reviewAt` INTEGER")
            }

            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurrence_occurrences_ruleId_scheduledAt` ON `recurrence_occurrences` (`ruleId`, `scheduledAt`)")
        }
    }
}

val MIGRATION_3_5 = object : Migration(3, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_5.migrate(db)
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        MIGRATION_1_5.migrate(db)
    }
}

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
            .addMigrations(MIGRATION_1_5, MIGRATION_2_5, MIGRATION_3_5, MIGRATION_4_5)
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
