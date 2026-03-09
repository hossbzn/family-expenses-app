package com.familyexpenses.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.familyexpenses.app.data.local.converter.RoomConverters
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.dao.RecurrenceRuleDao
import com.familyexpenses.app.data.local.dao.ReimbursementLedgerDao
import com.familyexpenses.app.data.local.dao.SettlementDao
import com.familyexpenses.app.data.local.dao.TransactionDao
import com.familyexpenses.app.data.local.dao.UserDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.data.local.entity.ReimbursementLedgerEntity
import com.familyexpenses.app.data.local.entity.SettlementEntity
import com.familyexpenses.app.data.local.entity.SettlementItemEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        RecurrenceRuleEntity::class,
        ReimbursementLedgerEntity::class,
        SettlementEntity::class,
        SettlementItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurrenceRuleDao(): RecurrenceRuleDao
    abstract fun reimbursementLedgerDao(): ReimbursementLedgerDao
    abstract fun settlementDao(): SettlementDao
}
