package com.familyexpenses.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.familyexpenses.app.di.MIGRATION_1_5
import com.familyexpenses.app.di.MIGRATION_2_5
import com.familyexpenses.app.di.MIGRATION_3_5
import com.familyexpenses.app.di.MIGRATION_4_5
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppDatabaseMigrationTest {

    private fun createHelper(version: Int, onCreate: (SupportSQLiteDatabase) -> Unit): SupportSQLiteDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val config = androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(null) // In-memory database
            .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(version) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    onCreate(db)
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        return helper.writableDatabase
    }

    @Test
    fun testMigration1To5() {
        val db = createHelper(1) { database ->
            database.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `initialBalanceMinor` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `type` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `favoritePosition` INTEGER, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `payingAccountId` TEXT NOT NULL, `responsibleAccountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `note` TEXT, `source` TEXT NOT NULL, `recurrenceRuleId` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `transfers` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `sourceAccountId` TEXT NOT NULL, `destinationAccountId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `occurredAt` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `balance_adjustments` (`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `amountDeltaMinor` INTEGER NOT NULL, `reason` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `recurrence_rules` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `payingAccountId` TEXT NOT NULL, `responsibleAccountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `note` TEXT, `frequency` TEXT NOT NULL, `intervalCount` INTEGER, `dayOfWeek` INTEGER, `dayOfMonth` INTEGER, `monthOfYear` INTEGER, `mode` TEXT NOT NULL, `status` TEXT NOT NULL, `startAt` INTEGER NOT NULL, `endAt` INTEGER, `nextOccurrenceAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")

            database.execSQL("INSERT INTO accounts (id, name, type, currencyCode, initialBalanceMinor, isArchived, createdAt) VALUES ('acc-1', 'Personal', 'PERSONAL', 'EUR', 1000, 0, 1000000)")
        }

        MIGRATION_1_5.migrate(db)

        val cursor = db.query("SELECT * FROM accounts WHERE id = 'acc-1'")
        assertTrue(cursor.moveToFirst())
        assertEquals("Personal", cursor.getString(cursor.getColumnIndexOrThrow("name")))
        cursor.close()

        val occCursor = db.query("PRAGMA table_info(recurrence_occurrences)")
        assertTrue(occCursor.count > 0)
        occCursor.close()
    }

    @Test
    fun testMigration2To5WithDuplicatesSanitization() {
        val db = createHelper(2) { database ->
            database.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `initialBalanceMinor` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `type` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `favoritePosition` INTEGER, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `payingAccountId` TEXT NOT NULL, `responsibleAccountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `note` TEXT, `source` TEXT NOT NULL, `recurrenceRuleId` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `transfers` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `sourceAccountId` TEXT NOT NULL, `destinationAccountId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `occurredAt` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `balance_adjustments` (`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `amountDeltaMinor` INTEGER NOT NULL, `reason` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `recurrence_rules` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `payingAccountId` TEXT NOT NULL, `responsibleAccountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `note` TEXT, `frequency` TEXT NOT NULL, `intervalCount` INTEGER, `dayOfWeek` INTEGER, `dayOfMonth` INTEGER, `monthOfYear` INTEGER, `mode` TEXT NOT NULL, `status` TEXT NOT NULL, `startAt` INTEGER NOT NULL, `endAt` INTEGER, `nextOccurrenceAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")

            database.execSQL("CREATE TABLE IF NOT EXISTS `recurrence_occurrences` (`id` TEXT NOT NULL, `ruleId` TEXT NOT NULL, `status` TEXT NOT NULL, `scheduledAt` INTEGER NOT NULL, `amountMinor` INTEGER NOT NULL, `type` TEXT NOT NULL, `payingAccountId` TEXT NOT NULL, `responsibleAccountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `note` TEXT, `transactionId` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")

            database.execSQL("INSERT INTO accounts (id, name, type, currencyCode, initialBalanceMinor, isArchived, createdAt) VALUES ('acc-1', 'Personal', 'PERSONAL', 'EUR', 1000, 0, 1000000)")
            database.execSQL("INSERT INTO recurrence_rules (id, type, payingAccountId, responsibleAccountId, categoryId, amountMinor, frequency, mode, status, startAt, nextOccurrenceAt, createdAt) VALUES ('rule-1', 'EXPENSE', 'acc-1', 'acc-1', 'cat-1', 5000, 'WEEKLY', 'PENDING', 'ACTIVE', 100000, 100000, 100000)")
            database.execSQL("INSERT INTO recurrence_occurrences (id, ruleId, status, scheduledAt, amountMinor, type, payingAccountId, responsibleAccountId, categoryId, createdAt) VALUES ('occ-1', 'rule-1', 'PENDING', 100000, 5000, 'EXPENSE', 'acc-1', 'acc-1', 'cat-1', 100000)")
            database.execSQL("INSERT INTO recurrence_occurrences (id, ruleId, status, scheduledAt, amountMinor, type, payingAccountId, responsibleAccountId, categoryId, createdAt) VALUES ('occ-2', 'rule-1', 'PENDING', 100000, 5000, 'EXPENSE', 'acc-1', 'acc-1', 'cat-1', 100001)")
        }

        MIGRATION_2_5.migrate(db)

        val occCursor = db.query("SELECT * FROM recurrence_occurrences WHERE ruleId = 'rule-1'")
        assertEquals(1, occCursor.count)
        occCursor.close()

        val tableInfoCursor = db.query("PRAGMA table_info(recurrence_occurrences)")
        var hasReviewAt = false
        while (tableInfoCursor.moveToNext()) {
            val nameIndex = tableInfoCursor.getColumnIndex("name")
            if (nameIndex != -1 && tableInfoCursor.getString(nameIndex) == "reviewAt") {
                hasReviewAt = true
                break
            }
        }
        tableInfoCursor.close()
        assertTrue(hasReviewAt)
    }

    @Test
    fun testMigration3To5() {
        val db = createHelper(3) { database ->
            database.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `initialBalanceMinor` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `type` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `favoritePosition` INTEGER, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `payingAccountId` TEXT NOT NULL, `responsibleAccountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `note` TEXT, `source` TEXT NOT NULL, `recurrenceRuleId` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `transfers` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `sourceAccountId` TEXT NOT NULL, `destinationAccountId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `occurredAt` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `balance_adjustments` (`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `amountDeltaMinor` INTEGER NOT NULL, `reason` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `recurrence_rules` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `payingAccountId` TEXT NOT NULL, `responsibleAccountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `note` TEXT, `frequency` TEXT NOT NULL, `intervalCount` INTEGER, `dayOfWeek` INTEGER, `dayOfMonth` INTEGER, `monthOfYear` INTEGER, `mode` TEXT NOT NULL, `status` TEXT NOT NULL, `startAt` INTEGER NOT NULL, `endAt` INTEGER, `nextOccurrenceAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }

        MIGRATION_3_5.migrate(db)
        assertTrue(db.isOpen)
    }

    @Test
    fun testMigration4To5() {
        val db = createHelper(4) { database ->
            database.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `accounts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `initialBalanceMinor` INTEGER NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `icon` TEXT NOT NULL, `type` TEXT NOT NULL, `orderIndex` INTEGER NOT NULL, `isFavorite` INTEGER NOT NULL, `favoritePosition` INTEGER, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `payingAccountId` TEXT NOT NULL, `responsibleAccountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `note` TEXT, `source` TEXT NOT NULL, `recurrenceRuleId` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `transfers` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `sourceAccountId` TEXT NOT NULL, `destinationAccountId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `occurredAt` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `balance_adjustments` (`id` TEXT NOT NULL, `accountId` TEXT NOT NULL, `amountDeltaMinor` INTEGER NOT NULL, `reason` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
            database.execSQL("CREATE TABLE IF NOT EXISTS `recurrence_rules` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, `payingAccountId` TEXT NOT NULL, `responsibleAccountId` TEXT NOT NULL, `categoryId` TEXT NOT NULL, `amountMinor` INTEGER NOT NULL, `note` TEXT, `frequency` TEXT NOT NULL, `intervalCount` INTEGER, `dayOfWeek` INTEGER, `dayOfMonth` INTEGER, `monthOfYear` INTEGER, `mode` TEXT NOT NULL, `status` TEXT NOT NULL, `startAt` INTEGER NOT NULL, `endAt` INTEGER, `nextOccurrenceAt` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        }

        MIGRATION_4_5.migrate(db)
        assertTrue(db.isOpen)
    }
}
