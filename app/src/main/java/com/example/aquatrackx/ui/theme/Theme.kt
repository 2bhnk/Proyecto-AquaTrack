package com.example.aquatrackx.ui.theme

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
    primary = AquaSecondary,
    secondary = AquaTertiary,
    tertiary = AquaPrimaryDark,
    error = Color(0xFFEF9A9A)
)

private val LightColorScheme = lightColorScheme(
    primary = AquaPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB2EBF2),
    onPrimaryContainer = Color(0xFF00363A),
    secondary = AquaTertiary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCEEF3),
    onSecondaryContainer = Color(0xFF001F24),
    tertiary = AquaPrimaryDark,
    background = FondoSuave,
    onBackground = Color(0xFF191C1C),
    surface = SurfaceClara,
    onSurface = Color(0xFF191C1C),
    surfaceVariant = Color(0xFFDBE4E6),
    onSurfaceVariant = Color(0xFF3F4948),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

/**
 * Tema visual de la aplicación. Se desactiva el color dinámico para mantener
 * una identidad coherente (jerarquía visual y consistencia entre vistas, IE 2.1.1).
 */
@Composable
fun AquaTrackXTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
