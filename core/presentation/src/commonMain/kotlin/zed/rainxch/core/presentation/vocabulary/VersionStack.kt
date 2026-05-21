package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.LocalStatusColors
import kotlin.math.min

@Composable
fun VersionStack(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color? = null,
    widthDp: Int = 22,
) {
    val fillColor = color ?: LocalStatusColors.current.freshnessWarm
    val n = min(count, 7)
    if (n == 0) return
    val heightDp = n * 3 + 5
    Canvas(modifier = modifier.size(width = widthDp.dp, height = heightDp.dp)) {
        val barWidth = size.width
        val barHeight = 4.dp.toPx()
        val gap = 3.dp.toPx()
        val barRadius = 1.5.dp.toPx()
        for (i in 0 until n) {
            val top = i * gap
            val alpha = 0.35f + i * 0.08f
            drawRoundRect(
                color = fillColor.copy(alpha = alpha),
                topLeft = androidx.compose.ui.geometry.Offset(0f, top),
                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barRadius, barRadius),
            )
        }
    }
}
