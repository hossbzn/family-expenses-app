package com.familyexpenses.app.feature.categories

import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.repository.CategoryRepository
import com.familyexpenses.app.domain.usecase.ManageCategoriesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(emptyList())

    private val fakeCategoryDao = object : CategoryDao {
        override suspend fun upsert(category: CategoryEntity) {
            categoriesFlow.value = categoriesFlow.value.filterNot { it.id == category.id } + category
        }

        override suspend fun upsertAll(categories: List<CategoryEntity>) {
            val map = categoriesFlow.value.associateBy { it.id }.toMutableMap()
            categories.forEach { map[it.id] = it }
            categoriesFlow.value = map.values.toList()
        }

        override fun observeCategories(): Flow<List<CategoryEntity>> = categoriesFlow
        override fun observeActiveCategories(): Flow<List<CategoryEntity>> =
            categoriesFlow.map { it.filter { cat -> !cat.isArchived } }
        override fun observeActiveCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> =
            categoriesFlow.map { it.filter { cat -> !cat.isArchived && cat.type == type } }
        override fun observeFavoriteExpenseCategories(): Flow<List<CategoryEntity>> =
            categoriesFlow.map { it.filter { cat -> !cat.isArchived && cat.type == CategoryType.EXPENSE && cat.isFavorite } }
        override suspend fun getCategoryById(categoryId: String): CategoryEntity? =
            categoriesFlow.value.find { it.id == categoryId }
        override suspend fun countCategories(): Int = categoriesFlow.value.size
    }

    private lateinit var viewModel: CategoriesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        categoriesFlow.value = (1..7).map { idx ->
            CategoryEntity(
                id = "fav-$idx",
                name = "Fav $idx",
                icon = "shopping_cart",
                type = CategoryType.EXPENSE,
                orderIndex = idx,
                isFavorite = true,
                favoritePosition = idx,
                createdAt = 0L,
            )
        } + listOf(
            CategoryEntity(
                id = "exp-8",
                name = "Mascotas",
                icon = "pets",
                type = CategoryType.EXPENSE,
                orderIndex = 8,
                isFavorite = false,
                createdAt = 0L,
            ),
            CategoryEntity(
                id = "inc-1",
                name = "Nómina",
                icon = "payments",
                type = CategoryType.INCOME,
                orderIndex = 1,
                isFavorite = false,
                createdAt = 0L,
            ),
        )

        viewModel = CategoriesViewModel(
            categoryRepository = CategoryRepository(fakeCategoryDao),
            manageCategoriesUseCase = ManageCategoriesUseCase(CategoryRepository(fakeCategoryDao)),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `prompts replacement when toggling 8th favorite and creates new category`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()

        assertEquals(7, viewModel.uiState.value.expenseFavorites.size)
        assertEquals(1, viewModel.uiState.value.expenseOthers.size)

        val extra = viewModel.uiState.value.expenseOthers.first()
        viewModel.onToggleFavorite(extra)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.replacementPrompt)
        viewModel.onConfirmFavoriteReplacement("fav-7")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.replacementPrompt)
        assertEquals("exp-8", viewModel.uiState.value.expenseFavorites.last().id)

        // Crear nueva categoría de ingreso
        viewModel.onTabSelected(CategoryType.INCOME)
        viewModel.onOpenCreate()
        viewModel.onEditorNameChanged("Alquiler")
        viewModel.onEditorIconSelected("home")
        viewModel.onSaveEditor()
        advanceUntilIdle()

        assertEquals(CategoryType.INCOME, viewModel.uiState.value.selectedTab)
        assertEquals(2, viewModel.uiState.value.incomeCategories.size)
    }
}
