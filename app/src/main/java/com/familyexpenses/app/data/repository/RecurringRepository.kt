package com.familyexpenses.app.data.repository

import com.familyexpenses.app.data.local.dao.RecurrenceRuleDao
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.domain.model.RecurringRuleDraft
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RecurringRepository @Inject constructor(
    private val recurrenceRuleDao: RecurrenceRuleDao,
) {

    fun observeRules(): Flow<List<RecurrenceRuleEntity>> = recurrenceRuleDao.observeRules()

    suspend fun getActiveRules(): List<RecurrenceRuleEntity> = recurrenceRuleDao.getActiveRules()

    suspend fun createMonthlyRule(draft: RecurringRuleDraft, firstOccurrenceAt: Long): String {
        val ruleId = UUID.randomUUID().toString()
        recurrenceRuleDao.upsert(
            RecurrenceRuleEntity(
                id = ruleId,
                type = draft.type,
                accountId = draft.accountId,
                categoryId = draft.categoryId,
                amountMinor = draft.amountMinor,
                paidFromPersonal = draft.paidFromPersonal,
                note = draft.note?.takeIf { it.isNotBlank() },
                startAt = draft.startAt,
                endAt = draft.endAt,
                nextOccurrenceAt = firstOccurrenceAt,
                isActive = true,
                createdAt = System.currentTimeMillis(),
            ),
        )
        return ruleId
    }

    suspend fun updateNextOccurrence(ruleId: String, nextOccurrenceAt: Long) {
        recurrenceRuleDao.updateNextOccurrence(ruleId, nextOccurrenceAt)
    }

    suspend fun setActive(ruleId: String, isActive: Boolean) {
        recurrenceRuleDao.updateActiveState(ruleId, isActive)
    }
}
