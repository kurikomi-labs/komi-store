package zed.rainxch.core.presentation.components.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape
import zed.rainxch.core.presentation.vocabulary.AppAccent

/**
 * Lead/hero card — full-width, accent-tinted bg, soft radial accent bloom, wonky
 * squircle shape (DESIGN.md §7.3). Used for the top Hot release card on Home and
 * featured items. Bloom uses [accent.c] at low alpha; DESIGN.md §2.5 explicitly
 * permits this as editorial flair.
 */
@Composable
fun LeadHeroCard(
    accent: AppAccent,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val baseTint = accent.tintFor(isDark)
    Box(
        modifier = modifier
            .clip(WonkySquircleShape.CtaPrimary)
            .background(if (isDark) cs.surface else baseTint)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(accent.c.copy(alpha = 0.14f), accent.c.copy(alpha = 0f)),
                        center = Offset(x = 200f, y = 60f),
                        radius = 480f,
                    ),
                ),
        )
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}
