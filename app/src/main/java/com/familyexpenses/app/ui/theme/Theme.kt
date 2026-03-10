package com.familyexpenses.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF2C2C2C),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF8F8F5),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFF0F0EC),
    onSurfaceVariant = Color(0xFF666666),
    outline = Color(0xFFD8D8D2),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF111111),
    secondary = Color(0xFFE8E8E8),
    onSecondary = Color(0xFF111111),
    background = Color(0xFF111111),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF171717),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF242424),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF3C3C3C),
)

private val AppTypography = Typography()

@Composable
fun FamilyExpensesTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
