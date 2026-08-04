package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BrandTealLight,
    onPrimary = BrandNavyDark,
    primaryContainer = Color(0xFF123D3B),
    onPrimaryContainer = Color(0xFFD8F5F1),
    secondary = Color(0xFF9AB7CA),
    onSecondary = BrandNavyDark,
    secondaryContainer = Color(0xFF243B4A),
    onSecondaryContainer = Color(0xFFDDEAF1),
    tertiary = WarningSoft,
    background = BrandNavyDark,
    onBackground = Color(0xFFF2F5F6),
    surface = BrandNavySurface,
    onSurface = Color(0xFFF2F5F6),
    surfaceVariant = BrandNavySurfaceHigh,
    onSurfaceVariant = Color(0xFFAAB5BB),
    surfaceContainer = BrandNavySurface,
    surfaceContainerHigh = BrandNavySurfaceHigh,
    outline = Color(0xFF466070),
    outlineVariant = Color(0xFF29404D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = BrandTeal,
    onPrimary = Color.White,
    primaryContainer = BrandTealTint,
    onPrimaryContainer = Color(0xFF073D38),
    secondary = BrandNavy,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE5EDF1),
    onSecondaryContainer = BrandNavy,
    tertiary = WarningSoft,
    background = PaperBackground,
    onBackground = InkPrimary,
    surface = PaperSurface,
    onSurface = InkPrimary,
    surfaceVariant = BrandTealTint,
    onSurfaceVariant = InkSecondary,
    surfaceContainer = Color(0xFFF0F3F3),
    surfaceContainerHigh = Color(0xFFE8EEEE),
    outline = Color(0xFF9AA8AE),
    outlineVariant = Color(0xFFDCE2E5),
    error = ErrorSoft,
    onError = Color.White
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(GundemDesignTokens.smallRadius),
    small = RoundedCornerShape(GundemDesignTokens.smallRadius),
    medium = RoundedCornerShape(GundemDesignTokens.controlRadius),
    large = RoundedCornerShape(GundemDesignTokens.cardRadius),
    extraLarge = RoundedCornerShape(GundemDesignTokens.panelRadius)
)

@Composable
fun GundemAITheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
