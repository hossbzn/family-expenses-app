package com.familyexpenses.app.feature.expense

import org.junit.Assert.assertEquals
import org.junit.Test

class NumericKeypadHandlerTest {

    @Test
    fun `digit replaces initial zero`() {
        val result = NumericKeypadHandler.onDigit("0", '5')
        assertEquals("5", result)
    }

    @Test
    fun `zero does not duplicate initial zero`() {
        val result = NumericKeypadHandler.onDigit("0", '0')
        assertEquals("0", result)
    }

    @Test
    fun `subsequent digits append`() {
        var current = "5"
        current = NumericKeypadHandler.onDigit(current, '4')
        current = NumericKeypadHandler.onDigit(current, '2')
        assertEquals("542", current)
    }

    @Test
    fun `comma appends and is not duplicated`() {
        var current = "12"
        current = NumericKeypadHandler.onComma(current)
        assertEquals("12,", current)

        // Second comma is ignored
        current = NumericKeypadHandler.onComma(current)
        assertEquals("12,", current)
    }

    @Test
    fun `maximum two decimal digits allowed`() {
        var current = "12,"
        current = NumericKeypadHandler.onDigit(current, '5')
        assertEquals("12,5", current)

        current = NumericKeypadHandler.onDigit(current, '0')
        assertEquals("12,50", current)

        // Third decimal digit is rejected
        current = NumericKeypadHandler.onDigit(current, '9')
        assertEquals("12,50", current)
    }

    @Test
    fun `clear resets to zero`() {
        val result = NumericKeypadHandler.onClear()
        assertEquals("0", result)
    }

    @Test
    fun `backspace removes last character and resets to zero when empty`() {
        var current = "12,5"
        current = NumericKeypadHandler.onBackspace(current)
        assertEquals("12,", current)

        current = NumericKeypadHandler.onBackspace(current)
        assertEquals("12", current)

        current = NumericKeypadHandler.onBackspace(current)
        assertEquals("1", current)

        current = NumericKeypadHandler.onBackspace(current)
        assertEquals("0", current)

        current = NumericKeypadHandler.onBackspace(current)
        assertEquals("0", current)
    }

    @Test
    fun `toMinorAmount converts amounts accurately without float rounding issues`() {
        assertEquals(0L, NumericKeypadHandler.toMinorAmount("0"))
        assertEquals(0L, NumericKeypadHandler.toMinorAmount(""))
        assertEquals(8000L, NumericKeypadHandler.toMinorAmount("80"))
        assertEquals(8000L, NumericKeypadHandler.toMinorAmount("80,"))
        assertEquals(8050L, NumericKeypadHandler.toMinorAmount("80,5"))
        assertEquals(8050L, NumericKeypadHandler.toMinorAmount("80,50"))
        assertEquals(99L, NumericKeypadHandler.toMinorAmount("0,99"))
        assertEquals(5L, NumericKeypadHandler.toMinorAmount("0,05"))
        assertEquals(150000L, NumericKeypadHandler.toMinorAmount("1500"))
    }

    @Test
    fun `fromMinorAmount formats minor amounts cleanly without float arithmetic`() {
        assertEquals("0", NumericKeypadHandler.fromMinorAmount(0L))
        assertEquals("80", NumericKeypadHandler.fromMinorAmount(8000L))
        assertEquals("80,50", NumericKeypadHandler.fromMinorAmount(8050L))
        assertEquals("80,05", NumericKeypadHandler.fromMinorAmount(8005L))
        assertEquals("0,50", NumericKeypadHandler.fromMinorAmount(50L))
        assertEquals("0,05", NumericKeypadHandler.fromMinorAmount(5L))
    }
}
