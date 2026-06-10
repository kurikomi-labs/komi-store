package zed.rainxch.core.presentation.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private fun repoHue(seed: String): Float {
    var hash = 0
    for (character in seed) {
        hash = hash * 31 + character.code
    }
    return (((hash % 360) + 360) % 360).toFloat()
}

fun repoAccentColor(seed: String): Color = Color.hsv(repoHue(seed), 0.55f, 0.90f)

fun repoAccentBrush(seed: String): Brush {
    val hue = repoHue(seed)
    return Brush.linearGradient(
        listOf(
            Color.hsv(hue, 0.55f, 0.90f),
            Color.hsv((hue + 22f) % 360f, 0.68f, 0.74f),
        ),
    )
}

fun repoMonogram(seed: String): String =
    seed.filter { it.isLetterOrDigit() }.take(2).uppercase().ifBlank { "?" }
