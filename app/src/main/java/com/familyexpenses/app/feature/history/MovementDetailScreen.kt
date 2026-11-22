package com.familyexpenses.app.feature.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.domain.model.MovementKind
import com.familyexpenses.app.feature.expense.resolveCategoryIcon
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MovementDetailRoute(
    onNavigateBack: () -> Unit,
    viewModel: MovementDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                MovementDetailEvent.Saved, MovementDetailEvent.Deleted -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    MovementDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onAmountChanged = viewModel::onAmountChanged,
        onPayingAccountSelected = viewModel::onPayingAccountSelected,
        onResponsibleAccountSelected = viewModel::onResponsibleAccountSelected,
        onTransferTypeSelected = viewModel::onTransferTypeSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onNoteChanged = viewModel::onNoteChanged,
        onDateSelected = viewModel::onDateSelected,
        onSave = viewModel::saveChanges,
        onDeleteClick = viewModel::onDeleteClicked,
        onDismissDeleteDialog = viewModel::onDismissDeleteDialog,
        onConfirmDelete = viewModel::onConfirmDelete,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
fun MovementDetailScreen(
    uiState: MovementDetailUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onPayingAccountSelected: (String) -> Unit,
    onResponsibleAccountSelected: (String) -> Unit,
    onTransferTypeSelected: (TransferType) -> Unit,
    onCategorySelected: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onSave: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var showDatePicker by remember { mutableStateOf(false) }

    val title = when (uiState.kind) {
        MovementKind.EXPENSE -> "Detalle de gasto"
        MovementKind.INCOME -> "Detalle de ingreso"
        MovementKind.NORMAL_TRANSFER -> "Detalle de transferencia"
        MovementKind.REIMBURSEMENT -> "Detalle de devolución"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                actions = {
                    if (!uiState.isLoading && !uiState.notFound) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar movimiento",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (!uiState.isLoading && !uiState.notFound) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Button(
                            onClick = onSave,
                            enabled = uiState.isSaveEnabled,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Black,
                                contentColor = Color.White,
                                disabledContainerColor = Color.LightGray,
                                disabledContentColor = Color.DarkGray,
                            ),
                        ) {
                            Text(
                                text = if (uiState.isSaving) "Guardando..." else "Guardar cambios",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.Black)
                }
            }
            uiState.notFound -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No se encontró el movimiento.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.amountInput,
                        onValueChange = onAmountChanged,
                        label = { Text("Importe (€)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    when (uiState.kind) {
                        MovementKind.EXPENSE -> {
                            AccountSelectionRow(
                                label = "Pagado desde",
                                accounts = uiState.accounts,
                                selectedId = uiState.payingAccountId,
                                onSelected = onPayingAccountSelected,
                            )
                            AccountSelectionRow(
                                label = "Gasto de",
                                accounts = uiState.accounts,
                                selectedId = uiState.responsibleAccountId,
                                onSelected = onResponsibleAccountSelected,
                            )
                            if (uiState.isAdvance) {
                                Surface(
                                    color = Color(0xFFF5F5F5),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        text = "Este gasto genera un adelanto automático entre cuentas.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.DarkGray,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    )
                                }
                            }
                        }
                        MovementKind.INCOME -> {
                            AccountSelectionRow(
                                label = "Ingresar en",
                                accounts = uiState.accounts,
                                selectedId = uiState.payingAccountId,
                                onSelected = onPayingAccountSelected,
                            )
                        }
                        MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> {
                            Text(
                                text = "Tipo de operación",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.Gray,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                TypeOptionChip(
                                    label = "Transferencia normal",
                                    isSelected = uiState.transferType == TransferType.NORMAL,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onTransferTypeSelected(TransferType.NORMAL) },
                                )
                                TypeOptionChip(
                                    label = "Devolución",
                                    isSelected = uiState.transferType == TransferType.REIMBURSEMENT,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onTransferTypeSelected(TransferType.REIMBURSEMENT) },
                                )
                            }
                            AccountSelectionRow(
                                label = "Desde",
                                accounts = uiState.accounts,
                                selectedId = uiState.payingAccountId,
                                onSelected = onPayingAccountSelected,
                            )
                            AccountSelectionRow(
                                label = "Hacia",
                                accounts = uiState.accounts,
                                selectedId = uiState.responsibleAccountId,
                                onSelected = onResponsibleAccountSelected,
                            )
                        }
                    }

                    if (uiState.categories.isNotEmpty()) {
                        Text(
                            text = "Categoría",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.Gray,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            uiState.categories.forEach { category ->
                                val isSelected = category.id == uiState.categoryId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onCategorySelected(category.id) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = resolveCategoryIcon(category.icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                    label = { Text(category.name) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Black,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                    ),
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.noteInput,
                        onValueChange = onNoteChanged,
                        label = { Text("Nota o descripción (opcional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    val formattedDate = remember(uiState.occurredAt) {
                        val sdf = SimpleDateFormat("EEEE, d 'de' MMMM yyyy", Locale("es", "ES"))
                        sdf.format(Date(uiState.occurredAt)).replaceFirstChar { it.uppercase() }
                    }

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Cambiar fecha",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = formattedDate)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            title = { Text("¿Eliminar movimiento?") },
            text = { Text("Se eliminará de forma permanente y se recalcularán los saldos y adelantos.") },
            confirmButton = {
                TextButton(onClick = onConfirmDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) {
                    Text("Cancelar")
                }
            },
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.occurredAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                        showDatePicker = false
                    },
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AccountSelectionRow(
    label: String,
    accounts: List<AccountEntity>,
    selectedId: String,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.DarkGray,
            modifier = Modifier.width(105.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            accounts.forEach { account ->
                val isSelected = account.id == selectedId
                Surface(
                    onClick = { onSelected(account.id) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color.Black else Color(0xFFF5F5F5),
                    border = BorderStroke(1.dp, if (isSelected) Color.Black else Color(0xFFE0E0E0)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isSelected) Color.White else Color.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeOptionChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 42.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color.Black else Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, if (isSelected) Color.Black else Color(0xFFE0E0E0)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (isSelected) Color.White else Color.Black,
            )
        }
    }
}
