package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = AnchorPrimary,
    onPrimary = AnchorOnPrimary,
    primaryContainer = AnchorPrimaryFixed,
    onPrimaryContainer = AnchorPrimary,
    secondary = AnchorSecondary,
    onSecondary = AnchorOnSecondary,
    secondaryContainer = AnchorSecondaryContainer,
    onSecondaryContainer = AnchorOnSecondaryContainer,
    tertiary = AnchorTertiary,
    onTertiary = AnchorOnTertiary,
    tertiaryContainer = AnchorTertiaryFixed,
    onTertiaryContainer = AnchorTertiary,
    error = AnchorError,
    onError = AnchorOnError,
    errorContainer = AnchorErrorContainer,
    onErrorContainer = AnchorOnErrorContainer,
    background = AnchorBackground,
    onBackground = AnchorOnBackground,
    surface = AnchorSurfaceLowest,
    onSurface = AnchorOnSurface,
    surfaceVariant = AnchorSurfaceLow,
    onSurfaceVariant = AnchorOnSurfaceVariant,
    surfaceContainerLowest = AnchorSurfaceLowest,
    surfaceContainerLow = AnchorSurfaceLow,
    surfaceContainer = AnchorSurfaceContainer,
    surfaceContainerHigh = AnchorSurfaceHigh,
    surfaceContainerHighest = AnchorSurfaceHighest,
    outline = AnchorOutline,
    outlineVariant = AnchorOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = AnchorPrimaryFixedDim,
    onPrimary = Color(0xFF002A78),
    primaryContainer = AnchorPrimaryContainer,
    onPrimaryContainer = AnchorPrimaryFixed,
    secondary = AnchorSecondaryFixed,
    onSecondary = Color(0xFF00382D),
    secondaryContainer = AnchorSecondary,
    onSecondaryContainer = AnchorSecondaryFixed,
    tertiary = AnchorTertiaryFixedDim,
    onTertiary = Color(0xFF432C00),
    tertiaryContainer = AnchorTertiaryContainer,
    onTertiaryContainer = AnchorTertiaryFixed,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0D141F),
    onBackground = Color(0xFFE1E8F5),
    surface = Color(0xFF131C28),
    onSurface = Color(0xFFE1E8F5),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFC4C6D3),
    outline = Color(0xFF8E909D),
    outlineVariant = Color(0xFF444651)
)

val AnchorShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AnchorParentTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AnchorShapes,
        content = content
    )
}
