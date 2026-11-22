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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.ui.AVAILABLE_CATEGORY_ICONS
import com.familyexpenses.app.core.ui.resolveAppIcon

@Composable
fun AccountsSettingsRoute(
    onBack: () -> Unit,
    viewModel: AccountsSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AccountsSettingsScreen(
        uiState = uiState,
        onBack = onBack,
        onEditAccount = viewModel::onEditAccount,
        onNameChanged = viewModel::onNameChanged,
        onIconSelected = viewModel::onIconSelected,
        onDismissEdit = viewModel::onDismissEdit,
        onSaveEdit = viewModel::onSaveEdit,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountsSettingsScreen(
    uiState: AccountsSettingsUiState,
    onBack: () -> Unit,
    onEditAccount: (EditableAccountItem) -> Unit,
    onNameChanged: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onDismissEdit: () -> Unit,
    onSaveEdit: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cuentas",
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
            item {
                Text(
                    text = "En V6 se gestionan las cuentas Familia y Personal. Puedes personalizar su nombre e icono; " +
                        "el saldo inicial y los ajustes de caja se gestionan dentro del detalle de cada cuenta.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                )
            }

            items(uiState.accounts, key = { it.account.id }) { item ->
                val isFamily = item.account.type == AccountType.FAMILY
                Card(
                    onClick = { onEditAccount(item) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 68.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFamily) Color.Black else Color(0xFFF5F5F5),
                    ),
                    border = if (isFamily) null else BorderStroke(1.dp, Color(0xFFE0E0E0)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = resolveAppIcon(item.iconKey),
                            contentDescription = null,
                            tint = if (isFamily) Color.White else Color.Black,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.account.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isFamily) Color.White else Color.Black,
                            )
                            Text(
                                text = if (isFamily) "Cuenta compartida (${item.account.currencyCode})" else "Cuenta personal (${item.account.currencyCode})",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isFamily) Color(0xFFCCCCCC) else Color.Gray,
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar cuenta",
                            tint = if (isFamily) Color.White else Color.Black,
                        )
                    }
                }
            }
        }
    }

    if (uiState.editingAccount != null) {
        AlertDialog(
            onDismissRequest = onDismissEdit,
            title = { Text("Editar cuenta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = uiState.editNameInput,
                        onValueChange = onNameChanged,
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Icono",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AVAILABLE_CATEGORY_ICONS, key = { it.key }) { option ->
                            val selected = option.key == uiState.editIconInput
                            Surface(
                                onClick = { onIconSelected(option.key) },
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Color.Black else Color(0xFFF5F5F5),
                                border = BorderStroke(1.dp, if (selected) Color.Black else Color(0xFFE0E0E0)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = resolveAppIcon(option.key),
                                        contentDescription = option.label,
                                        tint = if (selected) Color.White else Color.Black,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onSaveEdit) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissEdit) {
                    Text("Cancelar")
                }
            },
        )
    }
}
