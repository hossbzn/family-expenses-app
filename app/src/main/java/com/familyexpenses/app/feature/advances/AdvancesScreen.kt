package com.familyexpenses.app.feature.advances

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.domain.model.MovementKind
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlin.math.abs

@Composable
fun AdvancesRoute(
    onBack: () -> Unit,
    onReimburseClick: (debtorAccountId: String) -> Unit,
    onMovementClick: (kind: MovementKind, id: String) -> Unit,
    viewModel: AdvancesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AdvancesScreen(
        uiState = uiState,
        onBack = onBack,
        onFilterSelected = viewModel::onFilterSelected,
        onReimburseClick = {
            uiState.debtorAccountId?.let(onReimburseClick)
        },
        onMovementClick = onMovementClick,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdvancesScreen(
    uiState: AdvancesUiState,
    onBack: () -> Unit,
    onFilterSelected: (AdvancesHistoryFilter) -> Unit,
    onReimburseClick: () -> Unit,
    onMovementClick: (kind: MovementKind, id: String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Adelantos",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 1. Tarjeta de Deuda Neta (siempre visible, incluyendo estado cero)
            NetDebtSummaryCard(
                netDebtMinor = uiState.netDebtMinor,
                advancesCount = uiState.advancesCount,
                reimbursementsCount = uiState.reimbursementsCount,
                onReimburseClick = onReimburseClick,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            // 2. Filtros: Todos / Adelantos / Devoluciones
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AdvanceFilterChip(
                    label = "Todos",
                    selected = uiState.selectedFilter == AdvancesHistoryFilter.ALL,
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterSelected(AdvancesHistoryFilter.ALL) },
                )
                AdvanceFilterChip(
                    label = "Adelantos",
                    selected = uiState.selectedFilter == AdvancesHistoryFilter.ADVANCES,
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterSelected(AdvancesHistoryFilter.ADVANCES) },
                )
                AdvanceFilterChip(
                    label = "Devoluciones",
                    selected = uiState.selectedFilter == AdvancesHistoryFilter.REIMBURSEMENTS,
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterSelected(AdvancesHistoryFilter.REIMBURSEMENTS) },
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))

            // 3. Historial cronológico único con trazabilidad al origen
            if (uiState.items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when (uiState.selectedFilter) {
                            AdvancesHistoryFilter.ALL -> "No hay adelantos ni devoluciones registrados."
                            AdvancesHistoryFilter.ADVANCES -> "No hay adelantos registrados."
                            AdvancesHistoryFilter.REIMBURSEMENTS -> "No hay devoluciones registradas."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    items(
                        items = uiState.items,
                        key = { "${it.kind.name}_${it.id}" },
                    ) { item ->
                        AdvanceHistoryRow(
                            item = item,
                            onClick = { onMovementClick(item.kind, item.id) },
                        )
                        HorizontalDivider(
                            color = Color(0xFFF3F3F3),
                            modifier = Modifier.padding(start = 60.dp, end = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetDebtSummaryCard(
    netDebtMinor: Long,
    advancesCount: Int,
    reimbursementsCount: Int,
    onReimburseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasDebt = netDebtMinor != 0L
    val cardColor = if (hasDebt) Color.Black else Color(0xFFF5F5F5)
    val contentColor = if (hasDebt) Color.White else Color.Black

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = contentColor,
        ),
        border = if (hasDebt) null else BorderStroke(1.dp, Color(0xFFE0E0E0)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val statusTitle = when {
                netDebtMinor > 0L -> "Familia debe a Personal"
                netDebtMinor < 0L -> "Personal debe a Familia"
                else -> "Sin adelantos pendientes"
            }
            val statusSubtitle = when {
                netDebtMinor > 0L -> "Deuda neta pendiente de devolver a Personal"
                netDebtMinor < 0L -> "Deuda neta pendiente de devolver a Familia"
                else -> "Las cuentas están al día"
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (hasDebt) Icons.Default.SwapHoriz else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (hasDebt) Color(0xFFCCCCCC) else Color.DarkGray,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = statusTitle,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (hasDebt) Color(0xFFDDDDDD) else Color.Black,
                )
            }

            Text(
                text = formatCurrency(abs(netDebtMinor)),
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                ),
                color = contentColor,
            )

            Text(
                text = "$statusSubtitle · $advancesCount adelantos, $reimbursementsCount devoluciones",
                style = MaterialTheme.typography.bodySmall,
                color = if (hasDebt) Color(0xFFAAAAAA) else Color.Gray,
            )

            if (hasDebt) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onReimburseClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text(
                        text = "Registrar devolución",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvanceFilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) Color.Black else Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, if (selected) Color.Black else Color(0xFFE0E0E0)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (selected) Color.White else Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AdvanceHistoryRow(
    item: AdvanceHistoryRowUiModel,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        color = Color.Transparent,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (item.isReimbursement) Color(0xFFEAEAEA) else Color(0xFFF5F5F5),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.isReimbursement) {
                            Icons.AutoMirrored.Filled.Redo
                        } else {
                            Icons.Default.SwapHoriz
                        },
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    Text(
                        text = item.amountText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        maxLines = 1,
                    )
                }
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun formatCurrency(minorAmount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "ES")).apply {
        currency = Currency.getInstance("EUR")
    }
    return formatter.format(minorAmount / 100.0)
}
