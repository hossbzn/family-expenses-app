package com.familyexpenses.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min

class ThemeTokensTest {

    @Test
    fun `app typography meets minimum readable sizes for V6-22`() {
        assertTrue(AppTypography.labelLarge.fontSize >= 15.sp)
        assertTrue(AppTypography.labelMedium.fontSize >= 13.sp)
        assertTrue(AppTypography.bodyMedium.fontSize >= 15.sp)
        assertTrue(AppTypography.bodySmall.fontSize >= 13.sp)
        assertTrue(AppTypography.titleMedium.fontSize >= 17.sp)
    }

    @Test
    fun `semantic container pairs meet WCAG AA contrast ratio`() {
        assertTrue(contrastRatio(OnIncomeContainer, IncomeContainer) >= 4.5f)
        assertTrue(contrastRatio(OnExpenseContainer, ExpenseContainer) >= 4.5f)
        assertTrue(contrastRatio(OnIncomeContainer, PureWhiteSurface) >= 4.5f)
        assertTrue(contrastRatio(OnExpenseContainer, PureWhiteSurface) >= 4.5f)
        assertTrue(contrastRatio(PetroleumBluePrimary, PureWhiteSurface) >= 4.5f)
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val l1 = foreground.luminance()
        val l2 = background.luminance()
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
