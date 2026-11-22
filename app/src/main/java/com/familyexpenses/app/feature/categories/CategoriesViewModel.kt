package com.familyexpenses.app.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.domain.usecase.ManageCategoriesUseCase
import com.familyexpenses.app.domain.usecase.ToggleFavoriteResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryEditorState(
    val editingCategoryId: String? = null,
    val nameInput: String = "",
    val iconInput: String = "category",
    val typeInput: CategoryType = CategoryType.EXPENSE,
    val isArchived: Boolean = false,
    val errorMessage: String? = null,
)

data class FavoriteReplacementPrompt(
    val targetCategory: CategoryEntity,
    val currentFavorites: List<CategoryEntity>,
)

data class CategoriesUiState(
    val selectedTab: CategoryType = CategoryType.EXPENSE,
    val expenseFavorites: List<CategoryEntity> = emptyList(),
    val expenseOthers: List<CategoryEntity> = emptyList(),
    val incomeCategories: List<CategoryEntity> = emptyList(),
    val archivedCategories: List<CategoryEntity> = emptyList(),
    val showArchived: Boolean = false,
    val editorState: CategoryEditorState? = null,
    val replacementPrompt: FavoriteReplacementPrompt? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    categoryRepository: CategoryRepository,
    private val manageCategoriesUseCase: ManageCategoriesUseCase,
) : ViewModel() {

    private val localState = MutableStateFlow(CategoriesLocalState())

    val uiState: StateFlow<CategoriesUiState> = combine(
        categoryRepository.observeCategories(),
        localState,
    ) { allCategories, state ->
        val activeExpenses = allCategories.filter { it.type == CategoryType.EXPENSE && !it.isArchived }
        val expenseFavorites = activeExpenses
            .filter { it.isFavorite }
            .sortedWith(compareBy<CategoryEntity> { it.favoritePosition ?: Int.MAX_VALUE }.thenBy { it.orderIndex })
        val expenseOthers = activeExpenses
            .filter { !it.isFavorite }
            .sortedWith(compareBy<CategoryEntity> { it.orderIndex }.thenBy { it.name })
        val incomeCategories = allCategories
            .filter { it.type == CategoryType.INCOME && !it.isArchived }
            .sortedWith(compareBy<CategoryEntity> { it.orderIndex }.thenBy { it.name })
        val archivedCategories = allCategories
            .filter { it.type == state.selectedTab && it.isArchived }
            .sortedBy { it.name }

        CategoriesUiState(
            selectedTab = state.selectedTab,
            expenseFavorites = expenseFavorites,
            expenseOthers = expenseOthers,
            incomeCategories = incomeCategories,
            archivedCategories = archivedCategories,
            showArchived = state.showArchived,
            editorState = state.editorState,
            replacementPrompt = state.replacementPrompt,
            errorMessage = state.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CategoriesUiState(),
    )

    fun onTabSelected(type: CategoryType) {
        localState.update { it.copy(selectedTab = type, errorMessage = null) }
    }

    fun onToggleShowArchived() {
        localState.update { it.copy(showArchived = !it.showArchived) }
    }

    fun onOpenCreate() {
        localState.update { state ->
            state.copy(
                editorState = CategoryEditorState(
                    editingCategoryId = null,
                    nameInput = "",
                    iconInput = if (state.selectedTab == CategoryType.EXPENSE) "shopping_cart" else "payments",
                    typeInput = state.selectedTab,
                ),
            )
        }
    }

    fun onOpenEdit(category: CategoryEntity) {
        localState.update {
            it.copy(
                editorState = CategoryEditorState(
                    editingCategoryId = category.id,
                    nameInput = category.name,
                    iconInput = category.icon,
                    typeInput = category.type,
                    isArchived = category.isArchived,
                ),
            )
        }
    }

    fun onEditorNameChanged(name: String) {
        localState.update { state ->
            state.copy(editorState = state.editorState?.copy(nameInput = name, errorMessage = null))
        }
    }

    fun onEditorIconSelected(icon: String) {
        localState.update { state ->
            state.copy(editorState = state.editorState?.copy(iconInput = icon))
        }
    }

    fun onEditorTypeSelected(type: CategoryType) {
        localState.update { state ->
            state.copy(editorState = state.editorState?.copy(typeInput = type))
        }
    }

    fun onDismissEditor() {
        localState.update { it.copy(editorState = null) }
    }

    fun onSaveEditor() {
        val editor = localState.value.editorState ?: return
        val trimmed = editor.nameInput.trim()
        if (trimmed.isEmpty()) {
            localState.update {
                it.copy(editorState = editor.copy(errorMessage = "Introduce un nombre."))
            }
            return
        }

        viewModelScope.launch {
            runCatching {
                if (editor.editingCategoryId == null) {
                    manageCategoriesUseCase.createCategory(
                        name = trimmed,
                        icon = editor.iconInput,
                        type = editor.typeInput,
                    )
                } else {
                    manageCategoriesUseCase.updateCategory(
                        categoryId = editor.editingCategoryId,
                        name = trimmed,
                        icon = editor.iconInput,
                        type = editor.typeInput,
                    )
                }
            }.onSuccess {
                localState.update { it.copy(editorState = null, selectedTab = editor.typeInput) }
            }.onFailure { err ->
                localState.update {
                    it.copy(editorState = editor.copy(errorMessage = err.message ?: "Error al guardar."))
                }
            }
        }
    }

    fun onToggleFavorite(category: CategoryEntity) {
        viewModelScope.launch {
            runCatching {
                manageCategoriesUseCase.toggleFavorite(category.id)
            }.onSuccess { result ->
                if (result is ToggleFavoriteResult.ReplacementRequired) {
                    localState.update {
                        it.copy(
                            replacementPrompt = FavoriteReplacementPrompt(
                                targetCategory = category,
                                currentFavorites = result.currentFavorites,
                            ),
                        )
                    }
                }
            }.onFailure { err ->
                localState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun onConfirmFavoriteReplacement(replaceCategoryId: String) {
        val prompt = localState.value.replacementPrompt ?: return
        viewModelScope.launch {
            runCatching {
                manageCategoriesUseCase.toggleFavorite(
                    categoryId = prompt.targetCategory.id,
                    replaceCategoryId = replaceCategoryId,
                )
            }.onSuccess {
                localState.update { it.copy(replacementPrompt = null) }
            }.onFailure { err ->
                localState.update { it.copy(replacementPrompt = null, errorMessage = err.message) }
            }
        }
    }

    fun onDismissReplacementPrompt() {
        localState.update { it.copy(replacementPrompt = null) }
    }

    fun onMoveFavoriteUp(categoryId: String) {
        viewModelScope.launch {
            manageCategoriesUseCase.moveFavorite(categoryId, direction = -1)
        }
    }

    fun onMoveFavoriteDown(categoryId: String) {
        viewModelScope.launch {
            manageCategoriesUseCase.moveFavorite(categoryId, direction = +1)
        }
    }

    fun onMoveOrderUp(categoryId: String) {
        viewModelScope.launch {
            manageCategoriesUseCase.moveCategoryOrder(categoryId, direction = -1)
        }
    }

    fun onMoveOrderDown(categoryId: String) {
        viewModelScope.launch {
            manageCategoriesUseCase.moveCategoryOrder(categoryId, direction = +1)
        }
    }

    fun onSetArchived(categoryId: String, archived: Boolean) {
        viewModelScope.launch {
            runCatching {
                manageCategoriesUseCase.setArchived(categoryId, archived)
            }.onSuccess {
                localState.update { it.copy(editorState = null) }
            }.onFailure { err ->
                localState.update { it.copy(errorMessage = err.message) }
            }
        }
    }

    fun onClearError() {
        localState.update { it.copy(errorMessage = null) }
    }
}

private data class CategoriesLocalState(
    val selectedTab: CategoryType = CategoryType.EXPENSE,
    val showArchived: Boolean = false,
    val editorState: CategoryEditorState? = null,
    val replacementPrompt: FavoriteReplacementPrompt? = null,
    val errorMessage: String? = null,
)
