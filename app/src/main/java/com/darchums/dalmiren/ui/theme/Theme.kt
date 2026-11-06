package com.darchums.dalmiren.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ChampagneGold,
    onPrimary = MidnightNavy,
    primaryContainer = CardDark,
    onPrimaryContainer = ChampagneGoldLight,
    secondary = RoyalBlueLight,
    onSecondary = Ivory,
    secondaryContainer = BorderDark,
    onSecondaryContainer = Ivory,
    tertiary = ChampagneGoldLight,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = MidnightNavy,
    onPrimary = Ivory,
    primaryContainer = SoftBlueContainer,
    onPrimaryContainer = SoftBlueOnContainer,
    secondary = RoyalBlue,
    onSecondary = Ivory,
    secondaryContainer = SoftGoldContainer,
    onSecondaryContainer = SoftGoldOnContainer,
    tertiary = ChampagneGold,
    background = SurfaceLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight
)

@Composable
fun DALMIRENAiTheme(
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
        typography = Typography,
        content = content
    )
}
