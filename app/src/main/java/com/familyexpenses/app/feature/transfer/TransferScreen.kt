package com.familyexpenses.app.feature.transfer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.feature.expense.FoldedDetailsSection
import com.familyexpenses.app.feature.expense.NumericKeypad
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun TransferRoute(
    onNavigateBack: () -> Unit,
    onTransferSaved: (transferId: String) -> Unit,
    viewModel: TransferViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        viewModel.onBackClicked()
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is TransferEvent.Saved -> onTransferSaved(event.transferId)
                is TransferEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    TransferScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onTypeSelected = viewModel::onTypeSelected,
        onSourceAccountSelected = viewModel::onSourceAccountSelected,
        onSwapAccounts = viewModel::onSwapAccounts,
        onDigit = viewModel::onDigit,
        onComma = viewModel::onComma,
        onClear = viewModel::onClear,
        onBackspace = viewModel::onBackspace,
        onNoteChanged = viewModel::onNoteChanged,
        onDateSelected = viewModel::onDateSelected,
        onToggleDetails = viewModel::onToggleDetails,
        onBackClicked = viewModel::onBackClicked,
        onConfirmDiscard = viewModel::onConfirmDiscard,
        onDismissDiscardDialog = viewModel::onDismissDiscardDialog,
        onSave = viewModel::saveTransfer,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TransferScreen(
    uiState: TransferUiState,
    snackbarHostState: SnackbarHostState,
    onTypeSelected: (TransferType) -> Unit,
    onSourceAccountSelected: (String) -> Unit,
    onSwapAccounts: () -> Unit,
    onDigit: (Char) -> Unit,
    onComma: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onToggleDetails: () -> Unit,
    onBackClicked: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissDiscardDialog: () -> Unit,
    onSave: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Transferir",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
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
                    val buttonLabel = when {
                        uiState.isSaving -> "Guardando..."
                        uiState.selectedType == TransferType.REIMBURSEMENT -> "Confirmar devolución"
                        uiState.selectedType == TransferType.NORMAL -> "Confirmar transferencia"
                        else -> "Selecciona un tipo para continuar"
                    }
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
                            text = buttonLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Selección obligatoria y explícita de tipo
            Text(
                text = "Tipo de operación (obligatorio)",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.Gray,
            )

            TransferTypeOptionCard(
                title = "Transferencia normal",
                subtitle = "Mueve saldo entre cuentas sin modificar adelantos",
                isSelected = uiState.selectedType == TransferType.NORMAL,
                onClick = { onTypeSelected(TransferType.NORMAL) },
            )

            val reimbursementSubtitle = if (uiState.pendingDebtMinor > 0L) {
                "Deuda pendiente: ${formatCurrency(uiState.pendingDebtMinor)} (editable hasta el máximo)"
            } else {
                "Sin deuda pendiente en este momento"
            }

            TransferTypeOptionCard(
                title = "Devolución de adelanto",
                subtitle = reimbursementSubtitle,
                isSelected = uiState.selectedType == TransferType.REIMBURSEMENT,
                onClick = { onTypeSelected(TransferType.REIMBURSEMENT) },
            )

            // 2. Display destacado de importe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.displayAmount,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.selectedType == null) Color.Gray else Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 3. Dirección (origen -> destino)
            TransferDirectionSection(
                selectedType = uiState.selectedType,
                accounts = uiState.accounts,
                sourceAccountId = uiState.sourceAccountId,
                destinationAccountId = uiState.destinationAccountId,
                pendingDebtMinor = uiState.pendingDebtMinor,
                onSourceAccountSelected = onSourceAccountSelected,
                onSwapAccounts = onSwapAccounts,
            )

            // Mensaje de validación si supera el máximo o no hay deuda
            uiState.validationMessage?.let { message ->
                Surface(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFB00020).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            // 4. Teclado Numérico Propio
            NumericKeypad(
                onDigit = onDigit,
                onComma = onComma,
                onClear = onClear,
                onBackspace = onBackspace,
            )

            // 5. Detalles Plegados
            FoldedDetailsSection(
                isExpanded = uiState.isDetailsExpanded,
                note = uiState.noteInput,
                occurredAt = uiState.occurredAt,
                onToggle = onToggleDetails,
                onNoteChanged = onNoteChanged,
                onOpenDatePicker = { showDatePicker = true },
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismissDiscardDialog,
            title = { Text("¿Descartar operación?") },
            text = { Text("Se perderán los datos introducidos.") },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDiscardDialog) {
                    Text("Continuar editando")
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
private fun TransferTypeOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.Black else Color(0xFFF7F7F7),
        border = BorderStroke(1.dp, if (isSelected) Color.Black else Color(0xFFE0E0E0)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) Color.White else Color.Gray,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color.White else Color.Black,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) Color(0xFFCCCCCC) else Color.Gray,
                )
            }
        }
    }
}

@Composable
private fun TransferDirectionSection(
    selectedType: TransferType?,
    accounts: List<AccountEntity>,
    sourceAccountId: String,
    destinationAccountId: String,
    pendingDebtMinor: Long,
    onSourceAccountSelected: (String) -> Unit,
    onSwapAccounts: () -> Unit,
) {
    val sourceAccount = accounts.firstOrNull { it.id == sourceAccountId }
    val destinationAccount = accounts.firstOrNull { it.id == destinationAccountId }

    if (selectedType == TransferType.REIMBURSEMENT) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF5F5F5),
            border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (pendingDebtMinor > 0L && sourceAccount != null && destinationAccount != null) {
                    Text(
                        text = sourceAccount.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "hacia",
                        tint = Color.DarkGray,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = destinationAccount.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                    )
                } else {
                    Text(
                        text = "Dirección automática según deuda pendiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Desde",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.DarkGray,
                    modifier = Modifier.width(64.dp),
                )
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    accounts.forEach { account ->
                        val isSelected = account.id == sourceAccountId
                        Surface(
                            onClick = { onSourceAccountSelected(account.id) },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Hacia",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.DarkGray,
                    modifier = Modifier.width(64.dp),
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF5F5F5),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                ) {
                    Box(
                        contentAlignment = Alignment.CenterStart,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Text(
                            text = destinationAccount?.name ?: "Selecciona destino",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSwapAccounts,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = "Invertir cuentas",
                        tint = Color.Black,
                    )
                }
            }
        }
    }
}

private fun formatCurrency(minorAmount: Long): String {
    val amount = minorAmount / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    format.currency = Currency.getInstance("EUR")
    return format.format(amount)
}
