package com.familyexpenses.app.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.TableView
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

@Composable
fun DataSecurityRoute(
    onBack: () -> Unit,
    viewModel: DataSecurityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pendingWriteContent by remember { mutableStateOf<String?>(null) }
    var pendingWriteSuccessMessage by remember { mutableStateOf("Archivo exportado.") }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        val content = pendingWriteContent ?: return@rememberLauncherForActivityResult
        pendingWriteContent = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.toByteArray(StandardCharsets.UTF_8))
                    } ?: error("No se pudo abrir el archivo de destino.")
                }
            }
            result.onSuccess {
                viewModel.onFileWriteResult(true, pendingWriteSuccessMessage)
            }.onFailure { err ->
                viewModel.onFileWriteResult(false, err.message ?: "Error al guardar el CSV.")
            }
        }
    }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        val content = pendingWriteContent ?: return@rememberLauncherForActivityResult
        pendingWriteContent = null
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(content.toByteArray(StandardCharsets.UTF_8))
                    } ?: error("No se pudo abrir el archivo de destino.")
                }
            }
            result.onSuccess {
                viewModel.onFileWriteResult(true, pendingWriteSuccessMessage)
            }.onFailure { err ->
                viewModel.onFileWriteResult(false, err.message ?: "Error al guardar la copia.")
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        stream.bufferedReader(StandardCharsets.UTF_8).readText()
                    } ?: error("No se pudo leer el archivo seleccionado.")
                }
            }
            result.onSuccess { text ->
                viewModel.onBackupContentLoaded(text)
            }.onFailure { err ->
                viewModel.onFileWriteResult(false, err.message ?: "Error al leer la copia.")
            }
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is DataSecurityEvent.LaunchCreateCsv -> {
                    pendingWriteContent = event.content
                    pendingWriteSuccessMessage = "CSV de movimientos exportado correctamente."
                    saveCsvLauncher.launch(event.suggestedFileName)
                }
                is DataSecurityEvent.LaunchCreateBackup -> {
                    pendingWriteContent = event.content
                    pendingWriteSuccessMessage = "Copia de seguridad guardada correctamente."
                    saveBackupLauncher.launch(event.suggestedFileName)
                }
            }
        }
    }

    DataSecurityScreen(
        uiState = uiState,
        onBack = onBack,
        onExportCsvClick = viewModel::onExportCsvClick,
        onExportBackupClick = viewModel::onExportBackupClick,
        onImportBackupClick = {
            openBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
        },
        onConfirmRestore = viewModel::onConfirmRestore,
        onDismissRestoreConfirmation = viewModel::onDismissRestoreConfirmation,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun DataSecurityScreen(
    uiState: DataSecurityUiState,
    onBack: () -> Unit,
    onExportCsvClick: () -> Unit,
    onExportBackupClick: () -> Unit,
    onImportBackupClick: () -> Unit,
    onConfirmRestore: () -> Unit,
    onDismissRestoreConfirmation: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Datos y seguridad",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Aviso explícito de modelo 100 % local y offline
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Almacenamiento 100 % local (sin nube)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                            )
                            Text(
                                text = "Todos tus datos permanecen únicamente en este dispositivo, sin conexión ni PIN adicional. " +
                                    "Si desinstalas la app o cambias de teléfono, perderás los datos si no exportas una copia externa periódicamente.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6D6D6),
                            )
                        }
                    }
                }
            }

            // Resumen actual de registros locales
            uiState.currentSummary?.let { summary ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Estado actual en el dispositivo",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.Black,
                            )
                            Text(
                                text = "${summary.accountsCount} cuentas · ${summary.categoriesCount} categorías · " +
                                    "${summary.transactionsCount} gastos/ingresos · ${summary.transfersCount} transferencias/devoluciones · " +
                                    "${summary.adjustmentsCount} ajustes · ${summary.recurrenceRulesCount} recurrentes",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.DarkGray,
                            )
                        }
                    }
                }
            }

            if (uiState.statusMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isError) Color(0xFFFFEBEE) else Color(0xFFF0F0F0),
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (uiState.isError) MaterialTheme.colorScheme.error else Color.Black,
                        ),
                    ) {
                        Text(
                            text = uiState.statusMessage,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (uiState.isError) MaterialTheme.colorScheme.error else Color.Black,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }

            item {
                DataActionCard(
                    icon = Icons.Default.TableView,
                    title = "Exportar a CSV",
                    subtitle = "Exporta gastos, ingresos, transferencias, devoluciones y ajustes en formato tabular validado.",
                    enabled = !uiState.isBusy,
                    onClick = onExportCsvClick,
                )
            }

            item {
                DataActionCard(
                    icon = Icons.Default.Download,
                    title = "Exportar copia de seguridad (.json)",
                    subtitle = "Copia portable completa con cuentas, categorías, movimientos, adelantos, ajustes y preferencias.",
                    enabled = !uiState.isBusy,
                    onClick = onExportBackupClick,
                )
            }

            item {
                DataActionCard(
                    icon = Icons.Default.Restore,
                    title = "Restaurar copia de seguridad",
                    subtitle = "Valida una copia JSON portable y reemplaza por completo los datos actuales tras confirmación.",
                    enabled = !uiState.isBusy,
                    onClick = onImportBackupClick,
                )
            }
        }
    }

    val pending = uiState.pendingRestore
    if (pending != null) {
        val p = pending.preview
        AlertDialog(
            onDismissRequest = onDismissRestoreConfirmation,
            title = { Text("Confirmar restauración completa") },
            text = {
                Text(
                    "La copia es válida (esquema V${p.schemaVersion}):\n" +
                        "• ${p.accountsCount} cuentas\n" +
                        "• ${p.categoriesCount} categorías\n" +
                        "• ${p.transactionsCount} gastos e ingresos\n" +
                        "• ${p.transfersCount} transferencias y devoluciones\n" +
                        "• ${p.adjustmentsCount} ajustes de saldo\n" +
                        "• ${p.recurrenceRulesCount} reglas recurrentes\n\n" +
                        "Al restaurar se reemplazarán por completo todos los datos actuales de este dispositivo.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmRestore,
                    enabled = !uiState.isBusy,
                ) {
                    Text(
                        text = if (uiState.isBusy) "Restaurando..." else "Reemplazar y restaurar",
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissRestoreConfirmation,
                    enabled = !uiState.isBusy,
                ) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun DataActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF3F3F3),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.Black,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }
        }
    }
}
