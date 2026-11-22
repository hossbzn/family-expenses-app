package com.familyexpenses.app.feature.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val notificationsEnabled: StateFlow<Boolean> = userPreferencesRepository.observeNotificationsEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    init {
        checkPermissionAndSync()
    }

    private fun checkPermissionAndSync() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                viewModelScope.launch {
                    if (userPreferencesRepository.observeNotificationsEnabled().first()) {
                        userPreferencesRepository.setNotificationsEnabled(false)
                    }
                }
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        userPreferencesRepository.setNotificationsEnabled(enabled)
    }
}
