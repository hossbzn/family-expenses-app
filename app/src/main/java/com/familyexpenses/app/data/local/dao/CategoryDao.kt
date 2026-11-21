package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories ORDER BY orderIndex ASC, name ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY orderIndex ASC, name ASC")
    fun observeActiveCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type AND isArchived = 0 ORDER BY orderIndex ASC, name ASC")
    fun observeActiveCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = 'EXPENSE' AND isFavorite = 1 AND isArchived = 0 ORDER BY favoritePosition ASC")
    fun observeFavoriteExpenseCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryById(categoryId: String): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun countCategories(): Int
}
