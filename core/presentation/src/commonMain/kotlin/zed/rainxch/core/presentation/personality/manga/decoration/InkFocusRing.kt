package zed.rainxch.core.presentation.personality.manga.decoration

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Keyboard-focus ring (Manga Design Spec v1.0): a 3px accent outline drawn 3px outside the
// box on focus, unclipped. Pairs with clickable(indication = null) — the stamp is the press
// feedback, this restores the focus affordance that null indication drops. Read in the draw
// phase so a focus change never recomposes.
fun Modifier.inkFocusRing(
    focused: () -> Boolean,
    color: Color,
    width: Dp = 3.dp,
    gap: Dp = 3.dp,
): Modifier =
    drawWithCache {
        val strokePx = width.toPx()
        val inset = -(gap.toPx() + strokePx / 2f)
        onDrawWithContent {
            drawContent()
            if (focused()) {
                drawRect(
                    color = color,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2f * inset, size.height - 2f * inset),
                    style = Stroke(width = strokePx),
                )
            }
        }
    }
