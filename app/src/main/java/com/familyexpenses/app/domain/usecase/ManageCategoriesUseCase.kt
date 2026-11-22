package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.repository.CategoryRepository
import java.util.UUID
import javax.inject.Inject

sealed interface ToggleFavoriteResult {
    data object Updated : ToggleFavoriteResult
    data class ReplacementRequired(val currentFavorites: List<CategoryEntity>) : ToggleFavoriteResult
}

class ManageCategoriesUseCase @Inject constructor(
    private val categoryRepository: CategoryRepository,
) {

    suspend fun createCategory(
        name: String,
        icon: String,
        type: CategoryType,
    ): CategoryEntity {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "El nombre de la categoría no puede estar vacío." }

        val existing = categoryRepository.getAllCategories().filter { it.type == type }
        val nextOrder = (existing.maxOfOrNull { it.orderIndex } ?: 0) + 1
        val entity = CategoryEntity(
            id = "cat-${UUID.randomUUID()}",
            name = trimmed,
            icon = icon.ifBlank { "category" },
            type = type,
            orderIndex = nextOrder,
            isFavorite = false,
            favoritePosition = null,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
        )
        categoryRepository.upsert(entity)
        return entity
    }

    suspend fun updateCategory(
        categoryId: String,
        name: String,
        icon: String,
        type: CategoryType,
    ) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "El nombre de la categoría no puede estar vacío." }

        val all = categoryRepository.getAllCategories()
        val current = all.firstOrNull { it.id == categoryId }
            ?: throw IllegalArgumentException("Categoría no encontrada.")

        val wasExpenseFavorite = current.type == CategoryType.EXPENSE && current.isFavorite
        val keepFavorite = type == CategoryType.EXPENSE && current.isFavorite && !current.isArchived

        val updatedCurrent = current.copy(
            name = trimmed,
            icon = icon.ifBlank { "category" },
            type = type,
            isFavorite = keepFavorite,
            favoritePosition = if (keepFavorite) current.favoritePosition else null,
        )

        val toPersist = mutableListOf(updatedCurrent)
        if (wasExpenseFavorite && !keepFavorite) {
            toPersist += compactExpenseFavorites(all.filter { it.id != categoryId })
        }
        categoryRepository.upsertAll(toPersist.distinctBy { it.id })
    }

    suspend fun toggleFavorite(
        categoryId: String,
        replaceCategoryId: String? = null,
    ): ToggleFavoriteResult {
        val all = categoryRepository.getAllCategories()
        val target = all.firstOrNull { it.id == categoryId }
            ?: throw IllegalArgumentException("Categoría no encontrada.")

        require(target.type == CategoryType.EXPENSE) {
            "Solo las categorías de gasto admiten favoritas."
        }
        require(!target.isArchived) {
            "No se puede marcar como favorita una categoría archivada."
        }

        val activeFavorites = all
            .filter { it.type == CategoryType.EXPENSE && it.isFavorite && !it.isArchived }
            .sortedWith(compareBy<CategoryEntity> { it.favoritePosition ?: Int.MAX_VALUE }.thenBy { it.orderIndex })

        if (target.isFavorite) {
            val updatedTarget = target.copy(isFavorite = false, favoritePosition = null)
            val remaining = compactExpenseFavorites(
                all.filter { it.id != target.id },
            )
            categoryRepository.upsertAll(listOf(updatedTarget) + remaining)
            return ToggleFavoriteResult.Updated
        }

        if (activeFavorites.size < MAX_FAVORITE_EXPENSE_CATEGORIES) {
            val updatedTarget = target.copy(
                isFavorite = true,
                favoritePosition = activeFavorites.size + 1,
            )
            categoryRepository.upsert(updatedTarget)
            return ToggleFavoriteResult.Updated
        }

        if (replaceCategoryId == null) {
            return ToggleFavoriteResult.ReplacementRequired(activeFavorites)
        }

        val toReplace = activeFavorites.firstOrNull { it.id == replaceCategoryId }
            ?: throw IllegalArgumentException("La categoría favorita a sustituir no es válida.")

        val replacementPosition = toReplace.favoritePosition ?: MAX_FAVORITE_EXPENSE_CATEGORIES
        val demoted = toReplace.copy(isFavorite = false, favoritePosition = null)
        val promoted = target.copy(isFavorite = true, favoritePosition = replacementPosition)
        categoryRepository.upsertAll(listOf(demoted, promoted))
        return ToggleFavoriteResult.Updated
    }

    suspend fun moveFavorite(
        categoryId: String,
        direction: Int,
    ) {
        if (direction == 0) return
        val all = categoryRepository.getAllCategories()
        val favorites = all
            .filter { it.type == CategoryType.EXPENSE && it.isFavorite && !it.isArchived }
            .sortedWith(compareBy<CategoryEntity> { it.favoritePosition ?: Int.MAX_VALUE }.thenBy { it.orderIndex })
            .toMutableList()

        val index = favorites.indexOfFirst { it.id == categoryId }
        if (index == -1) return
        val targetIndex = (index + if (direction < 0) -1 else 1).coerceIn(0, favorites.lastIndex)
        if (index == targetIndex) return

        val item = favorites.removeAt(index)
        favorites.add(targetIndex, item)
        val reindexed = favorites.mapIndexed { pos, cat ->
            cat.copy(favoritePosition = pos + 1)
        }
        categoryRepository.upsertAll(reindexed)
    }

    suspend fun moveCategoryOrder(
        categoryId: String,
        direction: Int,
    ) {
        if (direction == 0) return
        val all = categoryRepository.getAllCategories()
        val target = all.firstOrNull { it.id == categoryId } ?: return

        val sameGroup = when (target.type) {
            CategoryType.EXPENSE -> all.filter {
                it.type == CategoryType.EXPENSE && !it.isArchived && !it.isFavorite
            }
            CategoryType.INCOME -> all.filter {
                it.type == CategoryType.INCOME && !it.isArchived
            }
        }.sortedWith(compareBy<CategoryEntity> { it.orderIndex }.thenBy { it.name })
            .toMutableList()

        val index = sameGroup.indexOfFirst { it.id == categoryId }
        if (index == -1) return
        val targetIndex = (index + if (direction < 0) -1 else 1).coerceIn(0, sameGroup.lastIndex)
        if (index == targetIndex) return

        val item = sameGroup.removeAt(index)
        sameGroup.add(targetIndex, item)
        val baseOffset = if (target.type == CategoryType.EXPENSE) MAX_FAVORITE_EXPENSE_CATEGORIES + 1 else 1
        val reindexed = sameGroup.mapIndexed { idx, cat ->
            cat.copy(orderIndex = baseOffset + idx)
        }
        categoryRepository.upsertAll(reindexed)
    }

    suspend fun setArchived(
        categoryId: String,
        archived: Boolean,
    ) {
        val all = categoryRepository.getAllCategories()
        val target = all.firstOrNull { it.id == categoryId }
            ?: throw IllegalArgumentException("Categoría no encontrada.")

        val updated = if (archived) {
            target.copy(
                isArchived = true,
                isFavorite = false,
                favoritePosition = null,
            )
        } else {
            val maxOrder = all.filter { it.type == target.type && !it.isArchived }
                .maxOfOrNull { it.orderIndex } ?: 0
            target.copy(
                isArchived = false,
                isFavorite = false,
                favoritePosition = null,
                orderIndex = maxOf(target.orderIndex, maxOrder + 1),
            )
        }

        val toPersist = mutableListOf(updated)
        if (archived && target.type == CategoryType.EXPENSE && target.isFavorite) {
            toPersist += compactExpenseFavorites(all.filter { it.id != target.id })
        }
        categoryRepository.upsertAll(toPersist.distinctBy { it.id })
    }

    private fun compactExpenseFavorites(candidates: List<CategoryEntity>): List<CategoryEntity> {
        return candidates
            .filter { it.type == CategoryType.EXPENSE && it.isFavorite && !it.isArchived }
            .sortedWith(compareBy<CategoryEntity> { it.favoritePosition ?: Int.MAX_VALUE }.thenBy { it.orderIndex })
            .mapIndexed { idx, cat ->
                cat.copy(favoritePosition = idx + 1)
            }
    }

    companion object {
        const val MAX_FAVORITE_EXPENSE_CATEGORIES = 7
    }
}
