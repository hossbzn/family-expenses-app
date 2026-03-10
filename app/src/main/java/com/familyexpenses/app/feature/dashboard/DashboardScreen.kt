package com.familyexpenses.app.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun DashboardRoute(
    onAddExpenseClick: () -> Unit,
    onAddFamilyPaidWithPersonalClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        uiState = uiState.value,
        onAddExpenseClick = onAddExpenseClick,
        onAddFamilyPaidWithPersonalClick = onAddFamilyPaidWithPersonalClick,
    )
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onAddExpenseClick: () -> Unit,
    onAddFamilyPaidWithPersonalClick: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF6EFE6),
                            Color(0xFFFDFBF7),
                        ),
                    ),
                )
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Dashboard mensual",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Resumen rapido del mes actual y del pendiente familia a personal.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SummaryCard(
                title = "Saldo personal",
                amountMinor = uiState.personalBalanceMinor,
                containerColor = Color(0xFFD9F0E4),
            )
            SummaryCard(
                title = "Saldo familiar",
                amountMinor = uiState.familyBalanceMinor,
                containerColor = Color(0xFFF7DFC7),
            )
            SummaryCard(
                title = "Pendiente familia a personal",
                amountMinor = uiState.pendingReimbursementMinor,
                containerColor = Color(0xFFF2D7D5),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onAddExpenseClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text("+ Anadir gasto")
                }
                Button(
                    onClick = onAddFamilyPaidWithPersonalClick,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    Text("+ Gasto familiar pagado con personal")
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amountMinor: Long,
    containerColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(containerColor)
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = amountMinor.toCurrencyText(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun Long.toCurrencyText(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "ES")).apply {
        currency = Currency.getInstance("EUR")
    }
    return formatter.format(this / 100.0)
}
