package br.com.ruy.pos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = RuyRed,
    onPrimary = RuyWhite,
    secondary = RuyBlack,
    onSecondary = RuyWhite,
    background = RuyBackground,
    onBackground = RuyTextPrimary,
    surface = RuySurface,
    onSurface = RuyTextPrimary,
    surfaceVariant = RuyLightGray,
    outline = RuyBorder,
    error = RuyError
)

@Composable
fun RuyPosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}


