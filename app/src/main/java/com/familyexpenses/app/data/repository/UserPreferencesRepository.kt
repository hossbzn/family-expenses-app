package com.familyexpenses.app.data.repository

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface UserPreferencesRepository {
    fun getLastExpensePayingAccountId(): String?
    fun setLastExpensePayingAccountId(accountId: String)
    fun getLastExpenseResponsibleAccountId(): String?
    fun setLastExpenseResponsibleAccountId(accountId: String)
    fun getLastExpenseCategoryId(): String?
    fun setLastExpenseCategoryId(categoryId: String)
    fun getLastIncomeAccountId(): String?
    fun setLastIncomeAccountId(accountId: String)
    fun getLastIncomeCategoryId(): String?
    fun setLastIncomeCategoryId(categoryId: String)
    fun getAccountIcon(accountId: String): String?
    fun setAccountIcon(accountId: String, icon: String)
}

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context,
) : UserPreferencesRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getLastExpensePayingAccountId(): String? = prefs.getString(KEY_LAST_EXPENSE_PAYING_ACCOUNT_ID, null)

    override fun setLastExpensePayingAccountId(accountId: String) {
        prefs.edit { putString(KEY_LAST_EXPENSE_PAYING_ACCOUNT_ID, accountId) }
    }

    override fun getLastExpenseResponsibleAccountId(): String? = prefs.getString(KEY_LAST_EXPENSE_RESPONSIBLE_ACCOUNT_ID, null)

    override fun setLastExpenseResponsibleAccountId(accountId: String) {
        prefs.edit { putString(KEY_LAST_EXPENSE_RESPONSIBLE_ACCOUNT_ID, accountId) }
    }

    override fun getLastExpenseCategoryId(): String? = prefs.getString(KEY_LAST_EXPENSE_CATEGORY_ID, null)

    override fun setLastExpenseCategoryId(categoryId: String) {
        prefs.edit { putString(KEY_LAST_EXPENSE_CATEGORY_ID, categoryId) }
    }

    override fun getLastIncomeAccountId(): String? = prefs.getString(KEY_LAST_INCOME_ACCOUNT_ID, null)

    override fun setLastIncomeAccountId(accountId: String) {
        prefs.edit { putString(KEY_LAST_INCOME_ACCOUNT_ID, accountId) }
    }

    override fun getLastIncomeCategoryId(): String? = prefs.getString(KEY_LAST_INCOME_CATEGORY_ID, null)

    override fun setLastIncomeCategoryId(categoryId: String) {
        prefs.edit { putString(KEY_LAST_INCOME_CATEGORY_ID, categoryId) }
    }

    override fun getAccountIcon(accountId: String): String? = prefs.getString("$KEY_ACCOUNT_ICON_PREFIX$accountId", null)

    override fun setAccountIcon(accountId: String, icon: String) {
        prefs.edit { putString("$KEY_ACCOUNT_ICON_PREFIX$accountId", icon) }
    }

    companion object {
        private const val PREFS_NAME = "family_expenses_user_prefs"
        private const val KEY_LAST_EXPENSE_PAYING_ACCOUNT_ID = "last_expense_paying_account_id"
        private const val KEY_LAST_EXPENSE_RESPONSIBLE_ACCOUNT_ID = "last_expense_responsible_account_id"
        private const val KEY_LAST_EXPENSE_CATEGORY_ID = "last_expense_category_id"
        private const val KEY_LAST_INCOME_ACCOUNT_ID = "last_income_account_id"
        private const val KEY_LAST_INCOME_CATEGORY_ID = "last_income_category_id"
        private const val KEY_ACCOUNT_ICON_PREFIX = "account_icon_"
    }
}
