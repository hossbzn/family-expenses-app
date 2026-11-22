package com.familyexpenses.app.domain.usecase

import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.core.model.CategoryType
import com.familyexpenses.app.core.model.RecurrenceFrequency
import com.familyexpenses.app.core.model.TransactionSource
import com.familyexpenses.app.core.model.TransactionType
import com.familyexpenses.app.core.model.TransferType
import com.familyexpenses.app.core.util.JsonArray
import com.familyexpenses.app.core.util.JsonBoolean
import com.familyexpenses.app.core.util.JsonNull
import com.familyexpenses.app.core.util.JsonNumber
import com.familyexpenses.app.core.util.JsonObject
import com.familyexpenses.app.core.util.JsonString
import com.familyexpenses.app.core.util.JsonValue
import com.familyexpenses.app.core.util.PortableJson
import com.familyexpenses.app.data.local.entity.AccountEntity
import com.familyexpenses.app.data.local.entity.BalanceAdjustmentEntity
import com.familyexpenses.app.data.local.entity.CategoryEntity
import com.familyexpenses.app.data.local.entity.RecurrenceRuleEntity
import com.familyexpenses.app.data.local.entity.TransactionEntity
import com.familyexpenses.app.data.local.entity.TransferEntity
import com.familyexpenses.app.data.repository.BackupRepository
import com.familyexpenses.app.domain.model.BackupPreferencesSnapshot
import com.familyexpenses.app.domain.model.PortableBackupSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class BackupValidationPreview(
    val schemaVersion: Int,
    val exportedAt: Long,
    val accountsCount: Int,
    val categoriesCount: Int,
    val transactionsCount: Int,
    val transfersCount: Int,
    val adjustmentsCount: Int,
    val recurrenceRulesCount: Int,
)

data class BackupExportResult(
    val jsonContent: String,
    val fileName: String,
    val preview: BackupValidationPreview,
)

class BackupRestoreUseCase @Inject constructor(
    private val backupRepository: BackupRepository,
) {

    suspend fun getCurrentSummary(): BackupValidationPreview {
        val snapshot = backupRepository.readSnapshot()
        return snapshot.toPreview()
    }

    suspend fun exportBackupJson(
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): BackupExportResult {
        val snapshot = backupRepository.readSnapshot()
        val json = encodeSnapshot(snapshot)
        // Autovalidación antes de entregar el archivo al usuario
        decodeAndValidate(json)
        val datePart = DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")
            .withZone(zoneId)
            .format(Instant.ofEpochMilli(snapshot.exportedAt))

        return BackupExportResult(
            jsonContent = json,
            fileName = "family-expenses-v6-backup-$datePart.json",
            preview = snapshot.toPreview(),
        )
    }

    fun validateBackupJson(jsonText: String): BackupValidationPreview {
        val snapshot = decodeAndValidate(jsonText)
        return snapshot.toPreview()
    }

    suspend fun restoreBackup(jsonText: String): BackupValidationPreview {
        val snapshot = decodeAndValidate(jsonText)
        backupRepository.replaceAll(snapshot)
        return snapshot.toPreview()
    }

    internal fun encodeSnapshot(snapshot: PortableBackupSnapshot): String {
        val root = JsonObject(
            linkedMapOf(
                "schemaVersion" to JsonNumber(snapshot.schemaVersion.toLong()),
                "appName" to JsonString(snapshot.appName),
                "exportedAt" to JsonNumber(snapshot.exportedAt),
                "accounts" to JsonArray(
                    snapshot.accounts.map { acc ->
                        JsonObject(
                            linkedMapOf(
                                "id" to JsonString(acc.id),
                                "name" to JsonString(acc.name),
                                "type" to JsonString(acc.type.name),
                                "currencyCode" to JsonString(acc.currencyCode),
                                "initialBalanceMinor" to JsonNumber(acc.initialBalanceMinor),
                                "isArchived" to JsonBoolean(acc.isArchived),
                                "createdAt" to JsonNumber(acc.createdAt),
                            ),
                        )
                    },
                ),
                "categories" to JsonArray(
                    snapshot.categories.map { cat ->
                        JsonObject(
                            linkedMapOf(
                                "id" to JsonString(cat.id),
                                "name" to JsonString(cat.name),
                                "icon" to JsonString(cat.icon),
                                "type" to JsonString(cat.type.name),
                                "orderIndex" to JsonNumber(cat.orderIndex.toLong()),
                                "isFavorite" to JsonBoolean(cat.isFavorite),
                                "favoritePosition" to (cat.favoritePosition?.let { JsonNumber(it.toLong()) } ?: JsonNull),
                                "isArchived" to JsonBoolean(cat.isArchived),
                                "createdAt" to JsonNumber(cat.createdAt),
                            ),
                        )
                    },
                ),
                "transactions" to JsonArray(
                    snapshot.transactions.map { tx ->
                        JsonObject(
                            linkedMapOf(
                                "id" to JsonString(tx.id),
                                "type" to JsonString(tx.type.name),
                                "amountMinor" to JsonNumber(tx.amountMinor),
                                "payingAccountId" to JsonString(tx.payingAccountId),
                                "responsibleAccountId" to JsonString(tx.responsibleAccountId),
                                "categoryId" to JsonString(tx.categoryId),
                                "occurredAt" to JsonNumber(tx.occurredAt),
                                "note" to nullableJsonString(tx.note),
                                "source" to JsonString(tx.source.name),
                                "recurrenceRuleId" to nullableJsonString(tx.recurrenceRuleId),
                                "createdAt" to JsonNumber(tx.createdAt),
                            ),
                        )
                    },
                ),
                "transfers" to JsonArray(
                    snapshot.transfers.map { tr ->
                        JsonObject(
                            linkedMapOf(
                                "id" to JsonString(tr.id),
                                "type" to JsonString(tr.type.name),
                                "sourceAccountId" to JsonString(tr.sourceAccountId),
                                "destinationAccountId" to JsonString(tr.destinationAccountId),
                                "amountMinor" to JsonNumber(tr.amountMinor),
                                "occurredAt" to JsonNumber(tr.occurredAt),
                                "note" to nullableJsonString(tr.note),
                                "createdAt" to JsonNumber(tr.createdAt),
                            ),
                        )
                    },
                ),
                "balanceAdjustments" to JsonArray(
                    snapshot.balanceAdjustments.map { adj ->
                        JsonObject(
                            linkedMapOf(
                                "id" to JsonString(adj.id),
                                "accountId" to JsonString(adj.accountId),
                                "amountDeltaMinor" to JsonNumber(adj.amountDeltaMinor),
                                "reason" to JsonString(adj.reason),
                                "occurredAt" to JsonNumber(adj.occurredAt),
                                "createdAt" to JsonNumber(adj.createdAt),
                            ),
                        )
                    },
                ),
                "recurrenceRules" to JsonArray(
                    snapshot.recurrenceRules.map { rule ->
                        JsonObject(
                            linkedMapOf(
                                "id" to JsonString(rule.id),
                                "type" to JsonString(rule.type.name),
                                "payingAccountId" to JsonString(rule.payingAccountId),
                                "responsibleAccountId" to JsonString(rule.responsibleAccountId),
                                "categoryId" to JsonString(rule.categoryId),
                                "amountMinor" to JsonNumber(rule.amountMinor),
                                "note" to nullableJsonString(rule.note),
                                "frequency" to JsonString(rule.frequency.name),
                                "intervalCount" to (rule.intervalCount?.let { JsonNumber(it.toLong()) } ?: JsonNull),
                                "dayOfWeek" to (rule.dayOfWeek?.let { JsonNumber(it.toLong()) } ?: JsonNull),
                                "dayOfMonth" to (rule.dayOfMonth?.let { JsonNumber(it.toLong()) } ?: JsonNull),
                                "monthOfYear" to (rule.monthOfYear?.let { JsonNumber(it.toLong()) } ?: JsonNull),
                                "mode" to JsonString(rule.mode.name),
                                "status" to JsonString(rule.status.name),
                                "startAt" to JsonNumber(rule.startAt),
                                "endAt" to (rule.endAt?.let { JsonNumber(it) } ?: JsonNull),
                                "nextOccurrenceAt" to JsonNumber(rule.nextOccurrenceAt),
                                "createdAt" to JsonNumber(rule.createdAt),
                            ),
                        )
                    },
                ),
                "preferences" to JsonObject(
                    linkedMapOf(
                        "lastExpensePayingAccountId" to nullableJsonString(snapshot.preferences.lastExpensePayingAccountId),
                        "lastExpenseResponsibleAccountId" to nullableJsonString(snapshot.preferences.lastExpenseResponsibleAccountId),
                        "lastExpenseCategoryId" to nullableJsonString(snapshot.preferences.lastExpenseCategoryId),
                        "lastIncomeAccountId" to nullableJsonString(snapshot.preferences.lastIncomeAccountId),
                        "lastIncomeCategoryId" to nullableJsonString(snapshot.preferences.lastIncomeCategoryId),
                        "accountIcons" to JsonObject(
                            snapshot.preferences.accountIcons.mapValues { (_, v) -> JsonString(v) },
                        ),
                    ),
                ),
            ),
        )
        return PortableJson.stringify(root)
    }

    internal fun decodeAndValidate(jsonText: String): PortableBackupSnapshot {
        require(jsonText.isNotBlank()) { "El archivo de copia de seguridad está vacío." }
        val root = PortableJson.parseObject(jsonText)

        val schemaVersion = root.getInt("schemaVersion")
        require(schemaVersion == PortableBackupSnapshot.CURRENT_SCHEMA_VERSION) {
            "Versión de copia no compatible ($schemaVersion). Se esperaba versión ${PortableBackupSnapshot.CURRENT_SCHEMA_VERSION}."
        }
        val appName = root.getString("appName")
        val exportedAt = root.getLong("exportedAt")

        val accounts = root.getArray("accounts").items.map { elem ->
            val obj = elem as? JsonObject ?: throw IllegalArgumentException("Cuenta inválida en copia.")
            AccountEntity(
                id = obj.getString("id"),
                name = obj.getString("name"),
                type = enumValue<AccountType>(obj.getString("type"), "tipo de cuenta"),
                currencyCode = obj.getString("currencyCode"),
                initialBalanceMinor = obj.getLong("initialBalanceMinor"),
                isArchived = obj.getBoolean("isArchived"),
                createdAt = obj.getLong("createdAt"),
            )
        }

        val categories = root.getArray("categories").items.map { elem ->
            val obj = elem as? JsonObject ?: throw IllegalArgumentException("Categoría inválida en copia.")
            CategoryEntity(
                id = obj.getString("id"),
                name = obj.getString("name"),
                icon = obj.getString("icon"),
                type = enumValue<CategoryType>(obj.getString("type"), "tipo de categoría"),
                orderIndex = obj.getInt("orderIndex"),
                isFavorite = obj.getBoolean("isFavorite"),
                favoritePosition = obj.getNullableInt("favoritePosition"),
                isArchived = obj.getBoolean("isArchived"),
                createdAt = obj.getLong("createdAt"),
            )
        }

        val transactions = root.getArray("transactions").items.map { elem ->
            val obj = elem as? JsonObject ?: throw IllegalArgumentException("Transacción inválida en copia.")
            TransactionEntity(
                id = obj.getString("id"),
                type = enumValue<TransactionType>(obj.getString("type"), "tipo de transacción"),
                amountMinor = obj.getLong("amountMinor"),
                payingAccountId = obj.getString("payingAccountId"),
                responsibleAccountId = obj.getString("responsibleAccountId"),
                categoryId = obj.getString("categoryId"),
                occurredAt = obj.getLong("occurredAt"),
                note = obj.getNullableString("note"),
                source = enumValue<TransactionSource>(obj.getString("source"), "origen de transacción"),
                recurrenceRuleId = obj.getNullableString("recurrenceRuleId"),
                createdAt = obj.getLong("createdAt"),
            )
        }

        val transfers = root.getArray("transfers").items.map { elem ->
            val obj = elem as? JsonObject ?: throw IllegalArgumentException("Transferencia inválida en copia.")
            TransferEntity(
                id = obj.getString("id"),
                type = enumValue<TransferType>(obj.getString("type"), "tipo de transferencia"),
                sourceAccountId = obj.getString("sourceAccountId"),
                destinationAccountId = obj.getString("destinationAccountId"),
                amountMinor = obj.getLong("amountMinor"),
                occurredAt = obj.getLong("occurredAt"),
                note = obj.getNullableString("note"),
                createdAt = obj.getLong("createdAt"),
            )
        }

        val adjustments = root.getArray("balanceAdjustments").items.map { elem ->
            val obj = elem as? JsonObject ?: throw IllegalArgumentException("Ajuste inválido en copia.")
            BalanceAdjustmentEntity(
                id = obj.getString("id"),
                accountId = obj.getString("accountId"),
                amountDeltaMinor = obj.getLong("amountDeltaMinor"),
                reason = obj.getString("reason"),
                occurredAt = obj.getLong("occurredAt"),
                createdAt = obj.getLong("createdAt"),
            )
        }

        val rules = root.getArray("recurrenceRules").items.map { elem ->
            val obj = elem as? JsonObject ?: throw IllegalArgumentException("Regla recurrente inválida en copia.")
            val frequencyStr = obj.entries["frequency"]?.let { (it as? JsonString)?.value } ?: "MONTHLY"
            val statusStr = obj.entries["status"]?.let { (it as? JsonString)?.value }
            val legacyIsActive = obj.entries["isActive"]?.let { (it as? JsonBoolean)?.value }
            
            val status = when {
                statusStr != null -> enumValue<com.familyexpenses.app.core.model.RecurrenceRuleStatus>(statusStr, "estado")
                legacyIsActive == false -> com.familyexpenses.app.core.model.RecurrenceRuleStatus.PAUSED
                else -> com.familyexpenses.app.core.model.RecurrenceRuleStatus.ACTIVE
            }

            val modeStr = obj.entries["mode"]?.let { (it as? JsonString)?.value }
            val mode = if (modeStr != null) {
                enumValue<com.familyexpenses.app.core.model.RecurrenceExecutionMode>(modeStr, "modo")
            } else {
                com.familyexpenses.app.core.model.RecurrenceExecutionMode.AUTOMATIC
            }

            RecurrenceRuleEntity(
                id = obj.getString("id"),
                type = enumValue<TransactionType>(obj.getString("type"), "tipo de regla recurrente"),
                payingAccountId = obj.getString("payingAccountId"),
                responsibleAccountId = obj.getString("responsibleAccountId"),
                categoryId = obj.getString("categoryId"),
                amountMinor = obj.getLong("amountMinor"),
                note = obj.getNullableString("note"),
                frequency = enumValue<RecurrenceFrequency>(frequencyStr, "frecuencia"),
                intervalCount = obj.entries["intervalCount"]?.let { (it as? JsonNumber)?.value?.toInt() },
                dayOfWeek = obj.entries["dayOfWeek"]?.let { (it as? JsonNumber)?.value?.toInt() },
                dayOfMonth = obj.entries["dayOfMonth"]?.let { (it as? JsonNumber)?.value?.toInt() } ?: 1,
                monthOfYear = obj.entries["monthOfYear"]?.let { (it as? JsonNumber)?.value?.toInt() },
                mode = mode,
                status = status,
                startAt = obj.getLong("startAt"),
                endAt = obj.getNullableLong("endAt"),
                nextOccurrenceAt = obj.getLong("nextOccurrenceAt"),
                createdAt = obj.getLong("createdAt"),
            )
        }

        val prefsObj = root.entries["preferences"] as? JsonObject
        val preferences = if (prefsObj != null) {
            val iconsObj = prefsObj.entries["accountIcons"] as? JsonObject
            val iconsMap = iconsObj?.entries?.mapNotNull { (k, v) ->
                (v as? JsonString)?.value?.let { k to it }
            }?.toMap().orEmpty()
            BackupPreferencesSnapshot(
                lastExpensePayingAccountId = prefsObj.getNullableString("lastExpensePayingAccountId"),
                lastExpenseResponsibleAccountId = prefsObj.getNullableString("lastExpenseResponsibleAccountId"),
                lastExpenseCategoryId = prefsObj.getNullableString("lastExpenseCategoryId"),
                lastIncomeAccountId = prefsObj.getNullableString("lastIncomeAccountId"),
                lastIncomeCategoryId = prefsObj.getNullableString("lastIncomeCategoryId"),
                accountIcons = iconsMap,
            )
        } else {
            BackupPreferencesSnapshot()
        }

        val snapshot = PortableBackupSnapshot(
            schemaVersion = schemaVersion,
            appName = appName,
            exportedAt = exportedAt,
            accounts = accounts,
            categories = categories,
            transactions = transactions,
            transfers = transfers,
            balanceAdjustments = adjustments,
            recurrenceRules = rules,
            preferences = preferences,
        )

        validateSnapshotIntegrity(snapshot)
        return snapshot
    }

    private fun validateSnapshotIntegrity(snapshot: PortableBackupSnapshot) {
        require(snapshot.accounts.isNotEmpty()) { "La copia debe incluir al menos una cuenta." }
        val accountIds = snapshot.accounts.map { it.id }.toSet()
        require(accountIds.size == snapshot.accounts.size) { "IDs de cuentas duplicados en la copia." }
        require(snapshot.accounts.all { it.id.isNotBlank() && it.name.isNotBlank() }) {
            "Las cuentas deben tener identificador y nombre."
        }

        val categoryIds = snapshot.categories.map { it.id }.toSet()
        require(categoryIds.size == snapshot.categories.size) { "IDs de categorías duplicados en la copia." }
        require(snapshot.categories.all { it.id.isNotBlank() && it.name.isNotBlank() }) {
            "Las categorías deben tener identificador y nombre."
        }
        val activeFavoriteCount = snapshot.categories.count {
            it.type == CategoryType.EXPENSE && it.isFavorite && !it.isArchived
        }
        require(activeFavoriteCount <= ManageCategoriesUseCase.MAX_FAVORITE_EXPENSE_CATEGORIES) {
            "La copia supera el límite de 7 categorías favoritas."
        }

        val txIds = snapshot.transactions.map { it.id }.toSet()
        require(txIds.size == snapshot.transactions.size) { "IDs de transacciones duplicados en la copia." }
        snapshot.transactions.forEach { tx ->
            require(tx.amountMinor > 0L) { "Transacción ${tx.id} tiene importe <= 0." }
            require(tx.payingAccountId in accountIds) { "Cuenta pagadora inexistente en transacción ${tx.id}." }
            require(tx.responsibleAccountId in accountIds) { "Cuenta responsable inexistente en transacción ${tx.id}." }
            require(tx.categoryId in categoryIds) { "Categoría inexistente en transacción ${tx.id}." }
        }

        val trIds = snapshot.transfers.map { it.id }.toSet()
        require(trIds.size == snapshot.transfers.size) { "IDs de transferencias duplicados en la copia." }
        snapshot.transfers.forEach { tr ->
            require(tr.amountMinor > 0L) { "Transferencia ${tr.id} tiene importe <= 0." }
            require(tr.sourceAccountId != tr.destinationAccountId) { "Transferencia ${tr.id} tiene origen = destino." }
            require(tr.sourceAccountId in accountIds) { "Cuenta origen inexistente en transferencia ${tr.id}." }
            require(tr.destinationAccountId in accountIds) { "Cuenta destino inexistente en transferencia ${tr.id}." }
        }

        val adjIds = snapshot.balanceAdjustments.map { it.id }.toSet()
        require(adjIds.size == snapshot.balanceAdjustments.size) { "IDs de ajustes duplicados en la copia." }
        snapshot.balanceAdjustments.forEach { adj ->
            require(adj.accountId in accountIds) { "Cuenta inexistente en ajuste ${adj.id}." }
            require(adj.reason.isNotBlank()) { "Ajuste ${adj.id} sin motivo." }
        }

        val ruleIds = snapshot.recurrenceRules.map { it.id }.toSet()
        require(ruleIds.size == snapshot.recurrenceRules.size) { "IDs de reglas duplicados en la copia." }
        snapshot.recurrenceRules.forEach { rule ->
            require(rule.amountMinor > 0L) { "Regla ${rule.id} tiene importe <= 0." }
            require(rule.payingAccountId in accountIds) { "Cuenta pagadora inexistente en regla ${rule.id}." }
            require(rule.responsibleAccountId in accountIds) { "Cuenta responsable inexistente en regla ${rule.id}." }
            require(rule.categoryId in categoryIds) { "Categoría inexistente en regla ${rule.id}." }
        }
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String, label: String): T =
        enumValues<T>().firstOrNull { it.name == raw }
            ?: throw IllegalArgumentException("Valor inválido '$raw' para $label.")

    private fun nullableJsonString(value: String?): JsonValue =
        if (value == null) JsonNull else JsonString(value)

    private fun PortableBackupSnapshot.toPreview(): BackupValidationPreview =
        BackupValidationPreview(
            schemaVersion = schemaVersion,
            exportedAt = exportedAt,
            accountsCount = accounts.size,
            categoriesCount = categories.size,
            transactionsCount = transactions.size,
            transfersCount = transfers.size,
            adjustmentsCount = balanceAdjustments.size,
            recurrenceRulesCount = recurrenceRules.size,
        )
}
