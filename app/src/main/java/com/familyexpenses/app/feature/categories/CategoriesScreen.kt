package com.familyexpenses.app.feature.categories

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.ui.AVAILABLE_CATEGORY_ICONS
import com.familyexpenses.app.core.ui.resolveAppIcon
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.domain.usecase.ManageCategoriesUseCase

@Composable
fun CategoriesRoute(
    onBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CategoriesScreen(
        uiState = uiState,
        onBack = onBack,
        onTabSelected = viewModel::onTabSelected,
        onToggleShowArchived = viewModel::onToggleShowArchived,
        onOpenCreate = viewModel::onOpenCreate,
        onOpenEdit = viewModel::onOpenEdit,
        onEditorNameChanged = viewModel::onEditorNameChanged,
        onEditorIconSelected = viewModel::onEditorIconSelected,
        onEditorTypeSelected = viewModel::onEditorTypeSelected,
        onDismissEditor = viewModel::onDismissEditor,
        onSaveEditor = viewModel::onSaveEditor,
        onToggleFavorite = viewModel::onToggleFavorite,
        onConfirmFavoriteReplacement = viewModel::onConfirmFavoriteReplacement,
        onDismissReplacementPrompt = viewModel::onDismissReplacementPrompt,
        onMoveFavoriteUp = viewModel::onMoveFavoriteUp,
        onMoveFavoriteDown = viewModel::onMoveFavoriteDown,
        onMoveOrderUp = viewModel::onMoveOrderUp,
        onMoveOrderDown = viewModel::onMoveOrderDown,
        onSetArchived = viewModel::onSetArchived,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CategoriesScreen(
    uiState: CategoriesUiState,
    onBack: () -> Unit,
    onTabSelected: (CategoryType) -> Unit,
    onToggleShowArchived: () -> Unit,
    onOpenCreate: () -> Unit,
    onOpenEdit: (CategoryEntity) -> Unit,
    onEditorNameChanged: (String) -> Unit,
    onEditorIconSelected: (String) -> Unit,
    onEditorTypeSelected: (CategoryType) -> Unit,
    onDismissEditor: () -> Unit,
    onSaveEditor: () -> Unit,
    onToggleFavorite: (CategoryEntity) -> Unit,
    onConfirmFavoriteReplacement: (String) -> Unit,
    onDismissReplacementPrompt: () -> Unit,
    onMoveFavoriteUp: (String) -> Unit,
    onMoveFavoriteDown: (String) -> Unit,
    onMoveOrderUp: (String) -> Unit,
    onMoveOrderDown: (String) -> Unit,
    onSetArchived: (String, Boolean) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Categorías",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenCreate,
                containerColor = Color.Black,
                contentColor = Color.White,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva categoría")
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val selectedTabIndex = if (uiState.selectedTab == CategoryType.EXPENSE) 0 else 1
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.Black,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color.Black,
                    )
                },
            ) {
                Tab(
                    selected = uiState.selectedTab == CategoryType.EXPENSE,
                    onClick = { onTabSelected(CategoryType.EXPENSE) },
                    text = { Text("Gastos", fontWeight = FontWeight.SemiBold) },
                )
                Tab(
                    selected = uiState.selectedTab == CategoryType.INCOME,
                    onClick = { onTabSelected(CategoryType.INCOME) },
                    text = { Text("Ingresos", fontWeight = FontWeight.SemiBold) },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (uiState.selectedTab == CategoryType.EXPENSE) {
                    item {
                        Text(
                            text = "Favoritas (${uiState.expenseFavorites.size}/${ManageCategoriesUseCase.MAX_FAVORITE_EXPENSE_CATEGORIES})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black,
                        )
                        Text(
                            text = "Aparecen en la cuadrícula rápida de Nuevo gasto. Usa la estrella o las flechas para ordenar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }

                    itemsIndexed(
                        items = uiState.expenseFavorites,
                        key = { _, cat -> "fav_${cat.id}" },
                    ) { idx, category ->
                        CategoryManageRow(
                            category = category,
                            badgeText = "${idx + 1}",
                            showStar = true,
                            isFavorite = true,
                            canMoveUp = idx > 0,
                            canMoveDown = idx < uiState.expenseFavorites.lastIndex,
                            onClick = { onOpenEdit(category) },
                            onToggleFavorite = { onToggleFavorite(category) },
                            onMoveUp = { onMoveFavoriteUp(category.id) },
                            onMoveDown = { onMoveFavoriteDown(category.id) },
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Otras categorías de gasto",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black,
                        )
                    }

                    if (uiState.expenseOthers.isEmpty()) {
                        item {
                            Text(
                                text = "Todas las categorías de gasto activas están en favoritas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.expenseOthers,
                            key = { _, cat -> "exp_${cat.id}" },
                        ) { idx, category ->
                            CategoryManageRow(
                                category = category,
                                badgeText = null,
                                showStar = true,
                                isFavorite = false,
                                canMoveUp = idx > 0,
                                canMoveDown = idx < uiState.expenseOthers.lastIndex,
                                onClick = { onOpenEdit(category) },
                                onToggleFavorite = { onToggleFavorite(category) },
                                onMoveUp = { onMoveOrderUp(category.id) },
                                onMoveDown = { onMoveOrderDown(category.id) },
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "Categorías de ingresos (orden manual)",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black,
                        )
                        Text(
                            text = "Los ingresos muestran todas las categorías activas en este orden.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                        )
                    }

                    itemsIndexed(
                        items = uiState.incomeCategories,
                        key = { _, cat -> "inc_${cat.id}" },
                    ) { idx, category ->
                        CategoryManageRow(
                            category = category,
                            badgeText = "${idx + 1}",
                            showStar = false,
                            isFavorite = false,
                            canMoveUp = idx > 0,
                            canMoveDown = idx < uiState.incomeCategories.lastIndex,
                            onClick = { onOpenEdit(category) },
                            onToggleFavorite = {},
                            onMoveUp = { onMoveOrderUp(category.id) },
                            onMoveDown = { onMoveOrderDown(category.id) },
                        )
                    }
                }

                if (uiState.archivedCategories.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(onClick = onToggleShowArchived) {
                            Text(
                                text = if (uiState.showArchived) {
                                    "Ocultar archivadas (${uiState.archivedCategories.size})"
                                } else {
                                    "Mostrar archivadas (${uiState.archivedCategories.size})"
                                },
                            )
                        }
                    }

                    if (uiState.showArchived) {
                        items(
                            items = uiState.archivedCategories,
                            key = { "arch_${it.id}" },
                        ) { category ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3F3)),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = resolveAppIcon(category.icon),
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Gray,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = { onSetArchived(category.id, false) }) {
                                        Icon(
                                            imageVector = Icons.Default.Unarchive,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restaurar")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val editor = uiState.editorState
    if (editor != null) {
        CategoryEditorDialog(
            editor = editor,
            onNameChanged = onEditorNameChanged,
            onIconSelected = onEditorIconSelected,
            onTypeSelected = onEditorTypeSelected,
            onArchiveClick = {
                editor.editingCategoryId?.let { id -> onSetArchived(id, true) }
            },
            onDismiss = onDismissEditor,
            onSave = onSaveEditor,
        )
    }

    val prompt = uiState.replacementPrompt
    if (prompt != null) {
        AlertDialog(
            onDismissRequest = onDismissReplacementPrompt,
            title = { Text("Sustituir favorita (7/7)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Ya tienes 7 favoritas. Elige cuál quieres sustituir por «${prompt.targetCategory.name}»:",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(prompt.currentFavorites, key = { it.id }) { fav ->
                            Surface(
                                onClick = { onConfirmFavoriteReplacement(fav.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF5F5F5),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Icon(
                                        imageVector = resolveAppIcon(fav.icon),
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Text(
                                        text = fav.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = Color.Black,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissReplacementPrompt) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun CategoryManageRow(
    category: CategoryEntity,
    badgeText: String?,
    showStar: Boolean,
    isFavorite: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFavorite) Color(0xFFF7F7F7) else Color.White,
        ),
        border = BorderStroke(1.dp, if (isFavorite) Color(0xFFD0D0D0) else Color(0xFFEEEEEE)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (badgeText != null) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Icon(
                imageVector = resolveAppIcon(category.icon),
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp),
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Subir",
                )
            }

            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Bajar",
                )
            }

            if (showStar) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (isFavorite) "Quitar de favoritas" else "Marcar favorita",
                        tint = if (isFavorite) Color.Black else Color.Gray,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryEditorDialog(
    editor: CategoryEditorState,
    onNameChanged: (String) -> Unit,
    onIconSelected: (String) -> Unit,
    onTypeSelected: (CategoryType) -> Unit,
    onArchiveClick: () -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val isEditing = editor.editingCategoryId != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditing) "Editar categoría" else "Nueva categoría")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = editor.nameInput,
                    onValueChange = onNameChanged,
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val expenseSelected = editor.typeInput == CategoryType.EXPENSE
                    Surface(
                        onClick = { onTypeSelected(CategoryType.EXPENSE) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (expenseSelected) Color.Black else Color(0xFFF5F5F5),
                        border = BorderStroke(1.dp, if (expenseSelected) Color.Black else Color(0xFFE0E0E0)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Gasto",
                                color = if (expenseSelected) Color.White else Color.Black,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Surface(
                        onClick = { onTypeSelected(CategoryType.INCOME) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (!expenseSelected) Color.Black else Color(0xFFF5F5F5),
                        border = BorderStroke(1.dp, if (!expenseSelected) Color.Black else Color(0xFFE0E0E0)),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Ingreso",
                                color = if (!expenseSelected) Color.White else Color.Black,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                Text(
                    text = "Icono",
                    style = MaterialTheme.typography.labelLarge,
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AVAILABLE_CATEGORY_ICONS, key = { it.key }) { option ->
                        val selected = option.key == editor.iconInput
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

                if (isEditing && !editor.isArchived) {
                    OutlinedButton(
                        onClick = onArchiveClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Archivar categoría")
                    }
                }

                if (editor.errorMessage != null) {
                    Text(
                        text = editor.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
