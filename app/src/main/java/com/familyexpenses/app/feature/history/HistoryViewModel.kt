package com.familyexpenses.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.domain.usecase.GetHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    getHistoryUseCase: GetHistoryUseCase,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = getHistoryUseCase()
        .map { items ->
            HistoryUiState(
                items = items.map { item ->
                    HistoryRowUiModel(
                        id = item.id,
                        title = item.note ?: item.categoryName ?: "Movimiento",
                        subtitle = buildString {
                            append(item.accountName)
                            item.categoryName?.let {
                                append(" · ")
                                append(it)
                            }
                            if (item.paidFromPersonal) {
                                append(" · pagado con personal")
                            }
                        },
                        amountText = item.amountMinor.toCurrencyText(),
                        dateText = item.occurredAt.toDateText(),
                    )
                },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HistoryUiState(),
        )

    private fun Long.toCurrencyText(): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "ES")).apply {
            currency = Currency.getInstance("EUR")
        }
        return formatter.format(-(this / 100.0))
    }

    private fun Long.toDateText(): String = Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .format(DateTimeFormatter.ofPattern("d MMM", Locale("es", "ES")))
}

data class HistoryUiState(
    val items: List<HistoryRowUiModel> = emptyList(),
)

data class HistoryRowUiModel(
    val id: String,
    val title: String,
    val subtitle: String,
    val amountText: String,
    val dateText: String,
)
