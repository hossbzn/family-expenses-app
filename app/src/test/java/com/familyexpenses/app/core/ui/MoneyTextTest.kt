package com.familyexpenses.app.core.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTextTest {

    @Test
    fun `formatCurrencyText adds plus sign only for positive amounts when requested`() {
        val positive = formatCurrencyText(1250L, showPlusSign = true)
        assertTrue(positive.startsWith("+"))
        assertTrue(positive.contains("12,50"))
        assertTrue(positive.contains("€"))

        val zero = formatCurrencyText(0L, showPlusSign = true)
        assertFalse(zero.startsWith("+"))
        assertTrue(zero.contains("0,00"))
    }

    @Test
    fun `formatCurrencyText adds typographic minus sign only for positive amounts when requested`() {
        val positive = formatCurrencyText(1250L, showMinusSign = true)
        assertTrue(positive.startsWith("−"))
        assertTrue(positive.contains("12,50"))
        assertTrue(positive.contains("€"))

        val zero = formatCurrencyText(0L, showMinusSign = true)
        assertFalse(zero.startsWith("−"))
        assertTrue(zero.contains("0,00"))
    }

    @Test
    fun `formatCurrencyText defaults to no explicit sign`() {
        val formatted = formatCurrencyText(1250L)
        assertFalse(formatted.startsWith("+"))
        assertFalse(formatted.startsWith("−"))
        assertTrue(formatted.contains("12,50"))
    }

    @Test
    fun `privacy mask matches V6 specification`() {
        assertEquals("•••• €", PRIVACY_MASK)
    }
}
