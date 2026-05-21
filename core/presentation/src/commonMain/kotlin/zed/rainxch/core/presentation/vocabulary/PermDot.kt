package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.LocalStatusColors

/** Permission risk classification mapped to a single colored dot. */
enum class PermLevel { LOW, MODERATE, HIGH }

/**
 * Single-dot heat indicator for permission risk. Replaces "App permissions" wall-of-text
 * (DESIGN.md §4.1). Optional 3px halo ring for emphasis on hero surfaces.
 */
@Composable
fun PermDot(
    level: PermLevel,
    modifier: Modifier = Modifier,
    size: Int = 8,
    ring: Boolean = false,
) {
    val status = LocalStatusColors.current
    val color = when (level) {
        PermLevel.LOW -> status.permLow
        PermLevel.MODERATE -> status.permModerate
        PermLevel.HIGH -> status.permHigh
    }
    val ringExtraDp = if (ring) 6 else 0
    Canvas(modifier = modifier.size((size + ringExtraDp).dp)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val dotRadius = size.dp.toPx() / 2f
        if (ring) {
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = dotRadius + 3.dp.toPx(),
                center = Offset(cx, cy),
            )
        }
        drawCircle(color = color, radius = dotRadius, center = Offset(cx, cy))
    }
}
