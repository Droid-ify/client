package com.looker.droidify.compose.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define the color palette for light theme based on XML colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF416835),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC2EFAE),
    onPrimaryContainer = Color(0xFF022100),
    secondary = Color(0xFF54624D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E8CC),
    onSecondaryContainer = Color(0xFF121F0E),
    tertiary = Color(0xFF845416),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBC),
    onTertiaryContainer = Color(0xFF2C1700),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8FAF0),
    onBackground = Color(0xFF191D17),
    surface = Color(0xFFF8FAF0),
    onSurface = Color(0xFF191D17),
    surfaceVariant = Color(0xFFDFE4D7),
    onSurfaceVariant = Color(0xFF43483F),
    outline = Color(0xFF73796E)
)

// Define the color palette for dark theme based on XML colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA6D394),
    onPrimary = Color(0xFF13380A),
    primaryContainer = Color(0xFF2A4F1F),
    onPrimaryContainer = Color(0xFFC2EFAE),
    secondary = Color(0xFFBBCBB1),
    onSecondary = Color(0xFF273421),
    secondaryContainer = Color(0xFF3D4B36),
    onSecondaryContainer = Color(0xFFD7E8CC),
    tertiary = Color(0xFFFBBA73),
    onTertiary = Color(0xFF492900),
    tertiaryContainer = Color(0xFF683D00),
    onTertiaryContainer = Color(0xFFFFDCBC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF11140F),
    onBackground = Color(0xFFE1E4DA),
    surface = Color(0xFF11140F),
    onSurface = Color(0xFFE1E4DA),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC3C8BC),
    outline = Color(0xFF8D9387)
)

// Define the color palette for dark theme with AMOLED (medium contrast) based on XML colors
private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFFAAD798),
    onPrimary = Color(0xFF021B00),
    primaryContainer = Color(0xFF729C62),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFFC0D0B5),
    onSecondary = Color(0xFF0D1909),
    secondaryContainer = Color(0xFF86957D),
    onSecondaryContainer = Color(0xFF000000),
    tertiary = Color(0xFFFFBE78),
    onTertiary = Color(0xFF241200),
    tertiaryContainer = Color(0xFFBE8544),
    onTertiaryContainer = Color(0xFF000000),
    error = Color(0xFFFFBAB1),
    onError = Color(0xFF370001),
    errorContainer = Color(0xFFFF5449),
    onErrorContainer = Color(0xFF000000),
    background = Color(0xFF000000), // pitch_black
    onBackground = Color(0xFFE1E4DA),
    surface = Color(0xFF11140F),
    onSurface = Color(0xFFF9FCF2),
    surfaceVariant = Color(0xFF43483F),
    onSurfaceVariant = Color(0xFFC7CDC0),
    outline = Color(0xFF9FA599)
)

@Composable
fun DroidifyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    amoledTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme && amoledTheme -> AmoledColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(), // Use the default Material 3 Typography
        content = content
    )
}
