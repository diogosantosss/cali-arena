package com.caliarena.ui.theme

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

// Destructive (vermelho ameaçador, equivalente a oklch do CSS)
private val DarkDestructive = Color(0xFFE5484D)
private val LightDestructive = Color(0xFFD9534F)

private val DarkColorScheme =
    darkColorScheme(
        primary = DarkPrimary,
        onPrimary = DarkPrimaryForeground,
        secondary = DarkSecondary,
        onSecondary = DarkSecondaryForeground,
        tertiary = DarkPrimary,
        onTertiary = DarkPrimaryForeground,
        background = DarkBackground,
        onBackground = DarkForeground,
        surface = DarkCard,
        onSurface = DarkForeground,
        surfaceVariant = DarkSecondary,
        onSurfaceVariant = DarkSecondaryForeground,
        error = DarkDanger,
        onError = Color.Black,
        outline = DarkBorder,
        outlineVariant = DarkBorderHover,
        surfaceContainerLowest = DarkCard,
        surfaceContainerLow = DarkSecondary,
        surfaceContainer = DarkSecondary,
        surfaceContainerHigh = DarkBorder,
        surfaceContainerHighest = DarkFaint,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = LightPrimary,
        onPrimary = LightPrimaryForeground,
        secondary = LightSecondary,
        onSecondary = LightSecondaryForeground,
        tertiary = LightPrimary,
        onTertiary = LightPrimaryForeground,
        background = LightBackground,
        onBackground = LightForeground,
        surface = LightCard,
        onSurface = LightForeground,
        surfaceVariant = LightMuted,
        onSurfaceVariant = LightMutedForeground,
        error = LightDanger,
        onError = Color.White,
        outline = LightBorder,
        outlineVariant = LightBorder,
        surfaceContainerLowest = LightCard,
        surfaceContainerLow = LightBackground,
        surfaceContainer = LightMuted,
        surfaceContainerHigh = LightBorder,
        surfaceContainerHighest = LightFaint,
    )

@Composable
fun CaliArenaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }

            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
