package com.familyexpenses.app.feature.history

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.core.ui.MoneyText
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.domain.model.MovementKind
import com.familyexpenses.app.domain.usecase.MovementAccountFilter
import com.familyexpenses.app.domain.usecase.MovementPeriodFilter
import com.familyexpenses.app.domain.usecase.MovementTypeFilter
import com.familyexpenses.app.feature.expense.resolveCategoryIcon
import com.familyexpenses.app.ui.theme.OffWhiteBackground
import com.familyexpenses.app.ui.theme.OutlineBorder
import com.familyexpenses.app.ui.theme.PetroleumBlueContainer
import com.familyexpenses.app.ui.theme.PetroleumBluePrimary
import com.familyexpenses.app.ui.theme.PureWhiteSurface

@Composable
fun HistoryRoute(
    onBack: () -> Unit,
    onHomeClick: () -> Unit = onBack,
    onAnalysisClick: () -> Unit = {},
    onMovementClick: (kind: MovementKind, id: String) -> Unit = { _, _ -> },
    showBottomBar: Boolean = true,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HistoryScreen(
        uiState = uiState,
        showBottomBar = showBottomBar,
        onBack = onBack,
        onHomeClick = onHomeClick,
        onAnalysisClick = onAnalysisClick,
        onMovementClick = onMovementClick,
        onAccountFilterSelected = viewModel::onAccountFilterSelected,
        onTypeFilterSelected = viewModel::onTypeFilterSelected,
        onCategorySelected = viewModel::onCategorySelected,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onToggleSearch = viewModel::onToggleSearch,
        onOpenCategorySheet = viewModel::onOpenCategorySheet,
        onDismissCategorySheet = viewModel::onDismissCategorySheet,
        onSelectMonthPeriod = viewModel::onSelectMonthPeriod,
        onSelectLast12MonthsPeriod = viewModel::onSelectLast12MonthsPeriod,
        onSelectYearPeriod = viewModel::onSelectYearPeriod,
        onOpenCustomPeriod = viewModel::onOpenCustomPeriod,
        onDismissCustomPeriod = viewModel::onDismissCustomPeriod,
        onCustomPeriodSelected = viewModel::onCustomPeriodSelected,
        onPreviousPeriod = viewModel::onPreviousPeriod,
        onNextPeriod = viewModel::onNextPeriod,
        onResetFilters = viewModel::onResetFilters,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HistoryScreen(
    uiState: HistoryUiState,
    showBottomBar: Boolean = true,
    onBack: () -> Unit,
    onHomeClick: () -> Unit,
    onAnalysisClick: () -> Unit,
    onMovementClick: (kind: MovementKind, id: String) -> Unit,
    onAccountFilterSelected: (MovementAccountFilter) -> Unit,
    onTypeFilterSelected: (MovementTypeFilter) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onOpenCategorySheet: () -> Unit,
    onDismissCategorySheet: () -> Unit,
    onSelectMonthPeriod: () -> Unit,
    onSelectLast12MonthsPeriod: () -> Unit,
    onSelectYearPeriod: () -> Unit,
    onOpenCustomPeriod: () -> Unit,
    onDismissCustomPeriod: () -> Unit,
    onCustomPeriodSelected: (Long, Long) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onResetFilters: () -> Unit,
) {
    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Movimientos",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                navigationIcon = {
                    if (!showBottomBar) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSearch) {
                        Icon(
                            imageVector = if (uiState.isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Buscar movimientos",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 4.dp,
                ) {
                    NavigationBarItem(
                        selected = false,
                        onClick = onHomeClick,
                        icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                        label = { Text("Inicio") },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                        ),
                    )
                    NavigationBarItem(
                        selected = true,
                        onClick = { /* Ya en Movimientos */ },
                        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Movimientos") },
                        label = { Text("Movimientos") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color.Black,
                            indicatorColor = Color.Black,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                        ),
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = onAnalysisClick,
                        icon = { Icon(Icons.Default.BarChart, contentDescription = "Análisis") },
                        label = { Text("Análisis") },
                        colors = NavigationBarItemDefaults.colors(
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Barra de búsqueda plegable
            AnimatedVisibility(visible = uiState.isSearchExpanded) {
                OutlinedTextField(
                    value = uiState.criteria.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Buscar por nota, categoría o cuenta...") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    trailingIcon = {
                        if (uiState.criteria.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            // Filtro principal de cuenta: Todas / Familia / Personal
            AccountSegmentedFilter(
                accounts = uiState.accounts,
                selectedFilter = uiState.criteria.accountFilter,
                onFilterSelected = onAccountFilterSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )

            // Selector de periodo (Mes, 12 meses, Año, Personalizado) + navegación < >
            PeriodFilterSection(
                periodFilter = uiState.criteria.periodFilter,
                periodLabel = uiState.periodLabel,
                onSelectMonth = onSelectMonthPeriod,
                onSelectLast12Months = onSelectLast12MonthsPeriod,
                onSelectYear = onSelectYearPeriod,
                onOpenCustom = onOpenCustomPeriod,
                onPrevious = onPreviousPeriod,
                onNext = onNextPeriod,
            )

            // Filtros secundarios: Tipo y Categoría
            SecondaryFiltersRow(
                selectedType = uiState.criteria.typeFilter,
                selectedCategoryName = uiState.selectedCategoryName,
                onTypeSelected = onTypeFilterSelected,
                onOpenCategorySheet = onOpenCategorySheet,
            )

            HorizontalDivider(color = Color(0xFFEEEEEE))

            // Lista compacta de dos líneas agrupada por día descendente (SIN resumen mensual)
            if (uiState.dayGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "No hay movimientos en este periodo",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.DarkGray,
                        )
                        TextButton(onClick = onResetFilters) {
                            Text("Restablecer filtros")
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                ) {
                    uiState.dayGroups.forEach { group ->
                        item(key = "header_${group.date}") {
                            Text(
                                text = group.headerLabel,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.Gray,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            )
                        }

                        items(
                            items = group.rows,
                            key = { "${it.kind.name}_${it.id}" },
                        ) { row ->
                            CompactMovementRow(
                                row = row,
                                privacyModeEnabled = uiState.privacyModeEnabled,
                                onClick = { onMovementClick(row.kind, row.id) },
                            )
                            HorizontalDivider(
                                color = OutlineBorder,
                                modifier = Modifier.padding(start = 60.dp, end = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    // Sheet de selección de categoría
    if (uiState.isCategorySheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismissCategorySheet,
            sheetState = sheetState,
        ) {
            CategoryFilterSheetContent(
                categories = uiState.categories,
                selectedCategoryId = uiState.criteria.categoryId,
                onCategorySelected = onCategorySelected,
            )
        }
    }

    // Selector de rango de fechas personalizado
    if (uiState.isCustomDateRangeOpen) {
        val rangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = onDismissCustomPeriod,
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = rangePickerState.selectedStartDateMillis
                        val end = rangePickerState.selectedEndDateMillis ?: start
                        if (start != null && end != null) {
                            onCustomPeriodSelected(start, end)
                        }
                    },
                    enabled = rangePickerState.selectedStartDateMillis != null,
                ) {
                    Text("Aplicar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissCustomPeriod) {
                    Text("Cancelar")
                }
            },
        ) {
            DateRangePicker(
                state = rangePickerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
            )
        }
    }
}

@Composable
private fun AccountSegmentedFilter(
    accounts: List<AccountEntity>,
    selectedFilter: MovementAccountFilter,
    onFilterSelected: (MovementAccountFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val isAllSelected = selectedFilter is MovementAccountFilter.All
        AccountSegmentChip(
            label = "Todas",
            isSelected = isAllSelected,
            modifier = Modifier.weight(1f),
            onClick = { onFilterSelected(MovementAccountFilter.All) },
        )
        accounts.forEach { account ->
            val isSelected = selectedFilter is MovementAccountFilter.Account &&
                selectedFilter.accountId == account.id
            AccountSegmentChip(
                label = account.name,
                isSelected = isSelected,
                modifier = Modifier.weight(1f),
                onClick = { onFilterSelected(MovementAccountFilter.Account(account.id)) },
            )
        }
    }
}

@Composable
private fun AccountSegmentChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) PetroleumBluePrimary else PureWhiteSurface,
        border = BorderStroke(1.dp, if (isSelected) PetroleumBluePrimary else OutlineBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (isSelected) Color.White else PetroleumBluePrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PeriodFilterSection(
    periodFilter: MovementPeriodFilter,
    periodLabel: String,
    onSelectMonth: () -> Unit,
    onSelectLast12Months: () -> Unit,
    onSelectYear: () -> Unit,
    onOpenCustom: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                PeriodChip(
                    label = "Mes",
                    selected = periodFilter is MovementPeriodFilter.Month,
                    onClick = onSelectMonth,
                )
            }
            item {
                PeriodChip(
                    label = "12 meses",
                    selected = periodFilter is MovementPeriodFilter.Last12Months,
                    onClick = onSelectLast12Months,
                )
            }
            item {
                PeriodChip(
                    label = "Año",
                    selected = periodFilter is MovementPeriodFilter.Year,
                    onClick = onSelectYear,
                )
            }
            item {
                PeriodChip(
                    label = "Personalizado",
                    selected = periodFilter is MovementPeriodFilter.Custom,
                    onClick = onOpenCustom,
                )
            }
        }

        val supportsStepping = periodFilter is MovementPeriodFilter.Month ||
            periodFilter is MovementPeriodFilter.Year

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (supportsStepping) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Periodo anterior")
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = periodLabel,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.Black,
                )
            }

            if (supportsStepping) {
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Periodo siguiente")
                }
            } else if (periodFilter is MovementPeriodFilter.Custom) {
                TextButton(onClick = onOpenCustom) {
                    Text("Cambiar")
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@Composable
private fun PeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = PetroleumBluePrimary,
            selectedLabelColor = Color.White,
        ),
    )
}

@Composable
private fun SecondaryFiltersRow(
    selectedType: MovementTypeFilter,
    selectedCategoryName: String?,
    onTypeSelected: (MovementTypeFilter) -> Unit,
    onOpenCategorySheet: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            FilterChip(
                selected = selectedCategoryName != null,
                onClick = onOpenCategorySheet,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                label = {
                    Text(selectedCategoryName ?: "Categoría")
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PetroleumBluePrimary,
                    selectedLabelColor = Color.White,
                    selectedLeadingIconColor = Color.White,
                ),
            )
        }
        val typeItems = listOf(
            MovementTypeFilter.ALL to "Todos",
            MovementTypeFilter.EXPENSES to "Gastos",
            MovementTypeFilter.INCOMES to "Ingresos",
            MovementTypeFilter.ADVANCES to "Adelantos",
            MovementTypeFilter.TRANSFERS to "Transferencias",
            MovementTypeFilter.REIMBURSEMENTS to "Devoluciones",
        )
        items(typeItems) { (typeFilter, label) ->
            FilterChip(
                selected = selectedType == typeFilter,
                onClick = { onTypeSelected(typeFilter) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PetroleumBluePrimary,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

/**
 * Fila compacta estrictamente de DOS líneas:
 * Línea 1: Título/Nota + Importe
 * Línea 2: Categoría / Cuenta(s) origen→destino / Indicador de adelanto o devolución
 */
@Composable
private fun CompactMovementRow(
    row: MovementRowUiModel,
    privacyModeEnabled: Boolean,
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
                color = PetroleumBlueContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = when (row.kind) {
                        MovementKind.NORMAL_TRANSFER, MovementKind.REIMBURSEMENT -> Icons.Default.SwapHoriz
                        MovementKind.EXPENSE, MovementKind.INCOME -> resolveCategoryIcon(row.categoryIcon ?: "")
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = PetroleumBluePrimary,
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
                        text = row.line1Title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PetroleumBluePrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    MoneyText(
                        amountMinor = row.amountMinor,
                        isPrivacyModeEnabled = privacyModeEnabled,
                        showPlusSign = row.kind == MovementKind.INCOME,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (row.kind == MovementKind.INCOME) Color(0xFF2E7D32) else PetroleumBluePrimary,
                    )
                }

                Text(
                    text = row.line2Subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterSheetContent(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Filtrar por categoría",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Card(
            onClick = { onCategorySelected(null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedCategoryId == null) Color.Black else Color(0xFFF7F7F7),
            ),
        ) {
            Text(
                text = "Todas las categorías",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (selectedCategoryId == null) Color.White else Color.Black,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 320.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(categories) { category ->
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
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) Color.White else Color.Black,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
