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
    primary = EditorialRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF5B211F),
    onPrimaryContainer = Color(0xFFFFDAD7),
    secondary = Color(0xFF70B9B2),
    onSecondary = Color(0xFF073B38),
    secondaryContainer = Color(0xFF174D49),
    onSecondaryContainer = Color(0xFFBCECE7),
    tertiary = EditorialAmber,
    background = Ink950,
    onBackground = Ink100,
    surface = Ink900,
    onSurface = Ink100,
    surfaceVariant = Ink800,
    onSurfaceVariant = Ink300,
    surfaceContainer = Ink850,
    surfaceContainerHigh = Ink800,
    outline = Ink700,
    outlineVariant = Color(0xFF292E33),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val LightColorScheme = lightColorScheme(
    primary = EditorialRedDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD7),
    onPrimaryContainer = Color(0xFF410004),
    secondary = EditorialTealDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBCECE7),
    onSecondaryContainer = Color(0xFF00201E),
    tertiary = Color(0xFF805600),
    background = Paper,
    onBackground = Color(0xFF191C1E),
    surface = PaperSurface,
    onSurface = Color(0xFF191C1E),
    surfaceVariant = PaperMuted,
    onSurfaceVariant = Color(0xFF5D6368),
    surfaceContainer = PaperSurface,
    surfaceContainerHigh = PaperMuted,
    outline = Color(0xFFD4D7D2),
    outlineVariant = Color(0xFFE2E3DF),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun GundemAITheme(
    darkTheme: Boolean = true,
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
