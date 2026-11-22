package com.familyexpenses.app.feature.settings

import com.familyexpenses.app.data.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val fakePreferences = object : UserPreferencesRepository {
        var privacyMode = false
        var notificationsFlag = true
        val notificationsFlow = MutableStateFlow(notificationsFlag)

        override fun observePrivacyModeEnabled(): Flow<Boolean> = MutableStateFlow(privacyMode)
        override fun setPrivacyModeEnabled(enabled: Boolean) { privacyMode = enabled }
        override fun observeNotificationsEnabled(): Flow<Boolean> = notificationsFlow
        override fun setNotificationsEnabled(enabled: Boolean) {
            notificationsFlag = enabled
            notificationsFlow.value = enabled
        }
        override fun getLastNotifiedPendingCount(): Int = 0
        override fun setLastNotifiedPendingCount(count: Int) {}
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
        override fun getAccountIcon(accountId: String): String? = null
        override fun setAccountIcon(accountId: String, icon: String) {}
    }

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SettingsViewModel(fakePreferences)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `notificationsEnabled reflects state and setNotificationsEnabled updates state`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.notificationsEnabled.collect() }
        advanceUntilIdle()

        assertTrue(viewModel.notificationsEnabled.value)

        viewModel.setNotificationsEnabled(false)
        advanceUntilIdle()

        assertFalse(viewModel.notificationsEnabled.value)
    }
}
