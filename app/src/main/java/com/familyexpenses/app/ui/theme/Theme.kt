package com.familyexpenses.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PetroleumBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PetroleumBlueContainer,
    onPrimaryContainer = PetroleumBlueOnContainer,
    secondary = PetroleumBluePrimary,
    onSecondary = Color.White,
    background = OffWhiteBackground,
    onBackground = TextPrimaryColor,
    surface = PureWhiteSurface,
    onSurface = TextPrimaryColor,
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = TextSecondaryColor,
    outline = OutlineBorder,
)

private val DarkColors = lightColorScheme(
    primary = PetroleumBluePrimary,
    onPrimary = Color.White,
    primaryContainer = PetroleumBlueContainer,
    onPrimaryContainer = PetroleumBlueOnContainer,
    secondary = PetroleumBluePrimary,
    onSecondary = Color.White,
    background = OffWhiteBackground,
    onBackground = TextPrimaryColor,
    surface = PureWhiteSurface,
    onSurface = TextPrimaryColor,
    surfaceVariant = Color(0xFFF1F3F5),
    onSurfaceVariant = TextSecondaryColor,
    outline = OutlineBorder,
)

@Composable
fun FamilyExpensesTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}
