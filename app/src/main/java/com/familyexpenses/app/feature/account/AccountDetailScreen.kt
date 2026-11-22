package com.familyexpenses.app.feature.account

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.ui.MoneyText
import com.familyexpenses.app.ui.theme.OffWhiteBackground
import com.familyexpenses.app.ui.theme.OutlineBorder
import com.familyexpenses.app.ui.theme.PetroleumBlueContainer
import com.familyexpenses.app.ui.theme.PetroleumBluePrimary
import com.familyexpenses.app.ui.theme.PureWhiteSurface
import kotlinx.coroutines.flow.collectLatest
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

@Composable
fun AccountDetailRoute(
    onNavigateBack: () -> Unit,
    onTransferClick: (accountId: String) -> Unit,
    onMovementsClick: () -> Unit,
    onAdvancesClick: () -> Unit,
    savedTransferId: String? = null,
    onClearSavedTransferId: () -> Unit = {},
    viewModel: AccountDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is AccountDetailEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    LaunchedEffect(savedTransferId) {
        if (!savedTransferId.isNullOrBlank()) {
            val result = snackbarHostState.showSnackbar(
                message = "Transferencia realizada",
                actionLabel = "DESHACER",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoTransfer(savedTransferId)
            }
            onClearSavedTransferId()
        }
    }

    AccountDetailScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onTransferClick = { onTransferClick(viewModel.accountId) },
        onMovementsClick = onMovementsClick,
        onAdvancesClick = onAdvancesClick,
        onOpenAdjustmentDialog = viewModel::openAdjustmentDialog,
        onDismissAdjustmentDialog = viewModel::dismissAdjustmentDialog,
        onAdjustmentSignChanged = viewModel::onAdjustmentSignChanged,
        onAdjustmentAmountChanged = viewModel::onAdjustmentAmountChanged,
        onAdjustmentReasonChanged = viewModel::onAdjustmentReasonChanged,
        onSaveAdjustment = viewModel::saveBalanceAdjustment,
        onOpenInitialBalanceDialog = viewModel::openInitialBalanceDialog,
        onDismissInitialBalanceDialog = viewModel::dismissInitialBalanceDialog,
        onInitialBalanceChanged = viewModel::onInitialBalanceChanged,
        onSaveInitialBalance = viewModel::saveInitialBalance,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountDetailScreen(
    uiState: AccountDetailUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onTransferClick: () -> Unit,
    onMovementsClick: () -> Unit,
    onAdvancesClick: () -> Unit,
    onOpenAdjustmentDialog: () -> Unit,
    onDismissAdjustmentDialog: () -> Unit,
    onAdjustmentSignChanged: (Boolean) -> Unit,
    onAdjustmentAmountChanged: (String) -> Unit,
    onAdjustmentReasonChanged: (String) -> Unit,
    onSaveAdjustment: () -> Unit,
    onOpenInitialBalanceDialog: () -> Unit,
    onDismissInitialBalanceDialog: () -> Unit,
    onInitialBalanceChanged: (String) -> Unit,
    onSaveInitialBalance: () -> Unit,
) {
    val scrollState = rememberScrollState()
    val account = uiState.account
    val isFamily = account?.type == AccountType.FAMILY
    val accountTitle = account?.name ?: "Cuenta"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = accountTitle,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 1. Tarjeta de saldo disponible real
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 130.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFamily) Color.Black else Color(0xFFF5F5F5),
                    contentColor = if (isFamily) Color.White else Color.Black,
                ),
                border = if (isFamily) null else BorderStroke(1.dp, Color(0xFFE0E0E0)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Saldo disponible (caja real)",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.Gray,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    MoneyText(
                        amountMinor = uiState.cashBalanceMinor,
                        isPrivacyModeEnabled = uiState.privacyModeEnabled,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                        ),
                        color = PetroleumBluePrimary,
                    )
                }
            }

            // 2. Accesos principales: Transferir y Movimientos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onTransferClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PetroleumBluePrimary,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Transferir",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }

                OutlinedButton(
                    onClick = onMovementsClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, OutlineBorder),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                        contentDescription = null,
                        tint = PetroleumBluePrimary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Movimientos",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = PetroleumBluePrimary,
                    )
                }
            }

            // 3. Acceso a Adelantos
            Surface(
                onClick = onAdvancesClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = PureWhiteSurface,
                border = BorderStroke(1.dp, OutlineBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = PetroleumBluePrimary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Adelantos entre cuentas",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (uiState.netDebtMinor == 0L) {
                            Text(
                                text = "Sin adelantos pendientes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.DarkGray,
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val prefix = if (uiState.netDebtMinor > 0L) "Familia debe " else "Personal debe "
                                val suffix = if (uiState.netDebtMinor > 0L) " a Personal" else " a Familia"
                                Text(
                                    text = prefix,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PetroleumBluePrimary,
                                )
                                MoneyText(
                                    amountMinor = abs(uiState.netDebtMinor),
                                    isPrivacyModeEnabled = uiState.privacyModeEnabled,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = PetroleumBluePrimary,
                                )
                                Text(
                                    text = suffix,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = PetroleumBluePrimary,
                                )
                            }
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Ver adelantos",
                        tint = Color.Gray,
                    )
                }
            }

            // 4. Gestión de saldo (Ajuste auditable y Saldo inicial)
            Text(
                text = "Gestión de saldo",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp),
            )

            Surface(
                onClick = onOpenAdjustmentDialog,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ajuste de saldo",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.Black,
                        )
                        Text(
                            text = "Corrección auditable con motivo (fuera de estadísticas)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray,
                    )
                }
            }

            Surface(
                onClick = onOpenInitialBalanceDialog,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Saldo inicial",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.Black,
                        )
                        Text(
                            text = "Configurado: ${formatCurrency(account?.initialBalanceMinor ?: 0L)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray,
                    )
                }
            }
        }
    }

    // Diálogo: Ajuste de saldo auditable
    if (uiState.isAdjustmentDialogOpen) {
        AlertDialog(
            onDismissRequest = onDismissAdjustmentDialog,
            title = { Text("Ajuste de saldo") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Los ajustes modifican el saldo de caja real sin contar como ingreso ni gasto estadístico.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SignChip(
                            label = "+ Añadir",
                            isSelected = uiState.adjustmentIsPositive,
                            modifier = Modifier.weight(1f),
                            onClick = { onAdjustmentSignChanged(true) },
                        )
                        SignChip(
                            label = "− Restar",
                            isSelected = !uiState.adjustmentIsPositive,
                            modifier = Modifier.weight(1f),
                            onClick = { onAdjustmentSignChanged(false) },
                        )
                    }
                    OutlinedTextField(
                        value = uiState.adjustmentAmountInput,
                        onValueChange = onAdjustmentAmountChanged,
                        label = { Text("Importe (€)") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = uiState.adjustmentReasonInput,
                        onValueChange = onAdjustmentReasonChanged,
                        label = { Text("Motivo (obligatorio)") },
                        placeholder = { Text("Ej. Cuadre bancario") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    uiState.adjustmentErrorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onSaveAdjustment,
                    enabled = !uiState.isSaving,
                ) {
                    Text("Guardar ajuste", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissAdjustmentDialog) {
                    Text("Cancelar")
                }
            },
        )
    }

    // Diálogo: Configurar saldo inicial
    if (uiState.isInitialBalanceDialogOpen) {
        AlertDialog(
            onDismissRequest = onDismissInitialBalanceDialog,
            title = { Text("Saldo inicial") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Establece el saldo inicial de arranque para esta cuenta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                    OutlinedTextField(
                        value = uiState.initialBalanceInput,
                        onValueChange = onInitialBalanceChanged,
                        label = { Text("Saldo inicial (€)") },
                        placeholder = { Text("0,00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    uiState.initialBalanceErrorMessage?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = onSaveInitialBalance,
                    enabled = !uiState.isSaving,
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissInitialBalanceDialog) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun SignChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
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

private fun formatCurrency(minorAmount: Long): String {
    val amount = minorAmount / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    format.currency = Currency.getInstance("EUR")
    return format.format(amount)
}
