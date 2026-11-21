package com.familyexpenses.app.data.repository

import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.entity.CategoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeCategories()

    fun observeActiveCategories(): Flow<List<CategoryEntity>> = categoryDao.observeActiveCategories()

    fun observeActiveCategoriesByType(type: CategoryType): Flow<List<CategoryEntity>> =
        categoryDao.observeActiveCategoriesByType(type)

    fun observeFavoriteExpenseCategories(): Flow<List<CategoryEntity>> =
        categoryDao.observeFavoriteExpenseCategories()

    fun observeExpenseCategories(): Flow<List<CategoryEntity>> = categoryDao.observeCategories()
        .map { categories ->
            categories.filter { it.type == CategoryType.EXPENSE }
        }

    suspend fun getAllCategories(): List<CategoryEntity> = categoryDao.observeCategories().first()

    suspend fun getCategoryById(id: String): CategoryEntity? = categoryDao.getCategoryById(id)

    suspend fun upsert(category: CategoryEntity) {
        categoryDao.upsert(category)
    }

    suspend fun upsertAll(categories: List<CategoryEntity>) {
        categoryDao.upsertAll(categories)
    }
}
