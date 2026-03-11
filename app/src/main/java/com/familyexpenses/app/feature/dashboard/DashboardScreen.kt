package com.familyexpenses.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun DashboardRoute(
    onAddPersonalIncomeClick: () -> Unit,
    onAddPersonalExpenseClick: () -> Unit,
    onAddFamilyIncomeClick: () -> Unit,
    onAddFamilyExpenseClick: () -> Unit,
    onAddFamilyExpensePaidFromPersonalClick: () -> Unit,
    onPendingSettlementClick: () -> Unit,
    onRecurringClick: () -> Unit,
    onHistoryClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        uiState = uiState.value,
        onAddPersonalIncomeClick = onAddPersonalIncomeClick,
        onAddPersonalExpenseClick = onAddPersonalExpenseClick,
        onAddFamilyIncomeClick = onAddFamilyIncomeClick,
        onAddFamilyExpenseClick = onAddFamilyExpenseClick,
        onAddFamilyExpensePaidFromPersonalClick = onAddFamilyExpensePaidFromPersonalClick,
        onPendingSettlementClick = onPendingSettlementClick,
        onRecurringClick = onRecurringClick,
        onHistoryClick = onHistoryClick,
    )
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onAddPersonalIncomeClick: () -> Unit,
    onAddPersonalExpenseClick: () -> Unit,
    onAddFamilyIncomeClick: () -> Unit,
    onAddFamilyExpenseClick: () -> Unit,
    onAddFamilyExpensePaidFromPersonalClick: () -> Unit,
    onPendingSettlementClick: () -> Unit,
    onRecurringClick: () -> Unit,
    onHistoryClick: () -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val compactLayout = maxHeight < 760.dp
            val horizontalPadding = if (compactLayout) 16.dp else 20.dp
            val verticalPadding = if (compactLayout) 16.dp else 24.dp
            val sectionSpacing = if (compactLayout) 16.dp else 24.dp
            val actionPadding = if (compactLayout) 14.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = horizontalPadding, vertical = verticalPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                Text(
                    text = "Dashboard mensual",
                    style = if (compactLayout) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                )

                SummaryCard(
                    title = "Personal",
                    amountMinor = uiState.personalBalanceMinor,
                    onPlusClick = onAddPersonalIncomeClick,
                    onMinusClick = onAddPersonalExpenseClick,
                )
                SummaryCard(
                    title = "Familiar",
                    amountMinor = uiState.familyBalanceMinor,
                    onPlusClick = onAddFamilyIncomeClick,
                    onMinusClick = onAddFamilyExpenseClick,
                )
                SummaryCard(
                    title = "Pendiente familia a personal",
                    amountMinor = uiState.pendingReimbursementMinor,
                )

                OutlinedButton(
                    onClick = onPendingSettlementClick,
                    enabled = uiState.pendingReimbursementMinor > 0L,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = actionPadding),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Liquidar pendiente")
                }

                OutlinedButton(
                    onClick = onRecurringClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = actionPadding),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Recurrentes")
                }

                Button(
                    onClick = onHistoryClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = actionPadding),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Ver historial")
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amountMinor: Long,
    onPlusClick: (() -> Unit)? = null,
    onMinusClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (onPlusClick != null && onMinusClick != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MinimalActionButton(label = "+", onClick = onPlusClick)
                        MinimalActionButton(label = "-", onClick = onMinusClick)
                    }
                }
            }
            HorizontalDivider()
            Text(
                text = amountMinor.toCurrencyText(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun MinimalActionButton(
    label: String,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.width(44.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

private fun Long.toCurrencyText(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "ES")).apply {
        currency = Currency.getInstance("EUR")
    }
    return formatter.format(this / 100.0)
}
