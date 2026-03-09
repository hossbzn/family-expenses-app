package com.familyexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.familyexpenses.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Upsert
    suspend fun upsert(user: UserEntity)

    @Query("SELECT * FROM users LIMIT 1")
    fun observePrimaryUser(): Flow<UserEntity?>
}
