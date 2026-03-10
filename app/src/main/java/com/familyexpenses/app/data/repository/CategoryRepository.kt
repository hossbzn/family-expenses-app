package com.familyexpenses.app.data.repository

import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.data.local.dao.CategoryDao
import com.familyexpenses.app.data.local.entity.CategoryEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
) {
    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeCategories()

    fun observeExpenseCategories(): Flow<List<CategoryEntity>> = categoryDao.observeCategories()
        .map { categories ->
            categories.filter { it.type == CategoryType.EXPENSE }
        }
}
