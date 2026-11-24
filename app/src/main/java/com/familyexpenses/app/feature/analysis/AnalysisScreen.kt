package com.familyexpenses.app.feature.analysis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.familyexpenses.app.core.ui.MoneyText
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.domain.usecase.AnalysisCategorySlice
import com.familyexpenses.app.domain.usecase.AnalysisEvolutionBucket
import com.familyexpenses.app.domain.usecase.AnalysisSeriesType
import com.familyexpenses.app.domain.usecase.MovementAccountFilter
import com.familyexpenses.app.domain.usecase.MovementPeriodFilter
import com.familyexpenses.app.feature.expense.resolveCategoryIcon
import com.familyexpenses.app.ui.theme.ExpenseBorder
import com.familyexpenses.app.ui.theme.ExpenseContainer
import com.familyexpenses.app.ui.theme.IncomeBorder
import com.familyexpenses.app.ui.theme.IncomeContainer
import com.familyexpenses.app.ui.theme.OffWhiteBackground
import com.familyexpenses.app.ui.theme.OnExpenseContainer
import com.familyexpenses.app.ui.theme.OnIncomeContainer
import com.familyexpenses.app.ui.theme.OutlineBorder
import com.familyexpenses.app.ui.theme.PetroleumBlueContainer
import com.familyexpenses.app.ui.theme.PetroleumBluePrimary
import com.familyexpenses.app.ui.theme.PureWhiteSurface
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val MonochromeShades = listOf(
    Color(0xFF111111),
    Color(0xFF3A3A3A),
    Color(0xFF616161),
    Color(0xFF888888),
    Color(0xFFA8A8A8),
    Color(0xFFC4C4C4),
    Color(0xFFDDDDDD),
)

@Composable
fun AnalysisRoute(
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onDrillDownToHistory: (AnalysisDrillDownRequest) -> Unit,
    viewModel: AnalysisViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnalysisScreen(
        uiState = uiState,
        onHomeClick = onHomeClick,
        onHistoryClick = onHistoryClick,
        onTabSelected = viewModel::onTabSelected,
        onAccountFilterSelected = viewModel::onAccountFilterSelected,
        onSeriesTypeSelected = viewModel::onSeriesTypeSelected,
        onSelectMonthPeriod = viewModel::onSelectMonthPeriod,
        onSelectLast12MonthsPeriod = viewModel::onSelectLast12MonthsPeriod,
        onSelectYearPeriod = viewModel::onSelectYearPeriod,
        onOpenCustomPeriod = viewModel::onOpenCustomPeriod,
        onDismissCustomPeriod = viewModel::onDismissCustomPeriod,
        onCustomPeriodSelected = viewModel::onCustomPeriodSelected,
        onPreviousPeriod = viewModel::onPreviousPeriod,
        onNextPeriod = viewModel::onNextPeriod,
        onCategoryClick = { categoryId ->
            onDrillDownToHistory(viewModel.buildCategoryDrillDown(categoryId))
        },
        onBucketClick = { bucket ->
            if (!bucket.isFuture) {
                onDrillDownToHistory(viewModel.buildBucketDrillDown(bucket))
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AnalysisScreen(
    uiState: AnalysisUiState,
    onHomeClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onTabSelected: (AnalysisTab) -> Unit,
    onAccountFilterSelected: (MovementAccountFilter) -> Unit,
    onSeriesTypeSelected: (AnalysisSeriesType) -> Unit,
    onSelectMonthPeriod: () -> Unit,
    onSelectLast12MonthsPeriod: () -> Unit,
    onSelectYearPeriod: () -> Unit,
    onOpenCustomPeriod: () -> Unit,
    onDismissCustomPeriod: () -> Unit,
    onCustomPeriodSelected: (Long, Long) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onBucketClick: (AnalysisEvolutionBucket) -> Unit,
) {
    Scaffold(
        containerColor = OffWhiteBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Análisis",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OffWhiteBackground,
                    titleContentColor = PetroleumBluePrimary,
                ),
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = PureWhiteSurface,
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
                    selected = false,
                    onClick = onHistoryClick,
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Movimientos") },
                    label = { Text("Movimientos") },
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                    ),
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { /* Ya en Análisis */ },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Análisis") },
                    label = { Text("Análisis") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PureWhiteSurface,
                        selectedTextColor = PetroleumBluePrimary,
                        indicatorColor = PetroleumBluePrimary,
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray,
                    ),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Filtros compartidos: Cuenta
            AnalysisAccountFilter(
                accounts = uiState.accounts,
                selectedFilter = uiState.accountFilter,
                onFilterSelected = onAccountFilterSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // Selector exclusivo de serie única: Gastos OR Ingresos
            SeriesTypeSelector(
                selectedSeries = uiState.seriesType,
                onSeriesSelected = onSeriesTypeSelected,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            // Periodo compartido
            AnalysisPeriodSection(
                periodFilter = uiState.periodFilter,
                periodLabel = uiState.periodLabel,
                onSelectMonth = onSelectMonthPeriod,
                onSelectLast12Months = onSelectLast12MonthsPeriod,
                onSelectYear = onSelectYearPeriod,
                onOpenCustom = onOpenCustomPeriod,
                onPrevious = onPreviousPeriod,
                onNext = onNextPeriod,
            )

            // Pestañas: Categorías (predeterminada) / Evolución
            val selectedTabIndex = if (uiState.selectedTab == AnalysisTab.CATEGORIES) 0 else 1
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = PureWhiteSurface,
                contentColor = PetroleumBluePrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = PetroleumBluePrimary,
                    )
                },
            ) {
                Tab(
                    selected = uiState.selectedTab == AnalysisTab.CATEGORIES,
                    onClick = { onTabSelected(AnalysisTab.CATEGORIES) },
                    text = {
                        Text(
                            text = "Categorías",
                            fontWeight = if (uiState.selectedTab == AnalysisTab.CATEGORIES) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                )
                Tab(
                    selected = uiState.selectedTab == AnalysisTab.EVOLUTION,
                    onClick = { onTabSelected(AnalysisTab.EVOLUTION) },
                    text = {
                        Text(
                            text = "Evolución",
                            fontWeight = if (uiState.selectedTab == AnalysisTab.EVOLUTION) FontWeight.Bold else FontWeight.Medium,
                        )
                    },
                )
            }

            when (uiState.selectedTab) {
                AnalysisTab.CATEGORIES -> CategoriesAnalysisContent(
                    uiState = uiState,
                    onCategoryClick = onCategoryClick,
                )
                AnalysisTab.EVOLUTION -> EvolutionAnalysisContent(
                    uiState = uiState,
                    onBucketClick = onBucketClick,
                )
            }
        }
    }

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
private fun AnalysisAccountFilter(
    accounts: List<AccountEntity>,
    selectedFilter: MovementAccountFilter,
    onFilterSelected: (MovementAccountFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnalysisSegmentChip(
            label = "Todas",
            isSelected = selectedFilter is MovementAccountFilter.All,
            modifier = Modifier.weight(1f),
            onClick = { onFilterSelected(MovementAccountFilter.All) },
        )
        accounts.forEach { account ->
            val isSelected = selectedFilter is MovementAccountFilter.Account &&
                selectedFilter.accountId == account.id
            AnalysisSegmentChip(
                label = account.name,
                isSelected = isSelected,
                modifier = Modifier.weight(1f),
                onClick = { onFilterSelected(MovementAccountFilter.Account(account.id)) },
            )
        }
    }
}

@Composable
private fun SeriesTypeSelector(
    selectedSeries: AnalysisSeriesType,
    onSeriesSelected: (AnalysisSeriesType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SemanticSeriesChip(
            label = "Gastos",
            icon = Icons.Default.Remove,
            isSelected = selectedSeries == AnalysisSeriesType.EXPENSES,
            selectedContainerColor = ExpenseContainer,
            selectedContentColor = OnExpenseContainer,
            selectedBorderColor = ExpenseBorder,
            modifier = Modifier.weight(1f),
            onClick = { onSeriesSelected(AnalysisSeriesType.EXPENSES) },
        )
        SemanticSeriesChip(
            label = "Ingresos",
            icon = Icons.Default.Add,
            isSelected = selectedSeries == AnalysisSeriesType.INCOMES,
            selectedContainerColor = IncomeContainer,
            selectedContentColor = OnIncomeContainer,
            selectedBorderColor = IncomeBorder,
            modifier = Modifier.weight(1f),
            onClick = { onSeriesSelected(AnalysisSeriesType.INCOMES) },
        )
    }
}

@Composable
private fun SemanticSeriesChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedContainerColor: Color,
    selectedContentColor: Color,
    selectedBorderColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val contentColor = if (isSelected) selectedContentColor else PetroleumBluePrimary
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) selectedContainerColor else PureWhiteSurface,
        border = BorderStroke(1.dp, if (isSelected) selectedBorderColor else OutlineBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AnalysisSegmentChip(
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
private fun AnalysisPeriodSection(
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
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                AnalysisPeriodChip(
                    label = "Mes",
                    selected = periodFilter is MovementPeriodFilter.Month,
                    onClick = onSelectMonth,
                )
            }
            item {
                AnalysisPeriodChip(
                    label = "12 meses",
                    selected = periodFilter is MovementPeriodFilter.Last12Months,
                    onClick = onSelectLast12Months,
                )
            }
            item {
                AnalysisPeriodChip(
                    label = "Año",
                    selected = periodFilter is MovementPeriodFilter.Year,
                    onClick = onSelectYear,
                )
            }
            item {
                AnalysisPeriodChip(
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
private fun AnalysisPeriodChip(
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
private fun CategoriesAnalysisContent(
    uiState: AnalysisUiState,
    onCategoryClick: (String) -> Unit,
) {
    val seriesLabel = if (uiState.seriesType == AnalysisSeriesType.EXPENSES) "Total gastos" else "Total ingresos"

    if (uiState.categorySlices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (uiState.seriesType == AnalysisSeriesType.EXPENSES) {
                    "No hay gastos en este periodo."
                } else {
                    "No hay ingresos en este periodo."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
                border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MonochromeDonutChart(
                        slices = uiState.categorySlices,
                        totalMinor = uiState.totalMinor,
                        seriesLabel = seriesLabel,
                        privacyModeEnabled = uiState.privacyModeEnabled,
                    )
                    Text(
                        text = "${uiState.transactionCount} movimientos · Pulsa una categoría para ver su detalle",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        itemsIndexed(
            items = uiState.categorySlices,
            key = { _, slice -> slice.categoryId },
        ) { index, slice ->
            val shade = MonochromeShades[index % MonochromeShades.size]
            CategoryRankingRow(
                slice = slice,
                shade = shade,
                privacyModeEnabled = uiState.privacyModeEnabled,
                onClick = { onCategoryClick(slice.categoryId) },
            )
        }
    }
}

@Composable
private fun MonochromeDonutChart(
    slices: List<AnalysisCategorySlice>,
    totalMinor: Long,
    seriesLabel: String,
    privacyModeEnabled: Boolean,
) {
    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 30.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f,
            )
            val arcSize = Size(diameter, diameter)

            if (totalMinor <= 0L) {
                drawArc(
                    color = Color(0xFFE0E0E0),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth),
                )
            } else {
                var startAngle = -90f
                val gapDegrees = if (slices.size > 1) 1.5f else 0f
                slices.forEachIndexed { index, slice ->
                    val rawSweep = (slice.amountMinor.toFloat() / totalMinor.toFloat()) * 360f
                    val sweep = (rawSweep - gapDegrees).coerceAtLeast(1f)
                    val color = MonochromeShades[index % MonochromeShades.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                    )
                    startAngle += rawSweep
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Text(
                text = seriesLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
            )
            MoneyText(
                amountMinor = totalMinor,
                isPrivacyModeEnabled = privacyModeEnabled,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = PetroleumBluePrimary,
            )
        }
    }
}

@Composable
private fun CategoryRankingRow(
    slice: AnalysisCategorySlice,
    shade: Color,
    privacyModeEnabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhiteSurface),
        border = BorderStroke(1.dp, OutlineBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(shade),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = resolveCategoryIcon(slice.categoryIcon),
                    contentDescription = null,
                    tint = PetroleumBluePrimary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slice.categoryName,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = PetroleumBluePrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${slice.transactionCount} movimientos",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    MoneyText(
                        amountMinor = slice.amountMinor,
                        isPrivacyModeEnabled = privacyModeEnabled,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = PetroleumBluePrimary,
                    )
                    Text(
                        text = String.format(Locale("es", "ES"), "%.1f %%", slice.percentage),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                }
            }

            // Barra de proporción
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFF0F0F0)),
            ) {
                val fraction = (slice.percentage / 100f).coerceIn(0.02f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(shade),
                )
            }
        }
    }
}

@Composable
private fun EvolutionAnalysisContent(
    uiState: AnalysisUiState,
    onBucketClick: (AnalysisEvolutionBucket) -> Unit,
) {
    val maxMinor = uiState.evolutionBuckets
        .mapNotNull { it.amountMinor }
        .maxOrNull()
        ?.coerceAtLeast(1L)
        ?: 1L

    val seriesLabel = if (uiState.seriesType == AnalysisSeriesType.EXPENSES) "Gastos" else "Ingresos"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SummaryMiniCard(
                    title = "Total $seriesLabel",
                    amountMinor = uiState.totalMinor,
                    privacyModeEnabled = uiState.privacyModeEnabled,
                    modifier = Modifier.weight(1f),
                )
                SummaryMiniCard(
                    title = "Media periodo",
                    amountMinor = uiState.averagePerBucketMinor,
                    privacyModeEnabled = uiState.privacyModeEnabled,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7)),
                border = BorderStroke(1.dp, Color(0xFFE5E5E5)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "$seriesLabel por tramo · Pulsa una barra para ver movimientos",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )

                    EvolutionBarChart(
                        buckets = uiState.evolutionBuckets,
                        maxMinor = maxMinor,
                        onBucketClick = onBucketClick,
                    )
                }
            }
        }

        val elapsedBucketsWithDataOrPast = uiState.evolutionBuckets
            .filter { !it.isFuture }
            .reversed()

        if (elapsedBucketsWithDataOrPast.isNotEmpty()) {
            item {
                Text(
                    text = "Desglose por periodo",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            items(
                items = elapsedBucketsWithDataOrPast,
                key = { "${it.startDate}_${it.endDate}" },
            ) { bucket ->
                Surface(
                    onClick = { onBucketClick(bucket) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bucket.fullLabel,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.Black,
                            )
                            Text(
                                text = "${bucket.transactionCount} movimientos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                        MoneyText(
                            amountMinor = bucket.amountMinor ?: 0L,
                            isPrivacyModeEnabled = uiState.privacyModeEnabled,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = PetroleumBluePrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMiniCard(
    title: String,
    amountMinor: Long,
    privacyModeEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhiteSurface),
        border = BorderStroke(1.dp, OutlineBorder),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
            )
            MoneyText(
                amountMinor = amountMinor,
                isPrivacyModeEnabled = privacyModeEnabled,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = PetroleumBluePrimary,
            )
        }
    }
}

@Composable
private fun EvolutionBarChart(
    buckets: List<AnalysisEvolutionBucket>,
    maxMinor: Long,
    onBucketClick: (AnalysisEvolutionBucket) -> Unit,
) {
    val chartHeight = 160.dp

    if (buckets.size <= 12) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight + 28.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            buckets.forEach { bucket ->
                EvolutionBarColumn(
                    bucket = bucket,
                    maxMinor = maxMinor,
                    chartHeight = chartHeight,
                    modifier = Modifier.weight(1f),
                    onClick = { onBucketClick(bucket) },
                )
            }
        }
    } else {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight + 28.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            items(
                items = buckets,
                key = { "${it.startDate}_${it.endDate}" },
            ) { bucket ->
                EvolutionBarColumn(
                    bucket = bucket,
                    maxMinor = maxMinor,
                    chartHeight = chartHeight,
                    modifier = Modifier.width(28.dp),
                    onClick = { onBucketClick(bucket) },
                )
            }
        }
    }
}

@Composable
private fun EvolutionBarColumn(
    bucket: AnalysisEvolutionBucket,
    maxMinor: Long,
    chartHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val amountMinor = bucket.amountMinor
    Column(
        modifier = modifier
            .then(
                if (!bucket.isFuture) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight),
            contentAlignment = Alignment.BottomCenter,
        ) {
            if (amountMinor != null) {
                val fraction = if (amountMinor == 0L) {
                    0.02f
                } else {
                    (amountMinor.toFloat() / maxMinor.toFloat()).coerceIn(0.04f, 1f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.72f)
                        .fillMaxHeight(fraction)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (amountMinor > 0L) Color.Black else Color(0xFFD0D0D0)),
                )
            } else {
                // Periodo futuro: sin barra (vacío, sin ceros ficticios)
                HorizontalDivider(
                    color = Color(0xFFEAEAEA),
                    modifier = Modifier.fillMaxWidth(0.5f),
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = bucket.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (bucket.isFuture) Color(0xFFBDBDBD) else Color.DarkGray,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatCurrency(minorAmount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "ES")).apply {
        currency = Currency.getInstance("EUR")
    }
    return formatter.format(minorAmount / 100.0)
}
