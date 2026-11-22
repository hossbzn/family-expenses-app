package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.RecurrenceRuleStatus
import com.familyexpenses.app.data.local.dao.RecurrenceRuleDao
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.domain.rule.RecurrenceCalculator
import java.time.ZoneId
import javax.inject.Inject

class ManageRecurrenceRulesUseCase @Inject constructor(
    private val recurrenceRuleDao: RecurrenceRuleDao,
    private val recurrenceCalculator: RecurrenceCalculator,
) {

    suspend fun createRule(rule: RecurrenceRuleEntity, zoneId: ZoneId = ZoneId.systemDefault()): String {
        require(rule.amountMinor > 0L) { "El importe de la regla debe ser mayor a 0." }
        require(rule.endAt == null || rule.endAt >= rule.startAt) { "La fecha fin debe ser igual o posterior a la fecha inicio." }

        val firstOccurrence = recurrenceCalculator.calculateFirstOccurrence(rule, zoneId)
            ?: throw IllegalArgumentException("La configuración de la regla no produce ninguna ejecución válida dentro del rango indicado.")

        val preparedRule = rule.copy(
            nextOccurrenceAt = firstOccurrence,
            status = RecurrenceRuleStatus.ACTIVE,
        )

        recurrenceRuleDao.upsert(preparedRule)
        return preparedRule.id
    }

    suspend fun updateRule(updatedRule: RecurrenceRuleEntity, zoneId: ZoneId = ZoneId.systemDefault()) {
        require(updatedRule.amountMinor > 0L) { "El importe de la regla debe ser mayor a 0." }
        require(updatedRule.endAt == null || updatedRule.endAt >= updatedRule.startAt) { "La fecha fin debe ser igual o posterior a la fecha inicio." }

        val existing = recurrenceRuleDao.getRuleById(updatedRule.id)
            ?: throw IllegalArgumentException("La regla recurrente a actualizar no existe.")

        val now = System.currentTimeMillis()
        val nextOccurrence = recurrenceCalculator.calculateNextOccurrence(updatedRule, now, zoneId)
            ?: (if (updatedRule.endAt != null && now > updatedRule.endAt) existing.nextOccurrenceAt else now)

        val preparedRule = updatedRule.copy(
            nextOccurrenceAt = nextOccurrence,
        )

        recurrenceRuleDao.upsert(preparedRule)
    }

    suspend fun pauseRule(ruleId: String) {
        val existing = recurrenceRuleDao.getRuleById(ruleId)
            ?: throw IllegalArgumentException("La regla recurrente no existe.")
        require(existing.status == RecurrenceRuleStatus.ACTIVE) { "Solo se pueden pausar reglas activas." }

        recurrenceRuleDao.updateStatus(ruleId, RecurrenceRuleStatus.PAUSED)
    }

    suspend fun resumeRule(ruleId: String, zoneId: ZoneId = ZoneId.systemDefault()) {
        val existing = recurrenceRuleDao.getRuleById(ruleId)
            ?: throw IllegalArgumentException("La regla recurrente no existe.")
        require(existing.status == RecurrenceRuleStatus.PAUSED) { "Solo se pueden reactivar reglas pausadas." }

        val now = System.currentTimeMillis()
        val nextOccurrence = recurrenceCalculator.calculateNextOccurrence(existing, now, zoneId)
            ?: existing.nextOccurrenceAt

        recurrenceRuleDao.upsert(
            existing.copy(
                status = RecurrenceRuleStatus.ACTIVE,
                nextOccurrenceAt = nextOccurrence,
            ),
        )
    }

    suspend fun archiveRule(ruleId: String) {
        val existing = recurrenceRuleDao.getRuleById(ruleId)
            ?: throw IllegalArgumentException("La regla recurrente no existe.")

        recurrenceRuleDao.updateStatus(ruleId, RecurrenceRuleStatus.ARCHIVED)
    }

    suspend fun restoreRule(
        ruleId: String,
        newStartAt: Long? = null,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val existing = recurrenceRuleDao.getRuleById(ruleId)
            ?: throw IllegalArgumentException("La regla recurrente no existe.")
        require(existing.status == RecurrenceRuleStatus.ARCHIVED) { "Solo se pueden restaurar reglas archivadas." }

        val now = System.currentTimeMillis()
        val startAt = newStartAt ?: now
        val tempRule = existing.copy(startAt = startAt, status = RecurrenceRuleStatus.ACTIVE)

        val nextOccurrence = recurrenceCalculator.calculateFirstOccurrence(tempRule, zoneId)
            ?: throw IllegalArgumentException("La nueva fecha de inicio no produce ejecuciones válidas.")

        recurrenceRuleDao.upsert(
            tempRule.copy(
                nextOccurrenceAt = nextOccurrence,
            ),
        )
    }
}
