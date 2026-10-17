package br.com.tecoff.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TecAccentLight,
    onPrimary = TecPrimaryDark,
    primaryContainer = TecPrimaryDark,
    onPrimaryContainer = TecTextPrimaryDark,
    secondary = TecSuccess,
    background = TecBackgroundDark,
    surface = TecSurfaceDark,
    onBackground = TecTextPrimaryDark,
    onSurface = TecTextPrimaryDark,
    outline = TecBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = TecPrimary,
    onPrimary = TecSurfaceLight,
    primaryContainer = TecPrimaryLight,
    onPrimaryContainer = TecSurfaceLight,
    secondary = TecAccent,
    background = TecBackgroundLight,
    surface = TecSurfaceLight,
    onBackground = TecTextPrimaryLight,
    onSurface = TecTextPrimaryLight,
    outline = TecBorderLight
)

@Composable
fun TecOffTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
