package com.familyexpenses.app.domain.model

import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity

data class BackupPreferencesSnapshot(
    val lastExpensePayingAccountId: String? = null,
    val lastExpenseResponsibleAccountId: String? = null,
    val lastExpenseCategoryId: String? = null,
    val lastIncomeAccountId: String? = null,
    val lastIncomeCategoryId: String? = null,
    val accountIcons: Map<String, String> = emptyMap(),
)

data class PortableBackupSnapshot(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appName: String = "FamilyExpensesV6",
    val exportedAt: Long,
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val transfers: List<TransferEntity>,
    val balanceAdjustments: List<BalanceAdjustmentEntity>,
    val recurrenceRules: List<RecurrenceRuleEntity>,
    val recurrenceOccurrences: List<RecurrenceOccurrenceEntity> = emptyList(),
    val preferences: BackupPreferencesSnapshot = BackupPreferencesSnapshot(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 6
    }
}
