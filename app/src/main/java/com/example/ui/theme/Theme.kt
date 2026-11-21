package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MotoOrange,
    onPrimary = Color.White,
    primaryContainer = MotoOrangeDark,
    onPrimaryContainer = MotoOrangeLight,
    secondary = MotoCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = MotoCyanLight,
    tertiary = Color(0xFFFFB74D),
    background = DarkBackground,
    onBackground = Color(0xFFE1E2E6),
    surface = DarkSurface,
    onSurface = Color(0xFFE1E2E6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = DarkBorder
)

private val LightColorScheme = lightColorScheme(
    primary = MotoOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE0B2),
    onPrimaryContainer = MotoOrangeDark,
    secondary = MotoTitanium,
    onSecondary = Color.White,
    secondaryContainer = LightSurfaceVariant,
    onSecondaryContainer = MotoTitaniumDark,
    tertiary = MotoCyan,
    background = LightBackground,
    onBackground = Color(0xFF1A1C1E),
    surface = LightSurface,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF44474E),
    outline = LightBorder
)

@Composable
fun MotoLogTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep custom brand identity intact
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
