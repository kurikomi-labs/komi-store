package zed.rainxch.core.presentation.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.vocabulary.PlatformGlyph
import zed.rainxch.core.presentation.vocabulary.PlatformKind

@Composable
fun PlatformsChip(
    platforms: ImmutableList<PlatformKind>,
    modifier: Modifier = Modifier,
    background: Color = Color.Transparent,
    border: Color = MaterialTheme.colorScheme.outline,
    glyphSizeDp: Int = 14,
) {
    if (platforms.isEmpty()) return
    Row(
        modifier = modifier
            .clip(Radii.chip)
            .background(background)
            .border(width = 1.dp, color = border, shape = Radii.chip)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        platforms.forEach { kind ->
            PlatformGlyph(kind = kind, supported = true, sizeDp = glyphSizeDp)
        }
    }
}
