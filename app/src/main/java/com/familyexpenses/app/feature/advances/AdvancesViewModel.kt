package com.familyexpenses.app.feature.advances

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.familyexpenses.app.core.model.AccountType
import com.familyexpenses.app.data.repository.AccountRepository
import com.familyexpenses.app.data.repository.ExpenseRepository
import com.familyexpenses.app.data.repository.HistoryRepository
import com.familyexpenses.app.data.repository.TransferRepository
import com.familyexpenses.app.domain.model.HistoryItem
import com.familyexpenses.app.domain.model.MovementKind
import com.familyexpenses.app.domain.usecase.ComputeNetDebtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

enum class AdvancesHistoryFilter {
    ALL,
    ADVANCES,
    REIMBURSEMENTS,
}

@HiltViewModel
class AdvancesViewModel @Inject constructor(
    accountRepository: AccountRepository,
    expenseRepository: ExpenseRepository,
    transferRepository: TransferRepository,
    historyRepository: HistoryRepository,
    private val computeNetDebtUseCase: ComputeNetDebtUseCase,
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(AdvancesHistoryFilter.ALL)

    val uiState: StateFlow<AdvancesUiState> = combine(
        accountRepository.observeAccounts(),
        expenseRepository.observeTransactions(),
        transferRepository.observeTransfers(),
        historyRepository.observeHistory(),
        selectedFilter,
    ) { accounts, transactions, transfers, historyItems, filter ->
        val netDebtMinor = computeNetDebtUseCase.compute(
            accounts = accounts,
            transactions = transactions,
            transfers = transfers,
        )

        val personalAccount = accounts.firstOrNull { it.type == AccountType.PERSONAL }
        val familyAccount = accounts.firstOrNull { it.type == AccountType.FAMILY }
        val debtorAccountId = when {
            netDebtMinor > 0L -> familyAccount?.id
            netDebtMinor < 0L -> personalAccount?.id
            else -> null
        }

        val allRelevantItems = historyItems.filter { item ->
            (item.kind == MovementKind.EXPENSE && item.isAdvance) ||
                item.kind == MovementKind.REIMBURSEMENT
        }

        val advancesCount = allRelevantItems.count { it.kind == MovementKind.EXPENSE }
        val reimbursementsCount = allRelevantItems.count { it.kind == MovementKind.REIMBURSEMENT }

        val filteredItems = when (filter) {
            AdvancesHistoryFilter.ALL -> allRelevantItems
            AdvancesHistoryFilter.ADVANCES -> allRelevantItems.filter { it.kind == MovementKind.EXPENSE }
            AdvancesHistoryFilter.REIMBURSEMENTS -> allRelevantItems.filter { it.kind == MovementKind.REIMBURSEMENT }
        }

        AdvancesUiState(
            netDebtMinor = netDebtMinor,
            debtorAccountId = debtorAccountId,
            selectedFilter = filter,
            advancesCount = advancesCount,
            reimbursementsCount = reimbursementsCount,
            items = filteredItems.map { it.toAdvanceRowUiModel() },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AdvancesUiState(),
    )

    fun onFilterSelected(filter: AdvancesHistoryFilter) {
        selectedFilter.update { filter }
    }

    private fun HistoryItem.toAdvanceRowUiModel(): AdvanceHistoryRowUiModel {
        val isReimbursement = kind == MovementKind.REIMBURSEMENT
        val dateText = Instant.ofEpochMilli(occurredAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "ES")))

        val title = if (isReimbursement) {
            note?.takeIf { it.isNotBlank() } ?: "Devolución de adelanto"
        } else {
            note?.takeIf { it.isNotBlank() } ?: categoryName ?: "Gasto adelantado"
        }

        val subtitle = if (isReimbursement) {
            "Devolución · $payingAccountName → $responsibleAccountName · $dateText"
        } else {
            val catPart = categoryName?.let { "$it · " } ?: ""
            "Adelanto · $catPart$payingAccountName → $responsibleAccountName · $dateText"
        }

        return AdvanceHistoryRowUiModel(
            id = id,
            kind = kind,
            isReimbursement = isReimbursement,
            title = title,
            subtitle = subtitle,
            amountText = formatCurrency(amountMinor),
        )
    }

    private fun formatCurrency(minorAmount: Long): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("es", "ES")).apply {
            currency = Currency.getInstance("EUR")
        }
        return formatter.format(minorAmount / 100.0)
    }
}

data class AdvancesUiState(
    val netDebtMinor: Long = 0L,
    val debtorAccountId: String? = null,
    val selectedFilter: AdvancesHistoryFilter = AdvancesHistoryFilter.ALL,
    val advancesCount: Int = 0,
    val reimbursementsCount: Int = 0,
    val items: List<AdvanceHistoryRowUiModel> = emptyList(),
)

data class AdvanceHistoryRowUiModel(
    val id: String,
    val kind: MovementKind,
    val isReimbursement: Boolean,
    val title: String,
    val subtitle: String,
    val amountText: String,
)
