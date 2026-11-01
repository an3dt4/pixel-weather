package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = SkyPrimaryDark,
    onPrimary = SkyOnPrimaryDark,
    primaryContainer = SkyPrimaryContainerDark,
    onPrimaryContainer = SkyOnPrimaryContainerDark,
    secondary = SkySecondaryDark,
    onSecondary = SkyOnSecondaryDark,
    secondaryContainer = SkySecondaryContainerDark,
    onSecondaryContainer = SkyOnSecondaryContainerDark,
    tertiary = SkyTertiaryDark,
    onTertiary = SkyOnTertiaryDark,
    tertiaryContainer = SkyTertiaryContainerDark,
    onTertiaryContainer = SkyOnTertiaryContainerDark,
    background = SkyBackgroundDark,
    onBackground = SkyOnBackgroundDark,
    surface = SkySurfaceDark,
    onSurface = SkyOnSurfaceDark,
    surfaceVariant = SkySurfaceVariantDark,
    onSurfaceVariant = SkyOnSurfaceVariantDark,
    outline = SkyOutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = SkyPrimaryLight,
    onPrimary = SkyOnPrimaryLight,
    primaryContainer = SkyPrimaryContainerLight,
    onPrimaryContainer = SkyOnPrimaryContainerLight,
    secondary = SkySecondaryLight,
    onSecondary = SkyOnSecondaryLight,
    secondaryContainer = SkySecondaryContainerLight,
    onSecondaryContainer = SkyOnSecondaryContainerLight,
    tertiary = SkyTertiaryLight,
    onTertiary = SkyOnTertiaryLight,
    tertiaryContainer = SkyTertiaryContainerLight,
    onTertiaryContainer = SkyOnTertiaryContainerLight,
    background = SkyBackgroundLight,
    onBackground = SkyOnBackgroundLight,
    surface = SkySurfaceLight,
    onSurface = SkyOnSurfaceLight,
    surfaceVariant = SkySurfaceVariantLight,
    onSurfaceVariant = SkyOnSurfaceVariantLight,
    outline = SkyOutlineLight
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun SkyPillWeatherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
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
        shapes = AppShapes,
        content = content
    )
}
