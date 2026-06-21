package zed.rainxch.tweaks.presentation.components.appearance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.components.icon.KomiIcon
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.manga.MangaAccent
import zed.rainxch.core.presentation.personality.manga.decoration.hardShadow
import zed.rainxch.core.presentation.personality.manga.mangaAccentSwatch

@Composable
fun AccentSwatch(
    accent: MangaAccent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPersonality.current.colors
    val swatch = mangaAccentSwatch(accent)
    val fill = swatch?.first ?: colors.onSurface
    val onFill = swatch?.second ?: colors.background

    Box(
        modifier =
            modifier
                .size(40.dp)
                .then(if (selected) Modifier.hardShadow(DpOffset(3.dp, 3.dp), colors.shadow) else Modifier)
                .background(fill)
                .border(2.5.dp, colors.outline)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (accent == MangaAccent.MONO) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(
                            Brush.linearGradient(
                                0.5f to colors.onSurface,
                                0.5f to colors.surface,
                                start = Offset.Zero,
                                end = Offset.Infinite,
                            ),
                        ),
            )
        }
        if (selected) {
            KomiIcon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = onFill,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
