package com.familyexpenses.app.feature.expense

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import com.familyexpenses.app.core.ui.resolveAppIcon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AddExpenseRoute(
    onNavigateBack: () -> Unit,
    onExpenseSaved: (transactionId: String) -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler {
        viewModel.onBackClicked()
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is AddExpenseEvent.Saved -> onExpenseSaved(event.transactionId)
                is AddExpenseEvent.NavigateBack -> onNavigateBack()
            }
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    AddExpenseScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onDigit = viewModel::onDigit,
        onComma = viewModel::onComma,
        onClear = viewModel::onClear,
        onBackspace = viewModel::onBackspace,
        onCategorySelected = viewModel::onCategorySelected,
        onPayingAccountSelected = viewModel::onPayingAccountSelected,
        onResponsibleAccountSelected = viewModel::onResponsibleAccountSelected,
        onNoteChanged = viewModel::onNoteChanged,
        onDateSelected = viewModel::onDateSelected,
        onToggleDetails = viewModel::onToggleDetails,
        onOpenMoreCategories = viewModel::onOpenMoreCategories,
        onDismissMoreCategories = viewModel::onDismissMoreCategories,
        onCreateQuickCategory = viewModel::onCreateQuickExpenseCategory,
        onBackClicked = viewModel::onBackClicked,
        onConfirmDiscard = viewModel::onConfirmDiscard,
        onDismissDiscardDialog = viewModel::onDismissDiscardDialog,
        onSave = viewModel::saveExpense,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddExpenseScreen(
    uiState: AddExpenseUiState,
    snackbarHostState: SnackbarHostState,
    onDigit: (Char) -> Unit,
    onComma: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onCategorySelected: (String) -> Unit,
    onPayingAccountSelected: (String) -> Unit,
    onResponsibleAccountSelected: (String) -> Unit,
    onNoteChanged: (String) -> Unit,
    onDateSelected: (Long) -> Unit,
    onToggleDetails: () -> Unit,
    onOpenMoreCategories: () -> Unit,
    onDismissMoreCategories: () -> Unit,
    onCreateQuickCategory: (String) -> Unit = {},
    onBackClicked: () -> Unit,
    onConfirmDiscard: () -> Unit,
    onDismissDiscardDialog: () -> Unit,
    onSave: () -> Unit,
) {
    val scrollState = rememberScrollState()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Nuevo gasto",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClicked) {
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 2.dp,
                shadowElevation = 4.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = onSave,
                        enabled = uiState.isSaveEnabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.DarkGray,
                        ),
                    ) {
                        Text(
                            text = if (uiState.isSaving) "Guardando..." else "Guardar gasto",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 1. Display Destacado de Importe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = uiState.displayAmount,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // 2. Cuadrícula 2x4 de Categorías Favoritas + "Más"
            Text(
                text = "Categoría",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = Color.Gray,
            )
            FavoriteCategoriesGrid2x4(
                favorites = uiState.favoriteCategories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategoryClick = onCategorySelected,
                onMoreClick = onOpenMoreCategories,
            )

            // 3. Dos selectores SEPARADOS: «Pagado desde» y «Gasto de»
            DualAccountSelectors(
                accounts = uiState.accounts,
                payingAccountId = uiState.payingAccountId,
                responsibleAccountId = uiState.responsibleAccountId,
                onPayingSelected = onPayingAccountSelected,
                onResponsibleSelected = onResponsibleAccountSelected,
            )

            // Aviso de adelanto si difieren
            if (uiState.advanceNotice != null) {
                Surface(
                    color = Color(0xFFF5F5F5),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "ℹ️ ${uiState.advanceNotice}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            // 4. Teclado Numérico Propio (coma, C, retroceso, 2 decimales)
            NumericKeypad(
                onDigit = onDigit,
                onComma = onComma,
                onClear = onClear,
                onBackspace = onBackspace,
            )

            // 5. Detalles Plegados ("Añadir detalles")
            FoldedDetailsSection(
                isExpanded = uiState.isDetailsExpanded,
                note = uiState.noteInput,
                occurredAt = uiState.occurredAt,
                onToggle = onToggleDetails,
                onNoteChanged = onNoteChanged,
                onOpenDatePicker = { showDatePicker = true },
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Diálogo de confirmación al descartar
    if (uiState.showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDismissDiscardDialog,
            title = { Text("¿Descartar gasto?") },
            text = { Text("Se perderán los datos introducidos.") },
            confirmButton = {
                TextButton(onClick = onConfirmDiscard) {
                    Text("Descartar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDiscardDialog) {
                    Text("Continuar editando")
                }
            },
        )
    }

    // Selector de Fecha
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.occurredAt)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                        showDatePicker = false
                    },
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Bottom Sheet: Más categorías
    if (uiState.isMoreCategoriesSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissMoreCategories,
            sheetState = sheetState,
        ) {
            MoreCategoriesSheetContent(
                categories = uiState.allExpenseCategories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelected = onCategorySelected,
                onCreateCategory = onCreateQuickCategory,
            )
        }
    }
}

/**
 * Cuadrícula 2x4 con 7 favoritas manuales + 1 celda de «Más».
 */
@Composable
private fun FavoriteCategoriesGrid2x4(
    favorites: List<CategoryEntity>,
    selectedCategoryId: String,
    onCategoryClick: (String) -> Unit,
    onMoreClick: () -> Unit,
) {
    val itemsToShow = favorites.take(7)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Fila 1 (4 elementos)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (i in 0 until 4) {
                if (i < itemsToShow.size) {
                    val category = itemsToShow[i]
                    CategoryCell(
                        category = category,
                        isSelected = category.id == selectedCategoryId,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(category.id) },
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        // Fila 2 (3 favoritos + «Más»)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (i in 4 until 7) {
                if (i < itemsToShow.size) {
                    val category = itemsToShow[i]
                    CategoryCell(
                        category = category,
                        isSelected = category.id == selectedCategoryId,
                        modifier = Modifier.weight(1f),
                        onClick = { onCategoryClick(category.id) },
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            // Celda 8: «Más»
            MoreCategoryCell(
                modifier = Modifier.weight(1f),
                onClick = onMoreClick,
            )
        }
    }
}

@Composable
private fun CategoryCell(
    category: CategoryEntity,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) Color.Black else Color(0xFFF5F5F5)
    val contentColor = if (isSelected) Color.White else Color.Black
    val borderColor = if (isSelected) Color.Black else Color(0xFFE0E0E0)

    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = resolveCategoryIcon(category.icon),
                contentDescription = category.name,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = contentColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MoreCategoryCell(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFEEEEEE),
        border = BorderStroke(1.dp, Color(0xFFDDDDDD)),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Más categorías",
                tint = Color.DarkGray,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Más",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color.DarkGray,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

/**
 * Dos selectores SEPARADOS: «Pagado desde» y «Gasto de».
 */
@Composable
private fun DualAccountSelectors(
    accounts: List<AccountEntity>,
    payingAccountId: String,
    responsibleAccountId: String,
    onPayingSelected: (String) -> Unit,
    onResponsibleSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Selector 1: Pagado desde
        AccountSelectorRow(
            label = "Pagado desde",
            accounts = accounts,
            selectedAccountId = payingAccountId,
            onAccountSelected = onPayingSelected,
        )
        // Selector 2: Gasto de
        AccountSelectorRow(
            label = "Gasto de",
            accounts = accounts,
            selectedAccountId = responsibleAccountId,
            onAccountSelected = onResponsibleSelected,
        )
    }
}

@Composable
private fun AccountSelectorRow(
    label: String,
    accounts: List<AccountEntity>,
    selectedAccountId: String,
    onAccountSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.DarkGray,
            modifier = Modifier.width(105.dp),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            accounts.forEach { (accountId, name) ->
                val isSelected = accountId == selectedAccountId
                Surface(
                    onClick = { onAccountSelected(accountId) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color.Black else Color(0xFFF5F5F5),
                    border = BorderStroke(1.dp, if (isSelected) Color.Black else Color(0xFFE0E0E0)),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isSelected) Color.White else Color.Black,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Teclado Numérico Propio con coma decimal, 2 decimales, C y retroceso.
 */
@Composable
internal fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onComma: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Fila 1: 1, 2, 3
        KeypadRow {
            KeypadButton(text = "1", modifier = Modifier.weight(1f), onClick = { onDigit('1') })
            KeypadButton(text = "2", modifier = Modifier.weight(1f), onClick = { onDigit('2') })
            KeypadButton(text = "3", modifier = Modifier.weight(1f), onClick = { onDigit('3') })
        }
        // Fila 2: 4, 5, 6
        KeypadRow {
            KeypadButton(text = "4", modifier = Modifier.weight(1f), onClick = { onDigit('4') })
            KeypadButton(text = "5", modifier = Modifier.weight(1f), onClick = { onDigit('5') })
            KeypadButton(text = "6", modifier = Modifier.weight(1f), onClick = { onDigit('6') })
        }
        // Fila 3: 7, 8, 9
        KeypadRow {
            KeypadButton(text = "7", modifier = Modifier.weight(1f), onClick = { onDigit('7') })
            KeypadButton(text = "8", modifier = Modifier.weight(1f), onClick = { onDigit('8') })
            KeypadButton(text = "9", modifier = Modifier.weight(1f), onClick = { onDigit('9') })
        }
        // Fila 4: C, 0, ,, ⌫
        KeypadRow {
            KeypadButton(text = "C", modifier = Modifier.weight(1f), onClick = onClear, isAction = true)
            KeypadButton(text = "0", modifier = Modifier.weight(1f), onClick = { onDigit('0') })
            KeypadButton(text = ",", modifier = Modifier.weight(1f), onClick = onComma, isAction = true)
            KeypadIconButton(
                icon = Icons.AutoMirrored.Filled.Backspace,
                modifier = Modifier.weight(1f),
                onClick = onBackspace,
            )
        }
    }
}

@Composable
private fun KeypadRow(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        content()
    }
}

@Composable
private fun KeypadButton(
    text: String,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isAction) Color(0xFFEBEBEB) else Color(0xFFF7F7F7),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = if (isAction) FontWeight.Bold else FontWeight.SemiBold,
                ),
                color = Color.Black,
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFEBEBEB),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Retroceso",
                tint = Color.Black,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/**
 * Sección plegada "Añadir detalles" (Nota y Fecha).
 */
@Composable
internal fun FoldedDetailsSection(
    isExpanded: Boolean,
    note: String,
    occurredAt: Long,
    onToggle: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onOpenDatePicker: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (isExpanded) "Ocultar detalles" else "Añadir detalles (nota, fecha)",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Color.DarkGray,
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.DarkGray,
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChanged,
                    placeholder = { Text("Nota o descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                )

                val formattedDate = remember(occurredAt) {
                    val sdf = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
                    sdf.format(Date(occurredAt)).replaceFirstChar { it.uppercase() }
                }

                OutlinedButton(
                    onClick = onOpenDatePicker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Cambiar fecha",
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = formattedDate)
                }
            }
        }
    }
}

/**
 * Contenido del BottomSheet para seleccionar entre todas las categorías de gasto.
 */
@Composable
private fun MoreCategoriesSheetContent(
    categories: List<CategoryEntity>,
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    onCreateCategory: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Todas las categorías de gasto",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
            TextButton(
                onClick = {
                    newCategoryName = searchQuery.trim()
                    showCreateDialog = true
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Crear")
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar categoría...") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(filteredCategories) { category ->
                val isSelected = category.id == selectedCategoryId
                Card(
                    onClick = { onCategorySelected(category.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color.Black else Color(0xFFF9F9F9),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = resolveCategoryIcon(category.icon),
                            contentDescription = category.name,
                            tint = if (isSelected) Color.White else Color.Black,
                            modifier = Modifier.size(22.dp),
                        )
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (isSelected) Color.White else Color.Black,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nueva categoría de gasto") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreateCategory(newCategoryName)
                        showCreateDialog = false
                    },
                    enabled = newCategoryName.isNotBlank(),
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

/**
 * Mapeo de nombres de iconos semánticos a vectores Material.
 */
internal fun resolveCategoryIcon(iconName: String): ImageVector = resolveAppIcon(iconName)
