package br.unb.ru.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = NotionActivePill,
    onPrimary = NotionActivePillText,
    background = NotionWarmBgLight,
    onBackground = NotionTextPrimaryLight,
    surface = NotionCardBgLight,
    onSurface = NotionTextPrimaryLight,
    surfaceVariant = NotionBorderLight,
    outline = NotionBorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = NotionTextPrimaryDark,
    onPrimary = NotionWarmBgDark,
    background = NotionWarmBgDark,
    onBackground = NotionTextPrimaryDark,
    surface = NotionCardBgDark,
    onSurface = NotionTextPrimaryDark,
    surfaceVariant = NotionBorderDark,
    outline = NotionBorderDark
)

@Composable
fun RUUnBTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
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
        typography = NotionTypography,
        content = content
    )
}
