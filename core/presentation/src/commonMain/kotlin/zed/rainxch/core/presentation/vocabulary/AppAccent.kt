package zed.rainxch.core.presentation.vocabulary

import androidx.compose.ui.graphics.Color

data class AppAccent(val c: Color, val lt: Color, val dtAlpha: Float = 0.22f) {
    fun tintFor(isDark: Boolean): Color = if (isDark) c.copy(alpha = dtAlpha) else lt
}
