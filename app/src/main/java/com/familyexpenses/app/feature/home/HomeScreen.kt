package com.familyexpenses.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.core.ui.MoneyText
import com.familyexpenses.app.domain.model.HistoryItem
import com.familyexpenses.app.domain.model.MovementKind
import com.familyexpenses.app.ui.theme.ExpenseBorder
import com.familyexpenses.app.ui.theme.ExpenseContainer
import com.familyexpenses.app.ui.theme.IncomeBorder
import com.familyexpenses.app.ui.theme.IncomeContainer
import com.familyexpenses.app.ui.theme.OffWhiteBackground
import com.familyexpenses.app.ui.theme.OnExpenseContainer
import com.familyexpenses.app.ui.theme.OnIncomeContainer
import com.familyexpenses.app.ui.theme.OutlineBorder
import com.familyexpenses.app.ui.theme.PetroleumBlueContainer
import com.familyexpenses.app.ui.theme.PetroleumBluePrimary
import com.familyexpenses.app.ui.theme.PureWhiteSurface
import com.familyexpenses.app.ui.theme.TextSecondaryColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun HomeRoute(
    onFamilyAccountClick: () -> Unit = {},
    onPersonalAccountClick: () -> Unit = {},
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onPendingSettlementClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecurringClick: () -> Unit = {},
    onMovementClick: (kind: MovementKind, id: String) -> Unit = { _, _ -> },
    savedTransactionId: String? = null,
    onClearSavedTransactionId: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(savedTransactionId) {
        if (!savedTransactionId.isNullOrBlank()) {
            val result = snackbarHostState.showSnackbar(
                message = "Movimiento guardado",
                actionLabel = "DESHACER",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoTransaction(savedTransactionId)
            }
            onClearSavedTransactionId()
        }
    }

    HomeScreen(
        uiState = uiState.value,
        snackbarHostState = snackbarHostState,
        onTogglePrivacyMode = viewModel::togglePrivacyMode,
        onFamilyAccountClick = onFamilyAccountClick,
        onPersonalAccountClick = onPersonalAccountClick,
        onAddExpenseClick = onAddExpenseClick,
        onAddIncomeClick = onAddIncomeClick,
        onPendingSettlementClick = onPendingSettlementClick,
        onHistoryClick = onHistoryClick,
        onAnalysisClick = onAnalysisClick,
        onSettingsClick = onSettingsClick,
        onRecurringClick = onRecurringClick,
        onMovementClick = onMovementClick,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onTogglePrivacyMode: () -> Unit = {},
    onFamilyAccountClick: () -> Unit = {},
    onPersonalAccountClick: () -> Unit = {},
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onPendingSettlementClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecurringClick: () -> Unit = {},
    onMovementClick: (kind: MovementKind, id: String) -> Unit = { _, _ -> },
) {
    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = OffWhiteBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Buenos días",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        )
                        Text(
                            text = "Resumen familiar",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondaryColor,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onTogglePrivacyMode) {
                        Icon(
                            imageVector = if (uiState.privacyModeEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (uiState.privacyModeEnabled) "Mostrar importes" else "Ocultar importes",
                            tint = PetroleumBluePrimary,
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                            tint = PetroleumBluePrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OffWhiteBackground,
                    titleContentColor = PetroleumBluePrimary,
                ),
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Acciones rápidas fijas abajo para uso con una sola mano: "Ingreso" (+) y "Gasto" (−)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = OffWhiteBackground,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(color = OutlineBorder)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            QuickActionButton(
                                label = "Ingreso",
                                icon = Icons.Default.Add,
                                containerColor = IncomeContainer,
                                borderColor = IncomeBorder,
                                contentColor = OnIncomeContainer,
                                onClick = onAddIncomeClick,
                                modifier = Modifier.weight(1f),
                            )
                            QuickActionButton(
                                label = "Gasto",
                                icon = Icons.Default.Remove,
                                containerColor = ExpenseContainer,
                                borderColor = ExpenseBorder,
                                contentColor = OnExpenseContainer,
                                onClick = onAddExpenseClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                NavigationBar(
                    containerColor = PureWhiteSurface,
                    tonalElevation = 4.dp,
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = { /* Ya en Inicio */ },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                        label = { Text("Inicio") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PureWhiteSurface,
                            selectedTextColor = PetroleumBluePrimary,
                            indicatorColor = PetroleumBluePrimary,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                        ),
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onHistoryClick,
                        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Movimientos") },
                        label = { Text("Movimientos") },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                        ),
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onAnalysisClick,
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Análisis") },
                        label = { Text("Análisis") },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                        ),
                    )
                }
            }
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
            // 1. Tarjetas de saldo del mes actual: Familia y Personal con el mismo peso e igual jerarquía visual
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AccountSummaryCard(
                    accountName = "Familia",
                    monthlyNetMinor = uiState.familyMonthlyNetMinor,
                    monthName = uiState.currentMonthName,
                    icon = Icons.Default.HomeWork,
                    privacyModeEnabled = uiState.privacyModeEnabled,
                    onClick = onFamilyAccountClick,
                    modifier = Modifier.weight(1f),
                )
                AccountSummaryCard(
                    accountName = "Personal",
                    monthlyNetMinor = uiState.personalMonthlyNetMinor,
                    monthName = uiState.currentMonthName,
                    icon = Icons.Default.Person,
                    privacyModeEnabled = uiState.privacyModeEnabled,
                    onClick = onPersonalAccountClick,
                    modifier = Modifier.weight(1f),
                )
            }

            // 2. Bloque de Adelantos siempre visible
            AdvancesBlock(
                netDebtMinor = uiState.netDebtMinor,
                privacyModeEnabled = uiState.privacyModeEnabled,
                onClick = onPendingSettlementClick,
            )

            // 4. Banner discreto de recurrentes pendientes
            if (uiState.pendingRecurrencesCount > 0) {
                PendingRecurrencesBanner(
                    count = uiState.pendingRecurrencesCount,
                    onClick = onRecurringClick,
                )
            }

            // 5. Sección de Últimos Movimientos (3 ítems más recientes)
            RecentMovementsSection(
                recentMovements = uiState.recentMovements,
                privacyModeEnabled = uiState.privacyModeEnabled,
                onMovementClick = onMovementClick,
                onViewAllClick = onHistoryClick,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Botón grande de acción rápida inferior con protagonismo en el signo (+ / −).
 */
@Composable
private fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    borderColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 74.dp),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = PureWhiteSurface,
                modifier = Modifier.size(44.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier
                        .padding(9.dp)
                        .size(26.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Tarjeta de saldo del mes actual con la misma jerarquía para Familia y Personal.
 */
@Composable
private fun AccountSummaryCard(
    accountName: String,
    monthlyNetMinor: Long,
    monthName: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    privacyModeEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val monthlyLabel = if (monthName.isNotBlank()) "Saldo de $monthName" else "Saldo del mes"
    val monthlyColor = when {
        privacyModeEnabled || monthlyNetMinor == 0L -> PetroleumBluePrimary
        monthlyNetMinor > 0L -> OnIncomeContainer
        else -> OnExpenseContainer
    }

    Card(
        onClick = onClick,
        modifier = modifier.heightIn(min = 114.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PureWhiteSurface,
            contentColor = PetroleumBluePrimary,
        ),
        border = BorderStroke(1.dp, OutlineBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PetroleumBluePrimary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = accountName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PetroleumBluePrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = monthlyLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            MoneyText(
                amountMinor = monthlyNetMinor,
                isPrivacyModeEnabled = privacyModeEnabled,
                showPlusSign = false,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = monthlyColor,
            )
        }
    }
}

/**
 * Bloque de Adelantos siempre visible con soporte de privacidad.
 */
@Composable
private fun AdvancesBlock(
    netDebtMinor: Long,
    privacyModeEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = PetroleumBluePrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (netDebtMinor == 0L) {
                    Text(
                        text = "Sin adelantos pendientes",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val prefix = if (netDebtMinor > 0L) "Familia te debe " else "Personal debe a Familia "
                        Text(
                            text = prefix,
                            style = MaterialTheme.typography.bodySmall,
                            color = PetroleumBluePrimary,
                        )
                        MoneyText(
                            amountMinor = abs(netDebtMinor),
                            isPrivacyModeEnabled = privacyModeEnabled,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = PetroleumBluePrimary,
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver adelantos",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Sección de los 3 últimos movimientos en Inicio.
 */
@Composable
private fun RecentMovementsSection(
    recentMovements: List<HistoryItem>,
    privacyModeEnabled: Boolean,
    onMovementClick: (kind: MovementKind, id: String) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Últimos movimientos",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PetroleumBluePrimary,
            )
            TextButton(onClick = onViewAllClick) {
                Text(
                    text = "Ver todos",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = PetroleumBluePrimary,
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = PureWhiteSurface,
            border = BorderStroke(1.dp, OutlineBorder),
        ) {
            if (recentMovements.isEmpty()) {
                Text(
                    text = "No hay movimientos registrados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                Column {
                    recentMovements.forEachIndexed { index, item ->
                        RecentMovementRow(
                            item = item,
                            privacyModeEnabled = privacyModeEnabled,
                            onClick = { onMovementClick(item.kind, item.id) },
                        )
                        if (index < recentMovements.size - 1) {
                            HorizontalDivider(color = OutlineBorder)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fila individual de movimiento reciente en Inicio.
 */
@Composable
private fun RecentMovementRow(
    item: HistoryItem,
    privacyModeEnabled: Boolean,
    onClick: () -> Unit,
) {
    val badgeContainerColor = when (item.kind) {
        MovementKind.INCOME -> IncomeContainer
        MovementKind.EXPENSE -> ExpenseContainer
        MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> PetroleumBlueContainer
    }
    val badgeIconTint = when (item.kind) {
        MovementKind.INCOME -> OnIncomeContainer
        MovementKind.EXPENSE -> OnExpenseContainer
        MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> PetroleumBluePrimary
    }
    val badgeIcon = when (item.kind) {
        MovementKind.INCOME -> Icons.Default.Add
        MovementKind.EXPENSE -> Icons.Default.Remove
        MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> Icons.Default.SwapHoriz
    }
    val amountColor = when (item.kind) {
        MovementKind.INCOME -> OnIncomeContainer
        MovementKind.EXPENSE -> OnExpenseContainer
        MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> PetroleumBluePrimary
    }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = badgeContainerColor,
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = badgeIconTint,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.note ?: item.categoryName ?: when (item.kind) {
                        MovementKind.NORMAL_TRANSFER -> "Transferencia"
                        MovementKind.REIMBURSEMENT -> "Devolución"
                        else -> "Movimiento"
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = PetroleumBluePrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val dateLabel = item.occurredAt.toRelativeDateText()
                val accountLabel = if (item.kind == MovementKind.EXPENSE) item.responsibleAccountName else item.payingAccountName
                Text(
                    text = "$dateLabel · $accountLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryColor,
                )
            }

            MoneyText(
                amountMinor = item.amountMinor,
                isPrivacyModeEnabled = privacyModeEnabled,
                showPlusSign = item.kind == MovementKind.INCOME,
                showMinusSign = item.kind == MovementKind.EXPENSE,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = amountColor,
            )
        }
    }
}

@Composable
private fun PendingRecurrencesBanner(
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = PureWhiteSurface,
        border = BorderStroke(1.dp, OutlineBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = PetroleumBluePrimary,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = if (count == 1) "1 recurrente pendiente" else "$count recurrentes pendientes",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = PetroleumBluePrimary,
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

private fun Long.toRelativeDateText(): String {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val movementDate = Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    return when {
        movementDate == today -> "Hoy"
        movementDate == today.minusDays(1) -> "Ayer"
        else -> movementDate.format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "ES")))
    }
}
