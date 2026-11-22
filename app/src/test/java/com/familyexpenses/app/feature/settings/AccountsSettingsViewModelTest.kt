package com.familyexpenses.app.feature.settings

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.data.local.dao.AccountDao
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountsSettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val accountsFlow = MutableStateFlow(
        listOf(
            AccountEntity(
                id = "account-family",
                name = "Familia",
                type = AccountType.FAMILY,
                currencyCode = "EUR",
                initialBalanceMinor = 0L,
                createdAt = 0L,
            ),
            AccountEntity(
                id = "account-personal",
                name = "Personal",
                type = AccountType.PERSONAL,
                currencyCode = "EUR",
                initialBalanceMinor = 0L,
                createdAt = 1L,
            ),
        ),
    )

    private val fakeAccountDao = object : AccountDao {
        override suspend fun upsert(account: AccountEntity) {
            accountsFlow.value = accountsFlow.value.map { if (it.id == account.id) account else it }
        }
        override suspend fun upsertAll(accounts: List<AccountEntity>) {}
        override fun observeAccounts(): Flow<List<AccountEntity>> = accountsFlow
        override suspend fun getAllAccounts(): List<AccountEntity> = accountsFlow.value
        override suspend fun getAccountById(accountId: String): AccountEntity? =
            accountsFlow.value.find { it.id == accountId }
        override suspend fun countAccounts(): Int = accountsFlow.value.size
        override suspend fun updateInitialBalance(accountId: String, initialBalanceMinor: Long) {}
    }

    private val iconsMap = mutableMapOf<String, String>()
    private val fakePreferences = object : UserPreferencesRepository {
        var privacyMode = false
        override fun observePrivacyModeEnabled(): Flow<Boolean> = MutableStateFlow(privacyMode)
        override fun setPrivacyModeEnabled(enabled: Boolean) { privacyMode = enabled }
        override fun getLastExpensePayingAccountId(): String? = null
        override fun setLastExpensePayingAccountId(accountId: String) {}
        override fun getLastExpenseResponsibleAccountId(): String? = null
        override fun setLastExpenseResponsibleAccountId(accountId: String) {}
        override fun getLastExpenseCategoryId(): String? = null
        override fun setLastExpenseCategoryId(categoryId: String) {}
        override fun getLastIncomeAccountId(): String? = null
        override fun setLastIncomeAccountId(accountId: String) {}
        override fun getLastIncomeCategoryId(): String? = null
        override fun setLastIncomeCategoryId(categoryId: String) {}
        override fun getAccountIcon(accountId: String): String? = iconsMap[accountId]
        override fun setAccountIcon(accountId: String, icon: String) {
            iconsMap[accountId] = icon
        }
    }

    private lateinit var viewModel: AccountsSettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        iconsMap.clear()
        viewModel = AccountsSettingsViewModel(
            accountRepository = AccountRepository(fakeAccountDao),
            preferencesRepository = fakePreferences,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updates account name and icon cleanly`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        val familyItem = viewModel.uiState.value.accounts.first { it.account.id == "account-family" }
        viewModel.onEditAccount(familyItem)
        viewModel.onNameChanged("Casa")
        viewModel.onIconSelected("savings")
        viewModel.onSaveEdit()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.editingAccount)
        val updated = viewModel.uiState.value.accounts.first { it.account.id == "account-family" }
        assertEquals("Casa", updated.account.name)
        assertEquals("savings", updated.iconKey)
    }
}
