package com.familyexpenses.app.feature.expense

object NumericKeypadHandler {

    fun onDigit(current: String, digit: Char): String {
        require(digit in '0'..'9')
        if (current == "0") {
            return if (digit == '0') "0" else digit.toString()
        }
        val commaIndex = current.indexOfAny(charArrayOf(',', '.'))
        if (commaIndex != -1) {
            val decimalsCount = current.length - 1 - commaIndex
            if (decimalsCount >= 2) {
                return current
            }
        }
        return current + digit
    }

    fun onComma(current: String): String {
        if (current.contains(',') || current.contains('.')) {
            return current
        }
        return if (current.isEmpty()) "0," else "$current,"
    }

    fun onClear(): String = "0"

    fun onBackspace(current: String): String {
        if (current.isEmpty() || current == "0") return "0"
        val trimmed = current.dropLast(1)
        return if (trimmed.isEmpty()) "0" else trimmed
    }

    fun toMinorAmount(input: String): Long {
        val sanitized = input.replace('.', ',').trim()
        if (sanitized.isEmpty() || sanitized == "0") return 0L
        val parts = sanitized.split(',')
        val whole = parts.getOrNull(0)?.toLongOrNull() ?: 0L
        val decimalPart = parts.getOrNull(1) ?: ""
        val decimal = when (decimalPart.length) {
            0 -> 0L
            1 -> (decimalPart + "0").toLongOrNull() ?: 0L
            else -> decimalPart.take(2).toLongOrNull() ?: 0L
        }
        return whole * 100L + decimal
    }

    fun fromMinorAmount(minorAmount: Long): String {
        val absMinor = kotlin.math.abs(minorAmount)
        if (absMinor == 0L) return "0"
        val whole = absMinor / 100L
        val cents = absMinor % 100L
        return if (cents == 0L) {
            whole.toString()
        } else {
            "$whole,${cents.toString().padStart(2, '0')}"
        }
    }

    fun formatDisplay(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return "0 €"
        return "$trimmed €"
    }
}
