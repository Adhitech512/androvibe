package com.kumbidi.androvibe.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// NvChad inspired Dark Scheme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF89B4FA),
    secondary = Color(0xFFF38BA8),
    tertiary = Color(0xFFA6E3A1),
    background = Color(0xFF1E1E2E), // Base
    surface = Color(0xFF181825),   // Mantle
    onPrimary = Color(0xFF11111B), // Crust
    onSecondary = Color(0xFF11111B),
    onTertiary = Color(0xFF11111B),
    onBackground = Color(0xFFCDD6F4), // Text
    onSurface = Color(0xFFCDD6F4)
)

@Composable
fun AndroVibeTheme(
    darkTheme: Boolean = true, // Force dark theme for NvChad vibe
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
