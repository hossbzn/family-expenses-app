package com.familyexpenses.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditableAccountItem(
    val account: AccountEntity,
    val iconKey: String,
)

data class AccountsSettingsUiState(
    val accounts: List<EditableAccountItem> = emptyList(),
    val editingAccount: EditableAccountItem? = null,
    val editNameInput: String = "",
    val editIconInput: String = "home",
    val errorMessage: String? = null,
)

@HiltViewModel
class AccountsSettingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val dialogState = MutableStateFlow(AccountsEditDialogState())

    val uiState: StateFlow<AccountsSettingsUiState> = combine(
        accountRepository.observeAccounts(),
        dialogState,
    ) { accounts, state ->
        val visible = accounts.filter { !it.isArchived }.map { account ->
            val defaultIcon = if (account.type == AccountType.FAMILY) "home" else "person"
            val icon = preferencesRepository.getAccountIcon(account.id) ?: defaultIcon
            EditableAccountItem(account = account, iconKey = icon)
        }
        AccountsSettingsUiState(
            accounts = visible,
            editingAccount = state.editingAccount,
            editNameInput = state.editNameInput,
            editIconInput = state.editIconInput,
            errorMessage = state.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AccountsSettingsUiState(),
    )

    fun onEditAccount(item: EditableAccountItem) {
        dialogState.update {
            AccountsEditDialogState(
                editingAccount = item,
                editNameInput = item.account.name,
                editIconInput = item.iconKey,
                errorMessage = null,
            )
        }
    }

    fun onNameChanged(name: String) {
        dialogState.update { it.copy(editNameInput = name, errorMessage = null) }
    }

    fun onIconSelected(iconKey: String) {
        dialogState.update { it.copy(editIconInput = iconKey) }
    }

    fun onDismissEdit() {
        dialogState.update { AccountsEditDialogState() }
    }

    fun onSaveEdit() {
        val state = dialogState.value
        val target = state.editingAccount ?: return
        val trimmed = state.editNameInput.trim()
        if (trimmed.isEmpty()) {
            dialogState.update { it.copy(errorMessage = "El nombre no puede estar vacío.") }
            return
        }

        viewModelScope.launch {
            runCatching {
                accountRepository.updateAccountName(target.account.id, trimmed)
                preferencesRepository.setAccountIcon(target.account.id, state.editIconInput)
            }.onSuccess {
                dialogState.update { AccountsEditDialogState() }
            }.onFailure { err ->
                dialogState.update {
                    it.copy(errorMessage = err.message ?: "No se pudo guardar la cuenta.")
                }
            }
        }
    }
}

private data class AccountsEditDialogState(
    val editingAccount: EditableAccountItem? = null,
    val editNameInput: String = "",
    val editIconInput: String = "home",
    val errorMessage: String? = null,
)
