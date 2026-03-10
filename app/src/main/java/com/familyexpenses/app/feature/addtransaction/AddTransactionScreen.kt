package com.familyexpenses.app.feature.addtransaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AddTransactionRoute(
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                AddTransactionEvent.Saved -> onBack()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    AddTransactionScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onAmountChanged = viewModel::onAmountChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onAccountSelected = viewModel::onAccountSelected,
        onPaidFromPersonalChanged = viewModel::onPaidFromPersonalChanged,
        onSave = viewModel::save,
    )
}

@Composable
fun AddTransactionScreen(
    uiState: AddTransactionUiState,
    snackbarHostState: SnackbarHostState,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAccountSelected: (String) -> Unit,
    onPaidFromPersonalChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = uiState.title,
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Flujo rapido de gasto con fecha de hoy.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.amountInput,
                    onValueChange = onAmountChanged,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Importe") },
                    placeholder = { Text("0,00") },
                )
            }

            item {
                OutlinedTextField(
                    value = uiState.noteInput,
                    onValueChange = onNoteChanged,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nota") },
                    placeholder = { Text("Opcional") },
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Categoria",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    uiState.categoryOptions.forEach { category ->
                        FilterChip(
                            selected = uiState.selectedCategoryId == category.id,
                            onClick = { onCategorySelected(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Cuenta",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    uiState.accountOptions.forEach { account ->
                        FilterChip(
                            selected = uiState.selectedAccountId == account.id,
                            enabled = !uiState.paidFromPersonal,
                            onClick = { onAccountSelected(account.id) },
                            label = { Text(account.name) },
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pagado con cuenta personal",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Switch(
                        checked = uiState.paidFromPersonal,
                        onCheckedChange = onPaidFromPersonalChanged,
                    )
                    if (uiState.paidFromPersonal) {
                        Text(
                            text = "La cuenta queda forzada a Familiar y se generara pendiente familia a personal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = onSave,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text(if (uiState.isSaving) "Guardando..." else "Guardar gasto")
                }
            }
        }
    }
}
