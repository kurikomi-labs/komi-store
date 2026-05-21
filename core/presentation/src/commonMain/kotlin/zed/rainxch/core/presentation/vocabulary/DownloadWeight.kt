package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.log10
import kotlin.math.min

/**
 * Log-scale dot inside a fixed-size ring. Radius = `log10(downloads)`. Replaces
 * "62.8k downloads" prose with adoption magnitude (DESIGN.md §4.1).
 */
@Composable
fun DownloadWeight(
    downloads: Long,
    modifier: Modifier = Modifier,
    sizeDp: Int = 16,
) {
    val ringColor = MaterialTheme.colorScheme.outline
    val dotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    val log = log10(downloads.coerceAtLeast(1L).toDouble()).toFloat()
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val ringRadius = size.width / 2f - 1.dp.toPx()
        val dotRadius = min(size.width / 2f, 2.dp.toPx() + log * 1.6.dp.toPx())
        drawCircle(
            color = ringColor,
            radius = ringRadius,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx()),
        )
        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = androidx.compose.ui.geometry.Offset(cx, cy),
        )
    }
}
