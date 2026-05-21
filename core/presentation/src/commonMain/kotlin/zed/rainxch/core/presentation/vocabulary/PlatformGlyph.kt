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
            PlatformKind.LINUX -> drawLinux(color, supported, alpha)
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
    val dash = if (on) null else PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))
    val center = Offset(s * (12f / 24f), s * (13f / 24f))
    val radius = s * (7f / 24f)
    if (on) {
        drawCircle(color = c.copy(alpha = alpha), radius = radius, center = center)
    } else {
        drawCircle(
            color = c.copy(alpha = alpha),
            radius = radius,
            center = center,
            style = Stroke(width = 1.4f.dp.toPx(), pathEffect = dash),
        )
    }
    val stem = Path().apply {
        moveTo(s * (12f / 24f), s * (6f / 24f))
        quadraticTo(s * (13.5f / 24f), s * (4f / 24f), s * (15f / 24f), s * (4f / 24f))
    }
    drawPath(stem, c.copy(alpha = alpha), style = Stroke(width = (if (on) 1.5f else 1.4f).dp.toPx(), pathEffect = dash))
}

private fun DrawScope.drawLinux(c: Color, on: Boolean, alpha: Float) {
    val s = size.minDimension
    val dash = if (on) null else PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 2.dp.toPx()))
    val hex = Path().apply {
        moveTo(s * (12f / 24f), s * (3.5f / 24f))
        lineTo(s * (19.5f / 24f), s * (7.5f / 24f))
        lineTo(s * (19.5f / 24f), s * (16.5f / 24f))
        lineTo(s * (12f / 24f), s * (20.5f / 24f))
        lineTo(s * (4.5f / 24f), s * (16.5f / 24f))
        lineTo(s * (4.5f / 24f), s * (7.5f / 24f))
        close()
    }
    if (on) {
        drawPath(hex, c.copy(alpha = alpha))
    } else {
        drawPath(hex, c.copy(alpha = alpha), style = Stroke(width = 1.4f.dp.toPx(), pathEffect = dash, join = StrokeJoin.Round))
    }
}
