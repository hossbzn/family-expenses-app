package com.familyexpenses.app.feature.addtransaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.KeyboardType
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
        onAmountChanged = viewModel::onAmountChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onCategorySelected = viewModel::onCategorySelected,
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
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onPaidFromPersonalChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                OutlinedTextField(
                    value = uiState.amountInput,
                    onValueChange = onAmountChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "0,00",
                            style = MaterialTheme.typography.displaySmall,
                        )
                    },
                    textStyle = MaterialTheme.typography.displaySmall,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(18.dp),
                )

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

                if (uiState.selectedType == TransactionType.EXPENSE) {
                    SectionBlock(title = "Pagado con personal") {
                        Switch(
                            checked = uiState.paidFromPersonal,
                            onCheckedChange = onPaidFromPersonalChanged,
                        )
                    }
                }

                SectionBlock(title = "Nota") {
                    OutlinedTextField(
                        value = uiState.noteInput,
                        onValueChange = onNoteChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Opcional") },
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }

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
