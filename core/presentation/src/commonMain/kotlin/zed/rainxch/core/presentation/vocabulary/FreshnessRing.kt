package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun FreshnessRing(
    daysSinceRelease: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 64,
    strokeDp: Float = if (sizeDp >= 60) 2.5f else 2f,
    color: Color? = null,
    content: @Composable () -> Unit,
) {
    val f = freshnessOf(daysSinceRelease)
    val ringColor = color ?: f.color
    val ringSize = sizeDp + 14
    Box(
        modifier = modifier.size(ringSize.dp),
        contentAlignment = Alignment.Center,
    ) {
        FreshnessArc(
            color = ringColor,
            fraction = f.ringFraction,
            strokeDp = strokeDp,
            modifier = Modifier.size(ringSize.dp),
        )
        Box(
            modifier = Modifier.size(sizeDp.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun FreshnessArc(
    color: Color,
    fraction: Float,
    strokeDp: Float,
    modifier: Modifier,
) {
    Canvas(modifier = modifier) {
        val sw = strokeDp.dp.toPx()
        val radius = (size.minDimension - sw - 2.dp.toPx()) / 2f
        val topLeft = Offset(
            (size.width - radius * 2) / 2f,
            (size.height - radius * 2) / 2f,
        )
        val arcSize = Size(radius * 2, radius * 2)

        drawArc(
            color = color.copy(alpha = 0.14f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )

        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * fraction,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = sw, cap = StrokeCap.Round),
        )
    }
}
