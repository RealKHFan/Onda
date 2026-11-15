package io.github.realkhfan.onda.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0F0F12)
val CardBg = Color(0xFF18181D)
val Raised = Color(0xFF24242B)
val Accent = Color(0xFF9D7BFF)
val TextMain = Color(0xFFF5F5F7)
val TextDim = Color(0xFFA9A9B3)
val ErrorRed = Color(0xFFFF6B6B)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF141018),
    background = Bg,
    onBackground = TextMain,
    surface = CardBg,
    onSurface = TextMain,
    surfaceVariant = Raised,
    onSurfaceVariant = TextDim,
    error = ErrorRed,
)

@Composable
fun OndaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
