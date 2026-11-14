package com.hisabh.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ForestGreenPrimary,
    onPrimary = CreamBackground,
    primaryContainer = ForestGreenContainer,
    onPrimaryContainer = ForestGreenOnContainer,
    secondary = ForestGreenLight,
    onSecondary = CreamBackground,
    secondaryContainer = CreamCard,
    onSecondaryContainer = TextPrimary,
    background = CreamBackground,
    onBackground = TextPrimary,
    surface = CreamSurface,
    onSurface = TextPrimary,
    surfaceVariant = CreamCard,
    onSurfaceVariant = TextSecondary,
    outline = CreamBorder,
    outlineVariant = CreamDivider,
    error = DebitTerracotta,
    onError = CreamBackground
)

private val DarkColorScheme = darkColorScheme(
    primary = ForestGreenLight,
    onPrimary = CreamBackground,
    primaryContainer = ForestGreenDark,
    onPrimaryContainer = ForestGreenContainer,
    secondary = ForestGreenPrimary,
    onSecondary = CreamBackground,
    secondaryContainer = ForestGreenDark,
    onSecondaryContainer = CreamBackground,
    background = ForestGreenDark,
    onBackground = CreamBackground,
    surface = Color(0xFF142E20),
    onSurface = CreamBackground,
    surfaceVariant = Color(0xFF1B3B2B),
    onSurfaceVariant = Color(0xFFA5C0B1),
    outline = Color(0xFF284D3A),
    outlineVariant = Color(0xFF1F3D2E),
    error = DebitTerracotta,
    onError = CreamBackground
)

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun HisabhTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context.findActivity()
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = HisabhTypography,
        content = content
    )
}
