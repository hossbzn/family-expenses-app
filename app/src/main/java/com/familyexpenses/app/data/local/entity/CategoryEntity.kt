package com.familyexpenses.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familyexpenses.app.core.model.CategoryType

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: CategoryType,
    val createdAt: Long,
)
