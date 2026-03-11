package com.familyexpenses.app.data.local.converter

import androidx.room.TypeConverter
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.LedgerStatus
import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType

class RoomConverters {

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toCategoryType(value: String): CategoryType = CategoryType.valueOf(value)

    @TypeConverter
    fun fromCategoryType(value: CategoryType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionSource(value: String): TransactionSource = TransactionSource.valueOf(value)

    @TypeConverter
    fun fromTransactionSource(value: TransactionSource): String = value.name

    @TypeConverter
    fun toRecurrenceFrequency(value: String): RecurrenceFrequency = RecurrenceFrequency.valueOf(value)

    @TypeConverter
    fun fromRecurrenceFrequency(value: RecurrenceFrequency): String = value.name

    @TypeConverter
    fun toLedgerStatus(value: String): LedgerStatus = LedgerStatus.valueOf(value)

    @TypeConverter
    fun fromLedgerStatus(value: LedgerStatus): String = value.name
}
