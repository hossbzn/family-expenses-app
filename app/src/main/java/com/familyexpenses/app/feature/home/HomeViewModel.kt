package com.familyexpenses.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.data.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = accountRepository.observeAccounts()
        .map { accounts ->
            HomeUiState(
                accountNames = accounts.map { it.name },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(),
        )
}

data class HomeUiState(
    val accountNames: List<String> = emptyList(),
)
