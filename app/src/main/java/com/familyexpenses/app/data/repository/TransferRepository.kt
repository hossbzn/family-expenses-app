package com.familyexpenses.app.data.repository

import com.familyexpenses.app.data.local.dao.TransferDao
import com.familyexpenses.app.data.local.entity.TransferEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepository @Inject constructor(
    private val transferDao: TransferDao,
) {

    fun observeTransfers(): Flow<List<TransferEntity>> = transferDao.observeTransfers()

    suspend fun getAllTransfers(): List<TransferEntity> = transferDao.getAllTransfers()

    suspend fun getTransferById(id: String): TransferEntity? = transferDao.getTransferById(id)

    suspend fun addTransfer(transfer: TransferEntity) {
        transferDao.insertTransfer(transfer)
    }

    suspend fun deleteTransfer(transferId: String) {
        transferDao.deleteTransfer(transferId)
    }
}
