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
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import java.text.NumberFormat
import java.util.Currency
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
        onFamilyAccountClick = onFamilyAccountClick,
        onPersonalAccountClick = onPersonalAccountClick,
        onAddExpenseClick = onAddExpenseClick,
        onAddIncomeClick = onAddIncomeClick,
        onPendingSettlementClick = onPendingSettlementClick,
        onHistoryClick = onHistoryClick,
        onAnalysisClick = onAnalysisClick,
        onSettingsClick = onSettingsClick,
        onRecurringClick = onRecurringClick,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    uiState: HomeUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onFamilyAccountClick: () -> Unit = {},
    onPersonalAccountClick: () -> Unit = {},
    onAddExpenseClick: () -> Unit,
    onAddIncomeClick: () -> Unit,
    onPendingSettlementClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecurringClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Family Expenses",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 4.dp,
            ) {
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Ya en Inicio */ },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                    label = { Text("Inicio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.Black,
                        indicatorColor = Color.Black,
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
            // 1. Tarjetas de saldo real: Familia (Negra) y Personal (Gris claro) con igual jerarquía
            FamilyAccountCard(
                balanceMinor = uiState.familyCashBalanceMinor,
                onClick = onFamilyAccountClick,
            )
            PersonalAccountCard(
                balanceMinor = uiState.personalCashBalanceMinor,
                onClick = onPersonalAccountClick,
            )

            // 2. Botones grandes: «Nuevo gasto» (principal negro) y «Nuevo ingreso» (secundario)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Botón principal: Nuevo gasto
                Button(
                    onClick = onAddExpenseClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                    ),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nuevo gasto",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }

                // Botón secundario: Nuevo ingreso
                Surface(
                    onClick = onAddIncomeClick,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 54.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF5F5F5),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nuevo ingreso",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black,
                        )
                    }
                }
            }

            // 3. Bloque de Adelantos siempre visible (incluso a cero)
            AdvancesBlock(
                netDebtMinor = uiState.netDebtMinor,
                onClick = onPendingSettlementClick,
            )

            // 4. Aviso discreto de recurrentes pendientes
            if (uiState.pendingRecurrencesCount > 0) {
                PendingRecurrencesBanner(
                    count = uiState.pendingRecurrencesCount,
                    onClick = onRecurringClick,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
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
        color = Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
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
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = if (count == 1) "1 recurrente pendiente" else "$count recurrentes pendientes",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.Black,
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

/**
 * Tarjeta de saldo real para Cuenta Familia: fondo negro, texto blanco.
 */
@Composable
private fun FamilyAccountCard(
    balanceMinor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black,
            contentColor = Color.White,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Familia",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFCCCCCC),
                )
                Text(
                    text = "Caja real",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF999999),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatCurrency(balanceMinor),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Tarjeta de saldo real para Cuenta Personal: fondo gris claro, texto negro.
 */
@Composable
private fun PersonalAccountCard(
    balanceMinor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 124.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5),
            contentColor = Color.Black,
        ),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Personal",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF555555),
                )
                Text(
                    text = "Caja real",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF777777),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatCurrency(balanceMinor),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                ),
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Bloque de Adelantos siempre visible (incluso a cero).
 */
@Composable
private fun AdvancesBlock(
    netDebtMinor: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFAFAFA),
        border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
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
                tint = Color.DarkGray,
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
                val debtText = when {
                    netDebtMinor > 0L -> "Familia te debe ${formatCurrency(netDebtMinor)}"
                    netDebtMinor < 0L -> "Personal debe a Familia ${formatCurrency(abs(netDebtMinor))}"
                    else -> "Sin adelantos pendientes"
                }
                Text(
                    text = debtText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (netDebtMinor != 0L) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (netDebtMinor != 0L) Color.Black else Color.DarkGray,
                )
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

private fun formatCurrency(minorAmount: Long): String {
    val amount = minorAmount / 100.0
    val format = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    format.currency = Currency.getInstance("EUR")
    return format.format(amount)
}
