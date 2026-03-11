package com.familyexpenses.app.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onHistoryClick: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Dashboard mensual",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Balance limpio del mes actual.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SummaryCard(
                title = "Saldo personal",
                amountMinor = uiState.personalBalanceMinor,
                onPlusClick = onAddPersonalIncomeClick,
                onMinusClick = onAddPersonalExpenseClick,
            )
            SummaryCard(
                title = "Saldo familiar",
                amountMinor = uiState.familyBalanceMinor,
                onPlusClick = onAddFamilyIncomeClick,
                onMinusClick = onAddFamilyExpenseClick,
            )
            SummaryCard(
                title = "Pendiente familia a personal",
                amountMinor = uiState.pendingReimbursementMinor,
            )

            OutlinedButton(
                onClick = onAddFamilyExpensePaidFromPersonalClick,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Anadir gasto familiar pagado con personal")
            }

            Button(
                onClick = onHistoryClick,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Ver historial")
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
