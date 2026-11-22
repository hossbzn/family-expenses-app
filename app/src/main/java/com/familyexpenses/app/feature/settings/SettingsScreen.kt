package com.familyexpenses.app.feature.settings

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onCategoriesClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onRecurrenceClick: () -> Unit,
    onDataSecurityClick: () -> Unit,
) {
    SettingsScreen(
        onBack = onBack,
        onCategoriesClick = onCategoriesClick,
        onAccountsClick = onAccountsClick,
        onRecurrenceClick = onRecurrenceClick,
        onDataSecurityClick = onDataSecurityClick,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    onBack: () -> Unit,
    onCategoriesClick: () -> Unit,
    onAccountsClick: () -> Unit,
    onRecurrenceClick: () -> Unit,
    onDataSecurityClick: () -> Unit,
) {
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ajustes",
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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SettingsRowCard(
                    icon = Icons.Default.Category,
                    title = "Categorías",
                    subtitle = "7 favoritas de gasto, orden manual, iconos y archivado",
                    onClick = onCategoriesClick,
                )
            }
            item {
                SettingsRowCard(
                    icon = Icons.Default.AccountBalanceWallet,
                    title = "Cuentas",
                    subtitle = "Configurar nombre e icono de Familia y Personal",
                    onClick = onAccountsClick,
                )
            }
            item {
                SettingsRowCard(
                    icon = Icons.Default.Repeat,
                    title = "Movimientos recurrentes",
                    subtitle = "Reglas mensuales automáticas de gastos e ingresos",
                    onClick = onRecurrenceClick,
                )
            }
            item {
                SettingsRowCard(
                    icon = Icons.Default.Storage,
                    title = "Datos y seguridad",
                    subtitle = "Exportación CSV, copia de seguridad portable y restauración",
                    onClick = onDataSecurityClick,
                )
            }
            item {
                SettingsRowCard(
                    icon = Icons.Default.Info,
                    title = "Acerca de",
                    subtitle = "Family Expenses V6 · 100 % local y sin conexión",
                    onClick = { showAboutDialog = true },
                )
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Family Expenses V6") },
            text = {
                Text(
                    "Versión 6.0 (offline-first). Todos los movimientos, saldos reales de caja, " +
                        "adelantos bidireccionales y copias de seguridad se almacenan únicamente en este dispositivo, " +
                        "sin nube, sin cuentas externas ni sincronización automática.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Cerrar")
                }
            },
        )
    }
}

@Composable
private fun SettingsRowCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
        border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
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
                color = Color.Black,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
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

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.DarkGray,
            )
        }
    }
}
