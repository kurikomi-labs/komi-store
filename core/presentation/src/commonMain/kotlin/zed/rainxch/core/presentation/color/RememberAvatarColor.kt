package zed.rainxch.core.presentation.color

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun avatarColorFor(url: String?, fallback: Color): Color {
    if (url.isNullOrBlank()) return fallback
    return AvatarColorStore.colorFor(url) ?: fallback
}
