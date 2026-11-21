package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String = "category",
    val type: CategoryType,
    val orderIndex: Int = 0,
    val isFavorite: Boolean = false,
    val favoritePosition: Int? = null,
    val isArchived: Boolean = false,
    val createdAt: Long,
)
