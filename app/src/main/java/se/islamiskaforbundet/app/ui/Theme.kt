package se.islamiskaforbundet.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val IfNavy = Color(0xFF1A237E)
val IfNavyDark = Color(0xFF0F1450)
val IfGold = Color(0xFFC4B357)
val IfGoldLight = Color(0xFFE6DAA0)
val IfCream = Color(0xFFFAF8F0)

private val Light = lightColorScheme(
    primary = IfNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE0F7),
    onPrimaryContainer = IfNavyDark,
    secondary = IfGold,
    onSecondary = IfNavyDark,
    secondaryContainer = IfGoldLight,
    onSecondaryContainer = IfNavyDark,
    background = IfCream,
    surface = IfCream,
    surfaceVariant = Color(0xFFEDEBE2),
)

private val Dark = darkColorScheme(
    primary = IfGold,
    onPrimary = IfNavyDark,
    primaryContainer = Color(0xFF2B3399),
    onPrimaryContainer = Color.White,
    secondary = IfGold,
    onSecondary = IfNavyDark,
    secondaryContainer = Color(0xFF4A4320),
    onSecondaryContainer = IfGoldLight,
    background = Color(0xFF0B0E2E),
    surface = Color(0xFF0B0E2E),
    surfaceVariant = Color(0xFF1C2050),
)

@Composable
fun IfTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
