package com.familyexpenses.app.feature.recurrence

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.core.model.TransactionType
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@Composable
fun RecurrenceRoute(
    onBack: () -> Unit,
    viewModel: RecurrenceViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    RecurrenceScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onTypeSelected = viewModel::onTypeSelected,
        onAccountSelected = viewModel::onAccountSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onAmountChanged = viewModel::onAmountChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onStartDateChanged = viewModel::onStartDateChanged,
        onEndDateChanged = viewModel::onEndDateChanged,
        onPaidFromPersonalChanged = viewModel::onPaidFromPersonalChanged,
        onSave = viewModel::save,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RecurrenceScreen(
    uiState: RecurrenceUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onAccountSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onPaidFromPersonalChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurrentes") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Text(
                    text = "Los recurrentes del MVP se generan el dia 1 de cada mes, dentro del rango que indiques.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                SectionBlock(title = "Nueva regla") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        RowChips(
                            title = "Tipo",
                            items = listOf(
                                ChipItem("Gasto", uiState.selectedType == TransactionType.EXPENSE) { onTypeSelected(TransactionType.EXPENSE) },
                                ChipItem("Ingreso", uiState.selectedType == TransactionType.INCOME) { onTypeSelected(TransactionType.INCOME) },
                            ),
                        )
                        RowChips(
                            title = "Cuenta",
                            items = uiState.accountOptions.map { account ->
                                ChipItem(account.name, uiState.selectedAccountId == account.id, enabled = !uiState.paidFromPersonal) {
                                    onAccountSelected(account.id)
                                }
                            },
                        )
                        if (uiState.selectedType == TransactionType.EXPENSE) {
                            SectionBlock(title = "Pagado con personal") {
                                Switch(
                                    checked = uiState.paidFromPersonal,
                                    onCheckedChange = onPaidFromPersonalChanged,
                                )
                            }
                        }
                        RowChips(
                            title = "Categoria",
                            items = uiState.categoryOptions.map { category ->
                                ChipItem(category.name, uiState.selectedCategoryId == category.id) {
                                    onCategorySelected(category.id)
                                }
                            },
                        )
                        OutlinedTextField(
                            value = uiState.amountInput,
                            onValueChange = onAmountChanged,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Importe") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(14.dp),
                        )
                        DateField(
                            label = "Inicio",
                            value = uiState.startDateInput,
                            onDateSelected = onStartDateChanged,
                        )
                        OptionalDateField(
                            label = "Fin",
                            value = uiState.endDateInput,
                            onDateSelected = onEndDateChanged,
                            onClear = { onEndDateChanged("") },
                        )
                        OutlinedTextField(
                            value = uiState.noteInput,
                            onValueChange = onNoteChanged,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Nota opcional") },
                            shape = RoundedCornerShape(14.dp),
                        )
                        Button(
                            onClick = onSave,
                            enabled = !uiState.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Text(if (uiState.isSaving) "Guardando..." else "Guardar recurrente")
                        }
                    }
                }
            }
            item {
                Text(
                    text = "Reglas guardadas",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (uiState.rules.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = CardDefaults.outlinedCardBorder(),
                    ) {
                        Text(
                            text = "Todavia no hay reglas recurrentes.",
                            modifier = Modifier.padding(18.dp),
                        )
                    }
                }
            } else {
                items(uiState.rules, key = { it.id }) { rule ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = CardDefaults.outlinedCardBorder(),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                text = rule.note ?: "Recurrente mensual",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            HorizontalDivider()
                            Text(rule.amountMinor.toCurrencyText(), style = MaterialTheme.typography.titleLarge)
                            Text(
                                text = "Siguiente generacion: ${rule.nextOccurrenceAt.toDateText()} - dia 1 de cada mes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = "Vigencia: ${rule.startAt.toDateText()} -> ${rule.endAt?.toDateText() ?: "Sin fin"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = if (rule.isActive) "Activa" else "Finalizada",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ChipItem(
    val label: String,
    val selected: Boolean,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
private fun RowChips(title: String, items: List<ChipItem>) {
    SectionBlock(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                FilterChip(
                    selected = item.selected,
                    enabled = item.enabled,
                    onClick = item.onClick,
                    label = { Text(item.label) },
                    shape = RoundedCornerShape(12.dp),
                )
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val selectedDate = value.toLocalDateOrNull() ?: LocalDate.now()

    OutlinedTextField(
        value = value,
        onValueChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        onDateSelected(LocalDate.of(year, month + 1, dayOfMonth).format(DateTimeFormatter.ISO_LOCAL_DATE))
                    },
                    selectedDate.year,
                    selectedDate.monthValue - 1,
                    selectedDate.dayOfMonth,
                ).show()
            },
        readOnly = true,
        singleLine = true,
        label = { Text(label) },
        supportingText = { Text("Toca para abrir el calendario") },
        shape = RoundedCornerShape(14.dp),
    )
}

@Composable
private fun OptionalDateField(
    label: String,
    value: String,
    onDateSelected: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DateField(
            label = label,
            value = value,
            onDateSelected = onDateSelected,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onClear) {
                Text(if (value.isBlank()) "Sin fecha fin" else "Quitar fecha fin")
            }
        }
    }
}

@Composable
private fun SectionBlock(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        HorizontalDivider()
        content()
    }
}

private fun Long.toCurrencyText(): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "ES")).apply {
        currency = Currency.getInstance("EUR")
    }
    return formatter.format(this / 100.0)
}

private fun Long.toDateText(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
    .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "ES")))

private fun String.toLocalDateOrNull(): LocalDate? = runCatching {
    LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
}.getOrNull()
