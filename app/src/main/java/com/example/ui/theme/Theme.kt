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
    primary = ZevoraPrimaryDark,
    onPrimary = Color(0xFF00344F),
    primaryContainer = ZevoraPrimaryContainerDark,
    onPrimaryContainer = ZevoraOnPrimaryContainerDark,
    secondary = ZevoraSecondaryDark,
    onSecondary = Color(0xFF003831),
    secondaryContainer = ZevoraSecondaryContainerDark,
    onSecondaryContainer = Color(0xFF99F6E4),
    tertiary = ZevoraTertiaryDark,
    background = ZevoraBackgroundDark,
    surface = ZevoraSurfaceDark,
    surfaceVariant = ZevoraSurfaceVariantDark,
    outline = ZevoraOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = ZevoraPrimary,
    onPrimary = Color.White,
    primaryContainer = ZevoraPrimaryContainer,
    onPrimaryContainer = ZevoraOnPrimaryContainer,
    secondary = ZevoraSecondary,
    onSecondary = Color.White,
    secondaryContainer = ZevoraSecondaryContainer,
    onSecondaryContainer = ZevoraOnSecondaryContainer,
    tertiary = ZevoraTertiary,
    background = ZevoraBackgroundLight,
    surface = ZevoraSurfaceLight,
    surfaceVariant = ZevoraSurfaceVariantLight,
    outline = ZevoraOutlineLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our brand custom colors for cohesive medical theme
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
