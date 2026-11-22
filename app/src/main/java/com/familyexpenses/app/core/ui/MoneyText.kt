package com.familyexpenses.app.core.ui

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

const val PRIVACY_MASK = "•••• €"

@Composable
fun MoneyText(
    amountMinor: Long,
    isPrivacyModeEnabled: Boolean,
    modifier: Modifier = Modifier,
    showPlusSign: Boolean = false,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val textToDisplay = if (isPrivacyModeEnabled) {
        PRIVACY_MASK
    } else {
        formatCurrencyText(amountMinor, showPlusSign)
    }

    Text(
        text = textToDisplay,
        style = style,
        color = color,
        modifier = modifier,
    )
}

fun formatCurrencyText(minorAmount: Long, showPlusSign: Boolean = false): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("es", "ES")).apply {
        currency = Currency.getInstance("EUR")
    }
    val formatted = formatter.format(minorAmount / 100.0)
    return if (showPlusSign && minorAmount > 0L) "+$formatted" else formatted
}
