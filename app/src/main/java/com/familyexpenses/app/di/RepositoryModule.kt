package com.familyexpenses.app.di

import com.familyexpenses.app.data.repository.BackupRepository
import com.familyexpenses.app.data.repository.RoomBackupRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepository
import com.familyexpenses.app.data.repository.UserPreferencesRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: UserPreferencesRepositoryImpl,
    ): UserPreferencesRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(
        impl: RoomBackupRepository,
    ): BackupRepository
}
