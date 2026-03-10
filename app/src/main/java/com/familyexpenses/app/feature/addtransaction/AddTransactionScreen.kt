package com.familyexpenses.app.feature.addtransaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.core.model.TransactionType
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
        onBack = onBack,
        onTypeSelected = viewModel::onTypeSelected,
        onAmountChanged = viewModel::onAmountChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onCategorySelected = viewModel::onCategorySelected,
        onAccountSelected = viewModel::onAccountSelected,
        onPaidFromPersonalChanged = viewModel::onPaidFromPersonalChanged,
        onSave = viewModel::save,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddTransactionScreen(
    uiState: AddTransactionUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAccountSelected: (String) -> Unit,
    onPaidFromPersonalChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.title) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            item {
                Text(
                    text = "Entrada rapida con el minimo ruido posible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item {
                SectionBlock(title = "Tipo") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(
                            selected = uiState.selectedType == TransactionType.EXPENSE,
                            onClick = { onTypeSelected(TransactionType.EXPENSE) },
                            label = { Text("Gasto") },
                            shape = RoundedCornerShape(12.dp),
                        )
                        FilterChip(
                            selected = uiState.selectedType == TransactionType.INCOME,
                            enabled = !uiState.paidFromPersonal,
                            onClick = { onTypeSelected(TransactionType.INCOME) },
                            label = { Text("Ingreso") },
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                }
            }

            item {
                SectionBlock(title = "Importe") {
                    OutlinedTextField(
                        value = uiState.amountInput,
                        onValueChange = onAmountChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("0,00") },
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }

            item {
                SectionBlock(title = "Nota") {
                    OutlinedTextField(
                        value = uiState.noteInput,
                        onValueChange = onNoteChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Opcional") },
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }

            item {
                SectionBlock(title = "Categoria") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (uiState.categoryOptions.isEmpty()) {
                            Text(
                                text = "Todavia no hay categorias disponibles. Cierra y vuelve a abrir la app para recargar el seed.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            uiState.categoryOptions.forEach { category ->
                                FilterChip(
                                    selected = uiState.selectedCategoryId == category.id,
                                    onClick = { onCategorySelected(category.id) },
                                    label = { Text(category.name) },
                                    shape = RoundedCornerShape(12.dp),
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionBlock(title = "Cuenta") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.accountOptions.forEach { account ->
                            FilterChip(
                                selected = uiState.selectedAccountId == account.id,
                                enabled = !uiState.paidFromPersonal,
                                onClick = { onAccountSelected(account.id) },
                                label = { Text(account.name) },
                                shape = RoundedCornerShape(12.dp),
                            )
                        }
                    }
                }
            }

            item {
                SectionBlock(title = "Pagado con cuenta personal") {
                    Text(
                        text = if (uiState.selectedType == TransactionType.INCOME) {
                            "Solo aplica a gastos familiares."
                        } else {
                            "Activalo solo si el gasto pertenece a familia pero se pago desde una cuenta personal."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (uiState.selectedType == TransactionType.EXPENSE) {
                        Switch(
                            checked = uiState.paidFromPersonal,
                            onCheckedChange = onPaidFromPersonalChanged,
                        )
                    }
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
                    contentPadding = PaddingValues(vertical = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(
                        if (uiState.isSaving) {
                            "Guardando..."
                        } else if (uiState.selectedType == TransactionType.INCOME) {
                            "Guardar ingreso"
                        } else {
                            "Guardar gasto"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionBlock(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        HorizontalDivider()
        content()
    }
}
