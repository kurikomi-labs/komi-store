package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

enum class PlatformKind { ANDROID, WINDOWS, MACOS, LINUX }

@Composable
fun PlatformGlyph(
    kind: PlatformKind,
    supported: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Int = 18,
) {
    val ink = MaterialTheme.colorScheme.onSurface
    val ink2 = MaterialTheme.colorScheme.onSurfaceVariant
    val bg = MaterialTheme.colorScheme.background
    val color = if (supported) ink else ink2
    val alpha = if (supported) 1f else 0.32f
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        when (kind) {
            PlatformKind.ANDROID -> drawAndroid(color, bg, supported, alpha)
            PlatformKind.WINDOWS -> drawWindows(color, supported, alpha)
            PlatformKind.MACOS -> drawMacos(color, supported, alpha)
            PlatformKind.LINUX -> drawLinux(color, bg, supported, alpha)
        }
    }
}

private fun DrawScope.drawAndroid(c: Color, bg: Color, on: Boolean, alpha: Float) {
    val s = size.minDimension
    val dash = if (on) null else PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))
    val rect = androidx.compose.ui.geometry.Rect(
        offset = Offset(s * (6f / 24f), s * (3.5f / 24f)),
        size = Size(s * (12f / 24f), s * (17f / 24f)),
    )
    val cr = CornerRadius(s * (2.6f / 24f), s * (2.6f / 24f))
    if (on) {
        drawRoundRect(color = c.copy(alpha = alpha), topLeft = rect.topLeft, size = rect.size, cornerRadius = cr)
    } else {
        drawRoundRect(
            color = c.copy(alpha = alpha),
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = cr,
            style = Stroke(width = 1.4f.dp.toPx(), pathEffect = dash),
        )
    }
    drawCircle(
        color = if (on) bg else Color.Transparent,
        radius = s * (0.9f / 24f),
        center = Offset(s * (12f / 24f), s * (17.5f / 24f)),
    )
    if (!on) {
        drawCircle(
            color = c.copy(alpha = alpha),
            radius = s * (0.9f / 24f),
            center = Offset(s * (12f / 24f), s * (17.5f / 24f)),
            style = Stroke(width = 1f.dp.toPx()),
        )
    }
}

private fun DrawScope.drawWindows(c: Color, on: Boolean, alpha: Float) {
    val s = size.minDimension
    val dash = if (on) null else PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))
    val cellSize = Size(s * (6f / 24f), s * (6f / 24f))
    val cr = CornerRadius(s * (0.5f / 24f), s * (0.5f / 24f))
    listOf(5f to 5f, 13f to 5f, 5f to 13f, 13f to 13f).forEach { (x, y) ->
        val tl = Offset(s * (x / 24f), s * (y / 24f))
        if (on) {
            drawRoundRect(color = c.copy(alpha = alpha), topLeft = tl, size = cellSize, cornerRadius = cr)
        } else {
            drawRoundRect(
                color = c.copy(alpha = alpha),
                topLeft = tl,
                size = cellSize,
                cornerRadius = cr,
                style = Stroke(width = 1.4f.dp.toPx(), pathEffect = dash),
            )
        }
    }
}

private fun DrawScope.drawMacos(c: Color, on: Boolean, alpha: Float) {
    val s = size.minDimension
    val u = s / 24f
    val dash = if (on) null else PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))

    val body = Path().apply {
        moveTo(7f * u, 7.5f * u)
        cubicTo(4.5f * u, 7.5f * u, 2.5f * u, 10f * u, 2.5f * u, 13.5f * u)
        cubicTo(2.5f * u, 17.5f * u, 4.5f * u, 21.5f * u, 7f * u, 21.5f * u)
        cubicTo(8.2f * u, 21.5f * u, 9f * u, 20.8f * u, 10.5f * u, 20.8f * u)
        cubicTo(12f * u, 20.8f * u, 12.5f * u, 21.5f * u, 14f * u, 21.5f * u)
        cubicTo(15.3f * u, 21.5f * u, 16f * u, 21f * u, 17f * u, 20f * u)
        cubicTo(18.5f * u, 18.4f * u, 19f * u, 16.5f * u, 19f * u, 16.4f * u)
        cubicTo(18.9f * u, 16.4f * u, 16.5f * u, 15.5f * u, 16.5f * u, 12.8f * u)
        cubicTo(16.5f * u, 10.6f * u, 18.3f * u, 9.4f * u, 18.4f * u, 9.3f * u)
        cubicTo(17.3f * u, 7.7f * u, 15.6f * u, 7.5f * u, 15f * u, 7.5f * u)
        cubicTo(13.5f * u, 7.4f * u, 12.2f * u, 8.3f * u, 11.5f * u, 8.3f * u)
        cubicTo(10.8f * u, 8.3f * u, 9.7f * u, 7.5f * u, 8.5f * u, 7.5f * u)
        cubicTo(8f * u, 7.5f * u, 7.5f * u, 7.5f * u, 7f * u, 7.5f * u)
        close()
    }
    val leaf = Path().apply {
        moveTo(11.4f * u, 6f * u)
        cubicTo(12f * u, 4.6f * u, 13.1f * u, 3.8f * u, 14f * u, 3.6f * u)
        cubicTo(14.3f * u, 4.7f * u, 14f * u, 6f * u, 13.3f * u, 6.9f * u)
        cubicTo(12.7f * u, 7.7f * u, 11.7f * u, 8.2f * u, 10.8f * u, 8.2f * u)
        cubicTo(10.6f * u, 7.4f * u, 10.9f * u, 6.7f * u, 11.4f * u, 6f * u)
        close()
    }
    if (on) {
        drawPath(body, c.copy(alpha = alpha))
        drawPath(leaf, c.copy(alpha = alpha))
    } else {
        val stroke = Stroke(width = 1.4f.dp.toPx(), pathEffect = dash, join = StrokeJoin.Round)
        drawPath(body, c.copy(alpha = alpha), style = stroke)
        drawPath(leaf, c.copy(alpha = alpha), style = stroke)
    }
}

private fun DrawScope.drawLinux(c: Color, bg: Color, on: Boolean, alpha: Float) {
    val s = size.minDimension
    val u = s / 24f
    val dash = if (on) null else PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))

    val body = Path().apply {
        moveTo(12f * u, 2.5f * u)
        cubicTo(9f * u, 2.5f * u, 7.5f * u, 4.5f * u, 7.5f * u, 7.5f * u)
        cubicTo(7.5f * u, 8.5f * u, 7.8f * u, 9.2f * u, 8.1f * u, 9.8f * u)
        cubicTo(6.5f * u, 10.3f * u, 5.2f * u, 12.2f * u, 5.2f * u, 14.5f * u)
        cubicTo(5.2f * u, 17.5f * u, 6.2f * u, 20f * u, 8f * u, 21f * u)
        cubicTo(9f * u, 21.7f * u, 10.5f * u, 22f * u, 12f * u, 22f * u)
        cubicTo(13.5f * u, 22f * u, 15f * u, 21.7f * u, 16f * u, 21f * u)
        cubicTo(17.8f * u, 20f * u, 18.8f * u, 17.5f * u, 18.8f * u, 14.5f * u)
        cubicTo(18.8f * u, 12.2f * u, 17.5f * u, 10.3f * u, 15.9f * u, 9.8f * u)
        cubicTo(16.2f * u, 9.2f * u, 16.5f * u, 8.5f * u, 16.5f * u, 7.5f * u)
        cubicTo(16.5f * u, 4.5f * u, 15f * u, 2.5f * u, 12f * u, 2.5f * u)
        close()
    }

    val belly = Path().apply {
        moveTo(12f * u, 12.5f * u)
        cubicTo(10.3f * u, 12.5f * u, 9.5f * u, 14.5f * u, 9.5f * u, 16.8f * u)
        cubicTo(9.5f * u, 19f * u, 10.5f * u, 20.5f * u, 12f * u, 20.5f * u)
        cubicTo(13.5f * u, 20.5f * u, 14.5f * u, 19f * u, 14.5f * u, 16.8f * u)
        cubicTo(14.5f * u, 14.5f * u, 13.7f * u, 12.5f * u, 12f * u, 12.5f * u)
        close()
    }

    val beak = Path().apply {
        moveTo(10.4f * u, 7.8f * u)
        lineTo(13.6f * u, 7.8f * u)
        lineTo(12f * u, 9.4f * u)
        close()
    }

    val leftFoot = Path().apply {
        moveTo(7f * u, 21.5f * u)
        cubicTo(5.5f * u, 21.5f * u, 4.5f * u, 22f * u, 4.5f * u, 22.6f * u)
        cubicTo(4.5f * u, 23.2f * u, 6.5f * u, 23.5f * u, 8.5f * u, 23f * u)
        cubicTo(9.5f * u, 22.7f * u, 9.5f * u, 21.8f * u, 9f * u, 21.4f * u)
        close()
    }
    val rightFoot = Path().apply {
        moveTo(17f * u, 21.5f * u)
        cubicTo(18.5f * u, 21.5f * u, 19.5f * u, 22f * u, 19.5f * u, 22.6f * u)
        cubicTo(19.5f * u, 23.2f * u, 17.5f * u, 23.5f * u, 15.5f * u, 23f * u)
        cubicTo(14.5f * u, 22.7f * u, 14.5f * u, 21.8f * u, 15f * u, 21.4f * u)
        close()
    }

    if (on) {
        drawPath(leftFoot, c.copy(alpha = alpha))
        drawPath(rightFoot, c.copy(alpha = alpha))
        drawPath(body, c.copy(alpha = alpha))
        drawPath(belly, bg)
        drawPath(beak, c.copy(alpha = alpha))
        drawCircle(bg, s * (0.85f / 24f), Offset(10.4f * u, 6.4f * u))
        drawCircle(bg, s * (0.85f / 24f), Offset(13.6f * u, 6.4f * u))
        drawCircle(c.copy(alpha = alpha), s * (0.35f / 24f), Offset(10.4f * u, 6.4f * u))
        drawCircle(c.copy(alpha = alpha), s * (0.35f / 24f), Offset(13.6f * u, 6.4f * u))
    } else {
        val stroke = Stroke(width = 1.4f.dp.toPx(), pathEffect = dash, join = StrokeJoin.Round)
        drawPath(leftFoot, c.copy(alpha = alpha), style = stroke)
        drawPath(rightFoot, c.copy(alpha = alpha), style = stroke)
        drawPath(body, c.copy(alpha = alpha), style = stroke)
        drawPath(belly, c.copy(alpha = alpha), style = Stroke(width = 1f.dp.toPx(), pathEffect = dash))
    }
}
