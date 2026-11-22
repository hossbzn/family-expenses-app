package com.familyexpenses.app.feature.recurrence

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.core.model.OccurrenceStatus
import com.familyexpenses.app.core.model.RecurrenceExecutionMode
import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.RecurrenceRuleStatus
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.ui.MoneyText
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.RecurrenceOccurrenceEntity
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.ui.theme.OffWhiteBackground
import com.familyexpenses.app.ui.theme.OutlineBorder
import com.familyexpenses.app.ui.theme.PetroleumBlueContainer
import com.familyexpenses.app.ui.theme.PetroleumBluePrimary
import com.familyexpenses.app.ui.theme.PureWhiteSurface
import java.math.RoundingMode
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
        onPayingAccountSelected = viewModel::onPayingAccountSelected,
        onResponsibleAccountSelected = viewModel::onResponsibleAccountSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onAmountChanged = viewModel::onAmountChanged,
        onNoteChanged = viewModel::onNoteChanged,
        onFrequencySelected = viewModel::onFrequencySelected,
        onIntervalCountSelected = viewModel::onIntervalCountSelected,
        onDayOfWeekSelected = viewModel::onDayOfWeekSelected,
        onDayOfMonthSelected = viewModel::onDayOfMonthSelected,
        onMonthOfYearSelected = viewModel::onMonthOfYearSelected,
        onModeSelected = viewModel::onModeSelected,
        onStartDateChanged = viewModel::onStartDateChanged,
        onEndDateChanged = viewModel::onEndDateChanged,
        onEditRule = viewModel::startEditRule,
        onResetForm = viewModel::resetForm,
        onSaveRule = viewModel::saveRule,
        onPauseRule = viewModel::pauseRule,
        onResumeRule = viewModel::resumeRule,
        onArchiveRule = viewModel::archiveRule,
        onRestoreRule = viewModel::restoreRule,
        onConfirmOccurrence = viewModel::confirmOccurrence,
        onConfirmAllPending = viewModel::confirmAllPendingOccurrences,
        onSkipOccurrence = viewModel::skipOccurrence,
        onPostponeOccurrence = viewModel::postponeOccurrence,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RecurrenceScreen(
    uiState: RecurrenceUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onTypeSelected: (TransactionType) -> Unit,
    onPayingAccountSelected: (String) -> Unit,
    onResponsibleAccountSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onFrequencySelected: (RecurrenceFrequency) -> Unit,
    onIntervalCountSelected: (Int) -> Unit,
    onDayOfWeekSelected: (Int) -> Unit,
    onDayOfMonthSelected: (Int) -> Unit,
    onMonthOfYearSelected: (Int) -> Unit,
    onModeSelected: (RecurrenceExecutionMode) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onEditRule: (RecurrenceRuleEntity) -> Unit,
    onResetForm: () -> Unit,
    onSaveRule: () -> Unit,
    onPauseRule: (String) -> Unit,
    onResumeRule: (String) -> Unit,
    onArchiveRule: (String) -> Unit,
    onRestoreRule: (String) -> Unit,
    onConfirmOccurrence: (String, Long?, Long?) -> Unit,
    onConfirmAllPending: () -> Unit,
    onSkipOccurrence: (String) -> Unit,
    onPostponeOccurrence: (String, Long) -> Unit,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isFormVisible by remember { mutableStateOf(false) }
    var selectedStatusFilter by remember { mutableStateOf(RecurrenceRuleStatus.ACTIVE) }
    var occurrenceToEdit by remember { mutableStateOf<RecurrenceOccurrenceEntity?>(null) }
    var occurrenceToPostpone by remember { mutableStateOf<RecurrenceOccurrenceEntity?>(null) }
    var selectedRuleDetail by remember { mutableStateOf<RecurrenceRuleEntity?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Movimientos Recurrentes", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
                )
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    indicator = { tabPositions ->
                        if (selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = Color.Black,
                            )
                        }
                    },
                ) {
                    Tab(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        text = {
                            Text(
                                if (uiState.pendingOccurrences.isNotEmpty()) {
                                    "Pendientes (${uiState.pendingOccurrences.size})"
                                } else {
                                    "Pendientes"
                                },
                                fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                    Tab(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        text = {
                            Text(
                                "Mis Reglas (${uiState.rules.size})",
                                fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (selectedTabIndex == 0) {
                // Pestaña Pendientes
                PendingOccurrencesContent(
                    occurrences = uiState.pendingOccurrences,
                    accounts = uiState.accountOptions,
                    categories = uiState.categoryOptions,
                    privacyModeEnabled = uiState.privacyModeEnabled,
                    onConfirm = { id -> onConfirmOccurrence(id, null, null) },
                    onEditBeforeConfirm = { occurrenceToEdit = it },
                    onPostpone = { occurrenceToPostpone = it },
                    onConfirmAll = onConfirmAllPending,
                    onSkip = onSkipOccurrence,
                )
            } else {
                // Pestaña Mis Reglas
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    if (isFormVisible) {
                                        onResetForm()
                                        isFormVisible = false
                                    } else {
                                        onResetForm()
                                        isFormVisible = true
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            ) {
                                Icon(
                                    imageVector = if (isFormVisible) Icons.Default.Close else Icons.Default.Add,
                                    contentDescription = null,
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isFormVisible) "Cerrar formulario" else "Nueva regla")
                            }
                        }
                    }

                    if (isFormVisible) {
                        item {
                            CreateOrEditRuleCard(
                                uiState = uiState,
                                onTypeSelected = onTypeSelected,
                                onPayingAccountSelected = onPayingAccountSelected,
                                onResponsibleAccountSelected = onResponsibleAccountSelected,
                                onCategorySelected = onCategorySelected,
                                onAmountChanged = onAmountChanged,
                                onNoteChanged = onNoteChanged,
                                onFrequencySelected = onFrequencySelected,
                                onIntervalCountSelected = onIntervalCountSelected,
                                onDayOfWeekSelected = onDayOfWeekSelected,
                                onDayOfMonthSelected = onDayOfMonthSelected,
                                onMonthOfYearSelected = onMonthOfYearSelected,
                                onModeSelected = onModeSelected,
                                onStartDateChanged = onStartDateChanged,
                                onEndDateChanged = onEndDateChanged,
                                onSave = {
                                    onSaveRule()
                                    isFormVisible = false
                                },
                            )
                        }
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(RecurrenceRuleStatus.entries.toTypedArray()) { status ->
                                val label = when (status) {
                                    RecurrenceRuleStatus.ACTIVE -> "Activas"
                                    RecurrenceRuleStatus.PAUSED -> "Pausadas"
                                    RecurrenceRuleStatus.ARCHIVED -> "Archivadas"
                                    RecurrenceRuleStatus.COMPLETED -> "Finalizadas"
                                }
                                FilterChip(
                                    selected = selectedStatusFilter == status,
                                    onClick = { selectedStatusFilter = status },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.Black,
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            }
                        }
                    }

                    val filteredRules = uiState.rules.filter { it.status == selectedStatusFilter }

                    if (filteredRules.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
                            ) {
                                Text(
                                    text = "No hay reglas en este estado.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    } else {
                        items(filteredRules, key = { it.id }) { rule ->
                            RuleCard(
                                rule = rule,
                                accounts = uiState.accountOptions,
                                categories = uiState.categoryOptions,
                                privacyModeEnabled = uiState.privacyModeEnabled,
                                onClick = { selectedRuleDetail = rule },
                                onEdit = {
                                    onEditRule(rule)
                                    isFormVisible = true
                                },
                                onPause = { onPauseRule(rule.id) },
                                onResume = { onResumeRule(rule.id) },
                                onArchive = { onArchiveRule(rule.id) },
                                onRestore = { onRestoreRule(rule.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal / Diálogo de revisión individual
    occurrenceToEdit?.let { occurrence ->
        IndividualOccurrenceReviewDialog(
            occurrence = occurrence,
            onDismiss = { occurrenceToEdit = null },
            onConfirm = { id, editedAmount, editedDate ->
                onConfirmOccurrence(id, editedAmount, editedDate)
                occurrenceToEdit = null
            },
        )
    }

    // Diálogo de posponer ocurrencia
    occurrenceToPostpone?.let { occurrence ->
        PostponeOccurrenceDialog(
            occurrence = occurrence,
            onDismiss = { occurrenceToPostpone = null },
            onPostpone = { id, newReviewAt ->
                onPostponeOccurrence(id, newReviewAt)
                occurrenceToPostpone = null
            },
        )
    }

    // Diálogo de detalle de regla
    selectedRuleDetail?.let { rule ->
        RuleDetailDialog(
            rule = rule,
            accounts = uiState.accountOptions,
            categories = uiState.categoryOptions,
            onDismiss = { selectedRuleDetail = null },
        )
    }
}

@Composable
private fun PendingOccurrencesContent(
    occurrences: List<RecurrenceOccurrenceEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    privacyModeEnabled: Boolean,
    onConfirm: (String) -> Unit,
    onEditBeforeConfirm: (RecurrenceOccurrenceEntity) -> Unit,
    onPostpone: (RecurrenceOccurrenceEntity) -> Unit,
    onConfirmAll: () -> Unit,
    onSkip: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (occurrences.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhiteSurface),
                    border = BorderStroke(1.dp, OutlineBorder),
                ) {
                    Text(
                        text = "No hay ejecuciones recurrentes pendientes de confirmar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        } else {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Ejecuciones por revisar",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = PetroleumBluePrimary,
                    )
                    Button(
                        onClick = onConfirmAll,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PetroleumBluePrimary),
                    ) {
                        Text("Confirmar todas")
                    }
                }
            }

            items(occurrences, key = { it.id }) { occurrence ->
                val payingAccName = accounts.firstOrNull { it.id == occurrence.payingAccountId }?.name ?: "Cuenta"
                val respAccName = accounts.firstOrNull { it.id == occurrence.responsibleAccountId }?.name ?: "Cuenta"
                val catName = categories.firstOrNull { it.id == occurrence.categoryId }?.name ?: "Categoría"

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhiteSurface),
                    border = BorderStroke(1.dp, OutlineBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = occurrence.note ?: catName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PetroleumBluePrimary,
                            )
                            MoneyText(
                                amountMinor = occurrence.amountMinor,
                                isPrivacyModeEnabled = privacyModeEnabled,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = PetroleumBluePrimary,
                            )
                        }

                        val accountSubtitle = if (occurrence.type == TransactionType.EXPENSE) {
                            if (occurrence.payingAccountId != occurrence.responsibleAccountId) {
                                "$catName · $payingAccName → $respAccName (Adelanto)"
                            } else {
                                "$catName · $respAccName"
                            }
                        } else {
                            "$catName · $payingAccName"
                        }

                        Text(text = accountSubtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        val dateLabel = if (occurrence.reviewAt != null) {
                            "Fecha de revisión: ${occurrence.reviewAt.toDateText()} (Programada: ${occurrence.scheduledAt.toDateText()})"
                        } else {
                            "Fecha programada: ${occurrence.scheduledAt.toDateText()}"
                        }
                        Text(text = dateLabel, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)

                        HorizontalDivider(color = Color(0xFFEEEEEE))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            TextButton(onClick = { onSkip(occurrence.id) }) {
                                Text("Omitir", color = Color.Gray)
                            }
                            TextButton(onClick = { onPostpone(occurrence) }) {
                                Text("Posponer", color = PetroleumBluePrimary)
                            }
                            TextButton(onClick = { onEditBeforeConfirm(occurrence) }) {
                                Text("Revisar")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = { onConfirm(occurrence.id) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Confirmar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleCard(
    rule: RecurrenceRuleEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    privacyModeEnabled: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
) {
    val payingAccName = accounts.firstOrNull { it.id == rule.payingAccountId }?.name ?: "Cuenta"
    val respAccName = accounts.firstOrNull { it.id == rule.responsibleAccountId }?.name ?: "Cuenta"
    val catName = categories.firstOrNull { it.id == rule.categoryId }?.name ?: "Categoría"

    val freqLabel = when (rule.frequency) {
        RecurrenceFrequency.WEEKLY -> "Semanal"
        RecurrenceFrequency.MONTHLY -> "Mensual (día ${rule.dayOfMonth ?: 1})"
        RecurrenceFrequency.MULTI_MONTH -> "Cada ${rule.intervalCount ?: 2} meses (día ${rule.dayOfMonth ?: 1})"
        RecurrenceFrequency.YEARLY -> "Anual"
    }

    val modeLabel = if (rule.mode == RecurrenceExecutionMode.AUTOMATIC) "Automático" else "Pendiente de confirmar"

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhiteSurface),
        border = BorderStroke(1.dp, OutlineBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = rule.note ?: catName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PetroleumBluePrimary,
                )
                MoneyText(
                    amountMinor = rule.amountMinor,
                    isPrivacyModeEnabled = privacyModeEnabled,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = PetroleumBluePrimary,
                )
            }

            val subtitle = if (rule.type == TransactionType.EXPENSE) {
                if (rule.payingAccountId != rule.responsibleAccountId) {
                    "$catName · $payingAccName → $respAccName"
                } else {
                    "$catName · $respAccName"
                }
            } else {
                "$catName · $payingAccName"
            }

            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(text = "Frecuencia: $freqLabel · Modo: $modeLabel", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            Text(text = "Próxima ejecución: ${rule.nextOccurrenceAt.toDateText()}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)

            HorizontalDivider(color = Color(0xFFEEEEEE))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar regla", modifier = Modifier.size(18.dp))
                }
                when (rule.status) {
                    RecurrenceRuleStatus.ACTIVE -> {
                        TextButton(onClick = onPause) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pausar")
                        }
                        TextButton(onClick = onArchive) {
                            Text("Archivar", color = Color.Gray)
                        }
                    }
                    RecurrenceRuleStatus.PAUSED -> {
                        TextButton(onClick = onResume) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reactivar")
                        }
                        TextButton(onClick = onArchive) {
                            Text("Archivar", color = Color.Gray)
                        }
                    }
                    RecurrenceRuleStatus.ARCHIVED -> {
                        TextButton(onClick = onRestore) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restaurar")
                        }
                    }
                    RecurrenceRuleStatus.COMPLETED -> {
                        TextButton(onClick = onArchive) {
                            Text("Archivar", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateOrEditRuleCard(
    uiState: RecurrenceUiState,
    onTypeSelected: (TransactionType) -> Unit,
    onPayingAccountSelected: (String) -> Unit,
    onResponsibleAccountSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onAmountChanged: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onFrequencySelected: (RecurrenceFrequency) -> Unit,
    onIntervalCountSelected: (Int) -> Unit,
    onDayOfWeekSelected: (Int) -> Unit,
    onDayOfMonthSelected: (Int) -> Unit,
    onMonthOfYearSelected: (Int) -> Unit,
    onModeSelected: (RecurrenceExecutionMode) -> Unit,
    onStartDateChanged: (String) -> Unit,
    onEndDateChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    val isEditing = uiState.editingRuleId != null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color.Black),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Cabecera del formulario
            Text(
                text = if (isEditing) "Editar recurrencia" else "Nueva recurrencia",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )

            if (isEditing) {
                Surface(
                    color = Color(0xFFFFF3CD),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Aviso: los cambios solo afectan a futuras ocurrencias no generadas. Ocurrencias pendientes y movimientos anteriores no cambian.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF856404),
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }

            // Datos principales
            RowChips(
                title = "Tipo de movimiento",
                items = listOf(
                    ChipItem("Gasto", uiState.selectedType == TransactionType.EXPENSE) { onTypeSelected(TransactionType.EXPENSE) },
                    ChipItem("Ingreso", uiState.selectedType == TransactionType.INCOME) { onTypeSelected(TransactionType.INCOME) },
                ),
            )

            OutlinedTextField(
                value = uiState.amountInput,
                onValueChange = onAmountChanged,
                label = { Text("Importe") },
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            RowChips(
                title = "Categoría",
                items = uiState.categoryOptions.map { cat ->
                    ChipItem(cat.name, uiState.selectedCategoryId == cat.id) {
                        onCategorySelected(cat.id)
                    }
                },
            )

            // Cuentas dinámicas
            if (uiState.selectedType == TransactionType.EXPENSE) {
                RowChips(
                    title = "Pagado desde (Caja real)",
                    items = uiState.accountOptions.map { account ->
                        ChipItem(account.name, uiState.selectedPayingAccountId == account.id) {
                            onPayingAccountSelected(account.id)
                        }
                    },
                )
                RowChips(
                    title = "Gasto de (Análisis)",
                    items = uiState.accountOptions.map { account ->
                        ChipItem(account.name, uiState.selectedResponsibleAccountId == account.id) {
                            onResponsibleAccountSelected(account.id)
                        }
                    },
                )
            } else {
                RowChips(
                    title = "Ingresar en (Cuenta destinataria)",
                    items = uiState.accountOptions.map { account ->
                        ChipItem(account.name, uiState.selectedPayingAccountId == account.id) {
                            onPayingAccountSelected(account.id)
                        }
                    },
                )
            }

            // Programación y frecuencia
            RowChips(
                title = "Periodicidad / Frecuencia",
                items = listOf(
                    ChipItem("Semanal", uiState.frequency == RecurrenceFrequency.WEEKLY) { onFrequencySelected(RecurrenceFrequency.WEEKLY) },
                    ChipItem("Mensual", uiState.frequency == RecurrenceFrequency.MONTHLY) { onFrequencySelected(RecurrenceFrequency.MONTHLY) },
                    ChipItem("Cada varios meses", uiState.frequency == RecurrenceFrequency.MULTI_MONTH) { onFrequencySelected(RecurrenceFrequency.MULTI_MONTH) },
                    ChipItem("Anual", uiState.frequency == RecurrenceFrequency.YEARLY) { onFrequencySelected(RecurrenceFrequency.YEARLY) },
                ),
            )

            if (uiState.frequency == RecurrenceFrequency.WEEKLY) {
                val daysOfWeek = listOf(
                    1 to "Lun", 2 to "Mar", 3 to "Mié", 4 to "Jue", 5 to "Vie", 6 to "Sáb", 7 to "Dom",
                )
                RowChips(
                    title = "Día de la semana",
                    items = daysOfWeek.map { (dayVal, label) ->
                        ChipItem(label, uiState.dayOfWeek == dayVal) { onDayOfWeekSelected(dayVal) }
                    },
                )
            }

            if (uiState.frequency == RecurrenceFrequency.MULTI_MONTH) {
                RowChips(
                    title = "Intervalo de meses",
                    items = listOf(2, 3, 6).map { months ->
                        ChipItem("Cada $months meses", uiState.intervalCount == months) { onIntervalCountSelected(months) }
                    },
                )
            }

            if (uiState.frequency == RecurrenceFrequency.MONTHLY || uiState.frequency == RecurrenceFrequency.MULTI_MONTH) {
                OutlinedTextField(
                    value = uiState.dayOfMonth.toString(),
                    onValueChange = { str -> str.toIntOrNull()?.let { onDayOfMonthSelected(it.coerceIn(1, 31)) } },
                    label = { Text("Día del mes (1-31)") },
                    supportingText = { Text("En meses con menos días (ej. 28, 29 o 30), se ejecutará el último día real del mes.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (uiState.frequency == RecurrenceFrequency.YEARLY) {
                val months = listOf(
                    1 to "Ene", 2 to "Feb", 3 to "Mar", 4 to "Abr", 5 to "May", 6 to "Jun",
                    7 to "Jul", 8 to "Ago", 9 to "Sep", 10 to "Oct", 11 to "Nov", 12 to "Dic",
                )
                RowChips(
                    title = "Mes del año",
                    items = months.map { (mVal, mLabel) ->
                        ChipItem(mLabel, uiState.monthOfYear == mVal) { onMonthOfYearSelected(mVal) }
                    },
                )
                OutlinedTextField(
                    value = uiState.dayOfMonth.toString(),
                    onValueChange = { str -> str.toIntOrNull()?.let { onDayOfMonthSelected(it.coerceIn(1, 31)) } },
                    label = { Text("Día del mes (1-31)") },
                    supportingText = { Text("Para el 29 de febrero en años no bisiestos, se ejecutará el 28 de febrero.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Previsualización dinámica
            uiState.nextExecutionPreviewAt?.let { nextAt ->
                Surface(
                    color = Color(0xFFF3F3F3),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Próxima ejecución: ${nextAt.toDateText()}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        )
                    }
                }
            }

            // Modo de ejecución
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Modo de ejecución", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                RowChips(
                    title = "",
                    items = listOf(
                        ChipItem("Automático", uiState.mode == RecurrenceExecutionMode.AUTOMATIC) { onModeSelected(RecurrenceExecutionMode.AUTOMATIC) },
                        ChipItem("Pendiente de confirmar", uiState.mode == RecurrenceExecutionMode.PENDING) { onModeSelected(RecurrenceExecutionMode.PENDING) },
                    ),
                )
                Text(
                    text = if (uiState.mode == RecurrenceExecutionMode.AUTOMATIC) {
                        "Automático: El movimiento se registra directamente al llegar la fecha sin requerir acción."
                    } else {
                        "Pendiente: Crea una ejecución en la bandeja para que la revises y confirmes antes de afectar la caja."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }

            DateField(
                label = "Fecha inicio (obligatoria)",
                value = uiState.startDateInput,
                errorText = uiState.startDateError,
                onDateSelected = onStartDateChanged,
            )
            OptionalDateField(
                label = "Fecha fin (opcional)",
                value = uiState.endDateInput,
                errorText = uiState.endDateError,
                onDateSelected = onEndDateChanged,
                onClear = { onEndDateChanged("") },
            )

            OutlinedTextField(
                value = uiState.noteInput,
                onValueChange = onNoteChanged,
                label = { Text("Nota / Concepto opcional") },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSave,
                enabled = !uiState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            ) {
                Text(if (uiState.isSaving) "Guardando..." else if (isEditing) "Guardar cambios" else "Guardar regla recurrente")
            }
        }
    }
}

@Composable
private fun IndividualOccurrenceReviewDialog(
    occurrence: RecurrenceOccurrenceEntity,
    onDismiss: () -> Unit,
    onConfirm: (id: String, editedAmount: Long?, editedDate: Long?) -> Unit,
) {
    var amountInput by remember { mutableStateOf((occurrence.amountMinor / 100.0).toString().replace('.', ',')) }
    var dateInput by remember { mutableStateOf(occurrence.scheduledAt.toIsoDateString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Revisar ejecución") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Puedes ajustar el importe o la fecha real antes de confirmar el movimiento en caja.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Importe") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                DateField(
                    label = "Fecha de ejecución",
                    value = dateInput,
                    onDateSelected = { dateInput = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountMinor = amountInput.toMinorAmountOrNull()
                    val scheduledAt = dateInput.toEpochMillisAtStartOfDayOrNull()
                    onConfirm(occurrence.id, amountMinor, scheduledAt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
            ) {
                Text("Confirmar y registrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun RuleDetailDialog(
    rule: RecurrenceRuleEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
) {
    val payingAccName = accounts.firstOrNull { it.id == rule.payingAccountId }?.name ?: "Cuenta"
    val respAccName = accounts.firstOrNull { it.id == rule.responsibleAccountId }?.name ?: "Cuenta"
    val catName = categories.firstOrNull { it.id == rule.categoryId }?.name ?: "Categoría"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(rule.note ?: catName) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Importe: ${rule.amountMinor.toCurrencyText()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("Tipo: ${if (rule.type == TransactionType.EXPENSE) "Gasto" else "Ingreso"}")
                Text("Pagado desde: $payingAccName")
                if (rule.type == TransactionType.EXPENSE) {
                    Text("Gasto de: $respAccName")
                }
                Text("Categoría: $catName")
                Text("Frecuencia: ${rule.frequency.name}")
                Text("Modo: ${if (rule.mode == RecurrenceExecutionMode.AUTOMATIC) "Automático" else "Pendiente de confirmar"}")
                Text("Estado: ${rule.status.name}")
                Text("Inicio: ${rule.startAt.toDateText()}")
                Text("Fin: ${rule.endAt?.toDateText() ?: "Sin fin"}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
    )
}

private data class ChipItem(
    val label: String,
    val selected: Boolean,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
private fun RowChips(title: String, items: List<ChipItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title.isNotBlank()) {
            Text(text = title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(items) { item ->
                FilterChip(
                    selected = item.selected,
                    enabled = item.enabled,
                    onClick = item.onClick,
                    label = { Text(item.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.Black,
                        selectedLabelColor = Color.White,
                    ),
                )
            }
        }
    }
}

@Composable
private fun DateField(
    label: String,
    value: String,
    errorText: String? = null,
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
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        shape = RoundedCornerShape(10.dp),
    )
}

@Composable
private fun OptionalDateField(
    label: String,
    value: String,
    errorText: String? = null,
    onDateSelected: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DateField(label = label, value = value, errorText = errorText, onDateSelected = onDateSelected)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onClear) {
                Text(if (value.isBlank()) "Sin fecha fin" else "Quitar fecha fin", style = MaterialTheme.typography.bodySmall)
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

private fun Long.toDateText(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()
    .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "ES")))

@Composable
private fun PostponeOccurrenceDialog(
    occurrence: RecurrenceOccurrenceEntity,
    onDismiss: () -> Unit,
    onPostpone: (id: String, newReviewAt: Long) -> Unit,
) {
    val zoneId = ZoneId.systemDefault()
    val today = LocalDate.now(zoneId)
    val defaultPostponedDate = today.plusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)
    var dateInput by remember { mutableStateOf(defaultPostponedDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Posponer recordatorio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Selecciona la nueva fecha para revisar este movimiento. La fecha económica programada (${occurrence.scheduledAt.toDateText()}) no se modificará.",
                    style = MaterialTheme.typography.bodySmall,
                )
                DateField(
                    label = "Fecha de nueva revisión",
                    value = dateInput,
                    onDateSelected = { dateInput = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newReviewAt = dateInput.toEpochMillisAtStartOfDayOrNull() ?: System.currentTimeMillis()
                    onPostpone(occurrence.id, newReviewAt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PetroleumBluePrimary),
            ) {
                Text("Posponer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

private fun Long.toIsoDateString(zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)

private fun String.toLocalDateOrNull(): LocalDate? = runCatching {
    LocalDate.parse(this, DateTimeFormatter.ISO_LOCAL_DATE)
}.getOrNull()

private fun String.toMinorAmountOrNull(): Long? {
    val normalized = replace(',', '.').trim()
    if (normalized.isBlank()) return null
    val decimal = normalized.toBigDecimalOrNull() ?: return null
    return decimal.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
}

private fun String.toEpochMillisAtStartOfDayOrNull(zoneId: ZoneId = ZoneId.systemDefault()): Long? =
    try {
        LocalDate.parse(trim(), DateTimeFormatter.ISO_LOCAL_DATE)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    } catch (_: java.time.format.DateTimeParseException) {
        null
    }
