package dev.reynardus.flinkly.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = Green20,
    secondary = Orange40,
    onSecondary = Color.White,
    secondaryContainer = OrangeContainer,
    onSecondaryContainer = Color(0xFF5D2E00),
    tertiary = Blue40,
    onTertiary = Color.White,
    tertiaryContainer = Blue80,
    onTertiaryContainer = Color(0xFF003161),
    error = Red40,
    onError = Color.White,
    background = Color(0xFFF5F5F5),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = GreenContainer,
    onSurfaceVariant = Color(0xFF4A4A4A),
    outline = Color(0xFFBDBDBD),
)

@Composable
fun FlinklyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
