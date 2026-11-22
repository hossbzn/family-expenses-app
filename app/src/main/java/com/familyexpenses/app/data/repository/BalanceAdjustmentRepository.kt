package com.familyexpenses.app.data.repository

import com.familyexpenses.app.data.local.dao.BalanceAdjustmentDao
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceAdjustmentRepository @Inject constructor(
    private val balanceAdjustmentDao: BalanceAdjustmentDao,
) {

    fun observeAdjustments(): Flow<List<BalanceAdjustmentEntity>> = balanceAdjustmentDao.observeAdjustments()

    fun observeAdjustmentsForAccount(accountId: String): Flow<List<BalanceAdjustmentEntity>> =
        balanceAdjustmentDao.observeAdjustmentsForAccount(accountId)

    suspend fun getAllAdjustments(): List<BalanceAdjustmentEntity> = balanceAdjustmentDao.getAllAdjustments()

    suspend fun addAdjustment(adjustment: BalanceAdjustmentEntity) {
        balanceAdjustmentDao.insertAdjustment(adjustment)
    }
}
