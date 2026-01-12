package com.mezo.biometric.ui.theme

import android.app.Activity
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
    primary = PearlWhite,
    onPrimary = RichBlack,
    primaryContainer = Charcoal,
    onPrimaryContainer = PearlWhite,
    secondary = Silver,
    onSecondary = RichBlack,
    secondaryContainer = DeepGray,
    onSecondaryContainer = PearlWhite,
    background = RichBlack,
    surface = Charcoal,
    onBackground = PearlWhite,
    onSurface = PearlWhite,
    surfaceVariant = DeepGray,
    onSurfaceVariant = Silver,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = RichBlack,
    onPrimary = PearlWhite,
    primaryContainer = Silver,
    onPrimaryContainer = RichBlack,
    secondary = DeepGray,
    onSecondary = PearlWhite,
    secondaryContainer = PearlWhite,
    onSecondaryContainer = RichBlack,
    background = PearlWhite,
    onBackground = RichBlack,
    surface = PearlWhite,
    onSurface = RichBlack,
    surfaceVariant = Silver,
    onSurfaceVariant = RichBlack,
    error = ErrorRed
)

@Composable
fun BiometricFeatureTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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