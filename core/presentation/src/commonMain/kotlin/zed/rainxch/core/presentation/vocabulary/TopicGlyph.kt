package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.tokens.Tokens

/**
 * Micro-pictogram per supported topic (DESIGN.md §4.2). Monochrome — never carries
 * an accent. Returns `null` (renders nothing) when the topic isn't in the supported
 * set or its alias map.
 */
@Composable
fun TopicGlyph(
    topic: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 14,
    color: Color = LocalContentColor.current,
) {
    val resolved = resolveTopic(topic) ?: return
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val sw = 1.7f.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (resolved) {
            "self-hosted" -> drawSelfHosted(color, stroke)
            "mobile" -> drawMobile(color, stroke)
            "photo" -> drawPhoto(color, stroke)
            "video" -> drawVideo(color, stroke)
            "book" -> drawBook(color, stroke)
            "manga" -> drawManga(color, stroke)
            "key" -> drawKey(color, stroke)
            "audio" -> drawAudio(color)
            "backup" -> drawBackup(color, stroke)
            "reader" -> drawReader(color, stroke)
            "cross-platform" -> drawCrossPlatform(color, stroke)
            "cloud" -> drawCloud(color, stroke)
        }
    }
}

private fun resolveTopic(topic: String): String? {
    val key = topic.lowercase()
    if (key in Tokens.Topics.supported) return key
    return Tokens.Topics.aliases[key]
}

private fun DrawScope.scaled(viewBoxValue: Float) = viewBoxValue / 24f * size.minDimension

private fun DrawScope.drawSelfHosted(c: Color, s: Stroke) {
    val p = Path().apply {
        moveTo(scaled(4f), scaled(12f))
        lineTo(scaled(12f), scaled(5f))
        lineTo(scaled(20f), scaled(12f))
        lineTo(scaled(20f), scaled(19f))
        lineTo(scaled(4f), scaled(19f))
        close()
    }
    drawPath(p, c, style = s)
}

private fun DrawScope.drawMobile(c: Color, s: Stroke) {
    drawRoundRect(
        color = c,
        topLeft = Offset(scaled(7f), scaled(3f)),
        size = Size(scaled(10f), scaled(18f)),
        cornerRadius = CornerRadius(scaled(2f), scaled(2f)),
        style = s,
    )
    drawLine(
        color = c,
        start = Offset(scaled(11f), scaled(18f)),
        end = Offset(scaled(13f), scaled(18f)),
        strokeWidth = s.width,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawPhoto(c: Color, s: Stroke) {
    drawRoundRect(
        color = c,
        topLeft = Offset(scaled(3f), scaled(6f)),
        size = Size(scaled(18f), scaled(14f)),
        cornerRadius = CornerRadius(scaled(1.5f), scaled(1.5f)),
        style = s,
    )
    drawCircle(color = c, radius = scaled(2f), center = Offset(scaled(9f), scaled(13f)))
    val mountain = Path().apply {
        moveTo(scaled(14f), scaled(16f))
        lineTo(scaled(17f), scaled(12f))
        lineTo(scaled(21f), scaled(17f))
    }
    drawPath(mountain, c, style = s)
}

private fun DrawScope.drawVideo(c: Color, s: Stroke) {
    drawRoundRect(
        color = c,
        topLeft = Offset(scaled(3f), scaled(6f)),
        size = Size(scaled(14f), scaled(12f)),
        cornerRadius = CornerRadius(scaled(1.5f), scaled(1.5f)),
        style = s,
    )
    val triangle = Path().apply {
        moveTo(scaled(17f), scaled(9f))
        lineTo(scaled(22f), scaled(7f))
        lineTo(scaled(22f), scaled(17f))
        lineTo(scaled(17f), scaled(15f))
        close()
    }
    drawPath(triangle, c)
}

private fun DrawScope.drawBook(c: Color, s: Stroke) {
    val p = Path().apply {
        moveTo(scaled(4f), scaled(5f))
        lineTo(scaled(12f), scaled(7f))
        lineTo(scaled(20f), scaled(5f))
        lineTo(scaled(20f), scaled(19f))
        lineTo(scaled(12f), scaled(21f))
        lineTo(scaled(4f), scaled(19f))
        close()
        moveTo(scaled(12f), scaled(7f))
        lineTo(scaled(12f), scaled(21f))
    }
    drawPath(p, c, style = s)
}

private fun DrawScope.drawManga(c: Color, s: Stroke) {
    drawRect(color = c, topLeft = Offset(scaled(4f), scaled(4f)), size = Size(scaled(7f), scaled(16f)), style = s)
    drawRect(color = c, topLeft = Offset(scaled(13f), scaled(4f)), size = Size(scaled(7f), scaled(16f)), style = s)
}

private fun DrawScope.drawKey(c: Color, s: Stroke) {
    drawCircle(color = c, radius = scaled(3.2f), center = Offset(scaled(8f), scaled(12f)), style = s)
    val teeth = Path().apply {
        moveTo(scaled(11.2f), scaled(12f))
        lineTo(scaled(20f), scaled(12f))
        lineTo(scaled(20f), scaled(16f))
        moveTo(scaled(17f), scaled(12f))
        lineTo(scaled(17f), scaled(14.5f))
    }
    drawPath(teeth, c, style = s)
}

private fun DrawScope.drawAudio(c: Color) {
    drawRect(color = c, topLeft = Offset(scaled(4f), scaled(10f)), size = Size(scaled(2.5f), scaled(8f)))
    drawRect(color = c, topLeft = Offset(scaled(10.75f), scaled(6f)), size = Size(scaled(2.5f), scaled(14f)))
    drawRect(color = c, topLeft = Offset(scaled(17.5f), scaled(3f)), size = Size(scaled(2.5f), scaled(18f)))
}

private fun DrawScope.drawBackup(c: Color, s: Stroke) {
    val arrow = Path().apply {
        moveTo(scaled(12f), scaled(4f))
        lineTo(scaled(12f), scaled(14f))
        moveTo(scaled(8f), scaled(11f))
        lineTo(scaled(12f), scaled(15f))
        lineTo(scaled(16f), scaled(11f))
    }
    drawPath(arrow, c, style = s)
    val tray = Path().apply {
        moveTo(scaled(4f), scaled(17f))
        lineTo(scaled(4f), scaled(20f))
        lineTo(scaled(20f), scaled(20f))
        lineTo(scaled(20f), scaled(17f))
    }
    drawPath(tray, c, style = s)
}

private fun DrawScope.drawReader(c: Color, s: Stroke) {
    val p = Path().apply {
        moveTo(scaled(6f), scaled(4f))
        lineTo(scaled(18f), scaled(4f))
        lineTo(scaled(18f), scaled(20f))
        lineTo(scaled(12f), scaled(17f))
        lineTo(scaled(6f), scaled(20f))
        close()
    }
    drawPath(p, c, style = s)
}

private fun DrawScope.drawCrossPlatform(c: Color, s: Stroke) {
    drawRoundRect(
        color = c,
        topLeft = Offset(scaled(3f), scaled(3f)),
        size = Size(scaled(11f), scaled(11f)),
        cornerRadius = CornerRadius(scaled(1.5f), scaled(1.5f)),
        style = s,
    )
    drawRoundRect(
        color = c,
        topLeft = Offset(scaled(10f), scaled(10f)),
        size = Size(scaled(11f), scaled(11f)),
        cornerRadius = CornerRadius(scaled(1.5f), scaled(1.5f)),
    )
}

private fun DrawScope.drawCloud(c: Color, s: Stroke) {
    val p = Path().apply {
        moveTo(scaled(7f), scaled(17f))
        cubicTo(scaled(3f), scaled(17f), scaled(3f), scaled(9f), scaled(7f), scaled(9f))
        cubicTo(scaled(7f), scaled(4f), scaled(17f), scaled(4f), scaled(17f), scaled(10f))
        cubicTo(scaled(20.5f), scaled(10f), scaled(20.5f), scaled(17f), scaled(17f), scaled(17f))
        close()
    }
    drawPath(p, c, style = s)
}
