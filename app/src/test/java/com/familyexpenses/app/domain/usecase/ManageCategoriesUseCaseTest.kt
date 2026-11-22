package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ManageCategoriesUseCaseTest {

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

    private lateinit var useCase: ManageCategoriesUseCase

    @Before
    fun setUp() {
        val initial = (1..7).map { idx ->
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
                name = "Extra",
                icon = "school",
                type = CategoryType.EXPENSE,
                orderIndex = 8,
                isFavorite = false,
                favoritePosition = null,
                createdAt = 0L,
            ),
            CategoryEntity(
                id = "inc-1",
                name = "Salario",
                icon = "payments",
                type = CategoryType.INCOME,
                orderIndex = 1,
                isFavorite = false,
                createdAt = 0L,
            ),
            CategoryEntity(
                id = "inc-2",
                name = "Bonus",
                icon = "savings",
                type = CategoryType.INCOME,
                orderIndex = 2,
                isFavorite = false,
                createdAt = 0L,
            ),
        )
        categoriesFlow.value = initial
        useCase = ManageCategoriesUseCase(CategoryRepository(fakeCategoryDao))
    }

    @Test
    fun `enforces max 7 favorites and replaces chosen favorite cleanly`() = runTest {
        val result = useCase.toggleFavorite("exp-8")
        assertTrue(result is ToggleFavoriteResult.ReplacementRequired)
        val prompt = result as ToggleFavoriteResult.ReplacementRequired
        assertEquals(7, prompt.currentFavorites.size)

        // Sustituir fav-3 por exp-8
        val swapResult = useCase.toggleFavorite("exp-8", replaceCategoryId = "fav-3")
        assertEquals(ToggleFavoriteResult.Updated, swapResult)

        val replaced = categoriesFlow.value.first { it.id == "fav-3" }
        val promoted = categoriesFlow.value.first { it.id == "exp-8" }
        assertFalse(replaced.isFavorite)
        assertNull(replaced.favoritePosition)
        assertTrue(promoted.isFavorite)
        assertEquals(3, promoted.favoritePosition)
        assertEquals(7, categoriesFlow.value.count { it.isFavorite })
    }

    @Test
    fun `reorders favorites and income categories manually`() = runTest {
        useCase.moveFavorite("fav-2", direction = -1)
        val fav1 = categoriesFlow.value.first { it.id == "fav-1" }
        val fav2 = categoriesFlow.value.first { it.id == "fav-2" }
        assertEquals(1, fav2.favoritePosition)
        assertEquals(2, fav1.favoritePosition)

        useCase.moveCategoryOrder("inc-2", direction = -1)
        val inc1 = categoriesFlow.value.first { it.id == "inc-1" }
        val inc2 = categoriesFlow.value.first { it.id == "inc-2" }
        assertEquals(1, inc2.orderIndex)
        assertEquals(2, inc1.orderIndex)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `income category cannot be marked favorite`() = runTest {
        useCase.toggleFavorite("inc-1")
    }

    @Test
    fun `archiving a favorite frees slot and compacts remaining favorite positions`() = runTest {
        useCase.setArchived("fav-2", archived = true)

        val archived = categoriesFlow.value.first { it.id == "fav-2" }
        assertTrue(archived.isArchived)
        assertFalse(archived.isFavorite)
        assertNull(archived.favoritePosition)

        val activeFavs = categoriesFlow.value
            .filter { it.isFavorite && !it.isArchived }
            .sortedBy { it.favoritePosition }
        assertEquals(6, activeFavs.size)
        assertEquals(listOf(1, 2, 3, 4, 5, 6), activeFavs.map { it.favoritePosition })
    }
}
