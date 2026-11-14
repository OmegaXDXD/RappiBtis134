package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = GuindaPrimary,
    onPrimary = OnGuindaPrimary,
    primaryContainer = GuindaContainer,
    onPrimaryContainer = OnGuindaContainer,
    secondary = AmberSecondary,
    onSecondary = OnAmberSecondary,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = OnAmberContainer,
    tertiary = TerracottaTertiary,
    onTertiary = OnTerracottaTertiary,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracottaContainer,
    background = CreamBackground,
    onBackground = EspressoOnBackground,
    surface = WarmSurface,
    onSurface = EspressoOnBackground,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedCocoaOnSurfaceVariant,
    surfaceContainerLow = ColorTokens.SurfaceContainerLowLight,
    surfaceContainer = ColorTokens.SurfaceContainerLight,
    surfaceContainerHigh = ColorTokens.SurfaceContainerHighLight,
    outline = WarmOutline
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkGuindaPrimary,
    onPrimary = DarkOnGuindaPrimary,
    primaryContainer = DarkGuindaContainer,
    onPrimaryContainer = DarkOnGuindaContainer,
    secondary = DarkAmberSecondary,
    onSecondary = DarkOnAmberSecondary,
    secondaryContainer = DarkAmberContainer,
    onSecondaryContainer = DarkOnAmberContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkBackground,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLow = ColorTokens.SurfaceContainerLowDark,
    surfaceContainer = ColorTokens.SurfaceContainerDark,
    surfaceContainerHigh = ColorTokens.SurfaceContainerHighDark,
    outline = DarkOutline
)

private object ColorTokens {
    val SurfaceContainerLowLight = androidx.compose.ui.graphics.Color(0xFFF8F3EC)
    val SurfaceContainerLight = androidx.compose.ui.graphics.Color(0xFFF3ECE2)
    val SurfaceContainerHighLight = androidx.compose.ui.graphics.Color(0xFFEDE3D7)

    val SurfaceContainerLowDark = androidx.compose.ui.graphics.Color(0xFF201619)
    val SurfaceContainerDark = androidx.compose.ui.graphics.Color(0xFF271B1F)
    val SurfaceContainerHighDark = androidx.compose.ui.graphics.Color(0xFF322327)
}

val CafeteriaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun Cafeteria134Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = CafeteriaShapes,
        content = content
    )
}
