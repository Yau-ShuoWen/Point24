package com.shuowen.point24.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    System,
    Light,
    Dark
}

private val Point24DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Ink,
    secondary = Mist,
    onSecondary = Ink,
    secondaryContainer = Moss,
    onSecondaryContainer = CreamCard,
    tertiary = Moss,
    onTertiary = CreamCard,
    background = TableGreen,
    onBackground = CreamCard,
    surface = FeltGreen,
    onSurface = CreamCard,
    surfaceVariant = Moss,
    onSurfaceVariant = Mist,
    surfaceContainer = Color(0xFF1E453D),
    surfaceContainerHigh = Color(0xFF255046),
    surfaceBright = CreamCard,
    outline = SoftSlate,
    error = Berry,
    errorContainer = Color(0xFF5A2C2C),
    onErrorContainer = Color(0xFFF8DEDE)
)

private val Point24LightColorScheme = lightColorScheme(
    primary = Sunburst,
    onPrimary = Snow,
    secondary = SageMist,
    onSecondary = Ink,
    secondaryContainer = SoftMint,
    onSecondaryContainer = ForestInk,
    tertiary = ForestInk,
    onTertiary = Ivory,
    background = Linen,
    onBackground = Ink,
    surface = Ivory,
    onSurface = Ink,
    surfaceVariant = PaleMoss,
    onSurfaceVariant = ForestInk,
    surfaceContainer = Porcelain,
    surfaceContainerHigh = MintWash,
    surfaceBright = White,
    outline = SageOutline,
    error = Berry,
    errorContainer = Color(0xFFF4D9D9),
    onErrorContainer = Color(0xFF5A2C2C)
)

@Composable
fun Point24Theme(
    themeMode: ThemeMode = ThemeMode.System,
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    MaterialTheme(
        colorScheme = if (isDarkTheme) Point24DarkColorScheme else Point24LightColorScheme,
        typography = Typography,
        content = content
    )
}
