package com.familyexpenses.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.domain.usecase.BackupRestoreUseCase
import com.familyexpenses.app.domain.usecase.BackupValidationPreview
import com.familyexpenses.app.domain.usecase.ExportCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingRestoreState(
    val rawJson: String,
    val preview: BackupValidationPreview,
)

data class DataSecurityUiState(
    val currentSummary: BackupValidationPreview? = null,
    val isBusy: Boolean = false,
    val pendingRestore: PendingRestoreState? = null,
    val statusMessage: String? = null,
    val isError: Boolean = false,
)

sealed interface DataSecurityEvent {
    data class LaunchCreateCsv(val suggestedFileName: String, val content: String) : DataSecurityEvent
    data class LaunchCreateBackup(val suggestedFileName: String, val content: String) : DataSecurityEvent
}

@HiltViewModel
class DataSecurityViewModel @Inject constructor(
    private val exportCsvUseCase: ExportCsvUseCase,
    private val backupRestoreUseCase: BackupRestoreUseCase,
) : ViewModel() {

    private val state = MutableStateFlow(DataSecurityUiState())
    val uiState: StateFlow<DataSecurityUiState> = state.asStateFlow()

    private val events = MutableSharedFlow<DataSecurityEvent>()
    val uiEvents = events.asSharedFlow()

    init {
        refreshSummary()
    }

    fun refreshSummary() {
        viewModelScope.launch {
            runCatching { backupRestoreUseCase.getCurrentSummary() }
                .onSuccess { summary ->
                    state.update { it.copy(currentSummary = summary) }
                }
        }
    }

    fun onExportCsvClick() {
        if (state.value.isBusy) return
        viewModelScope.launch {
            state.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            runCatching { exportCsvUseCase.export() }
                .onSuccess { result ->
                    state.update { it.copy(isBusy = false) }
                    events.emit(
                        DataSecurityEvent.LaunchCreateCsv(
                            suggestedFileName = result.fileName,
                            content = result.csvContent,
                        ),
                    )
                }
                .onFailure { err ->
                    state.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = err.message ?: "Error al generar el archivo CSV.",
                            isError = true,
                        )
                    }
                }
        }
    }

    fun onExportBackupClick() {
        if (state.value.isBusy) return
        viewModelScope.launch {
            state.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            runCatching { backupRestoreUseCase.exportBackupJson() }
                .onSuccess { result ->
                    state.update { it.copy(isBusy = false) }
                    events.emit(
                        DataSecurityEvent.LaunchCreateBackup(
                            suggestedFileName = result.fileName,
                            content = result.jsonContent,
                        ),
                    )
                }
                .onFailure { err ->
                    state.update {
                        it.copy(
                            isBusy = false,
                            statusMessage = err.message ?: "Error al generar la copia de seguridad.",
                            isError = true,
                        )
                    }
                }
        }
    }

    fun onBackupContentLoaded(rawJson: String) {
        viewModelScope.launch {
            state.update { it.copy(statusMessage = null, isError = false) }
            runCatching { backupRestoreUseCase.validateBackupJson(rawJson) }
                .onSuccess { preview ->
                    state.update {
                        it.copy(
                            pendingRestore = PendingRestoreState(
                                rawJson = rawJson,
                                preview = preview,
                            ),
                        )
                    }
                }
                .onFailure { err ->
                    state.update {
                        it.copy(
                            pendingRestore = null,
                            statusMessage = "Copia no válida: ${err.message ?: "formato incorrecto."}",
                            isError = true,
                        )
                    }
                }
        }
    }

    fun onConfirmRestore() {
        val pending = state.value.pendingRestore ?: return
        viewModelScope.launch {
            state.update { it.copy(isBusy = true, statusMessage = null, isError = false) }
            runCatching { backupRestoreUseCase.restoreBackup(pending.rawJson) }
                .onSuccess { restoredPreview ->
                    state.update {
                        it.copy(
                            isBusy = false,
                            pendingRestore = null,
                            currentSummary = restoredPreview,
                            statusMessage = "Restauración completada (${restoredPreview.transactionsCount} movimientos, ${restoredPreview.transfersCount} transferencias).",
                            isError = false,
                        )
                    }
                }
                .onFailure { err ->
                    state.update {
                        it.copy(
                            isBusy = false,
                            pendingRestore = null,
                            statusMessage = "No se pudo restaurar: ${err.message ?: "error desconocido."}",
                            isError = true,
                        )
                    }
                }
        }
    }

    fun onDismissRestoreConfirmation() {
        state.update { it.copy(pendingRestore = null) }
    }

    fun onFileWriteResult(success: Boolean, message: String) {
        state.update {
            it.copy(
                statusMessage = message,
                isError = !success,
            )
        }
    }
}
