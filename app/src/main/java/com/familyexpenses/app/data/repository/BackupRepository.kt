package com.familyexpenses.app.data.repository

import com.familyexpenses.app.domain.model.PortableBackupSnapshot

interface BackupRepository {
    suspend fun readSnapshot(): PortableBackupSnapshot
    suspend fun replaceAll(snapshot: PortableBackupSnapshot)
}
