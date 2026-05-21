package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.LocalStatusColors

/**
 * 4 ascending bars — WiFi-style mirror / connection strength. Replaces "62 ms latency"
 * prose (DESIGN.md §4.1). `level` in 0..4; bars above `level` use outline color.
 */
@Composable
fun SignalBars(
    level: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 14,
) {
    val activeColor = LocalStatusColors.current.freshnessFresh
    val inactiveColor = MaterialTheme.colorScheme.outline
    val clampedLevel = level.coerceIn(0, 4)
    Canvas(modifier = modifier.size(width = (sizeDp + 4).dp, height = sizeDp.dp)) {
        val barW = 2.5.dp.toPx()
        val gap = 1.5.dp.toPx()
        val cornerR = 1.dp.toPx()
        for (i in 0 until 4) {
            val barH = size.height * (0.3f + (i + 1) * 0.18f)
            val left = i * (barW + gap)
            val top = size.height - barH
            val c = if (i < clampedLevel) activeColor else inactiveColor
            drawRoundRect(
                color = c,
                topLeft = Offset(left, top),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(cornerR, cornerR),
            )
        }
    }
}
