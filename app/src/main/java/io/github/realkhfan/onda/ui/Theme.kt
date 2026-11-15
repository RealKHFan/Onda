package io.github.realkhfan.onda.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg = Color(0xFF0F0F12)
val CardBg = Color(0xFF18181D)
val Raised = Color(0xFF24242B)
/** The highlight colour (violet unless you pick another in Settings), the same choices as on the desktop. */
val accentState = androidx.compose.runtime.mutableStateOf(Color(0xFF9D7BFF))
val Accent: Color get() = accentState.value
val accents = listOf("Violet" to 0xFF9D7BFF, "Blue" to 0xFF5B9DFF, "Teal" to 0xFF2EC4B6, "Green" to 0xFF4ADE80, "Yellow" to 0xFFFACC15,
    "Orange" to 0xFFFF9248, "Pink" to 0xFFF472B6, "Red" to 0xFFF87171)
val TextMain = Color(0xFFF5F5F7)
val TextDim = Color(0xFFA9A9B3)
val ErrorRed = Color(0xFFFF6B6B)

private fun scheme() = darkColorScheme(
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
    MaterialTheme(colorScheme = scheme(), content = content)
}
