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

@Composable
fun TopicGlyph(
    topic: String,
    modifier: Modifier = Modifier,
    sizeDp: Int = 14,
    color: Color = LocalContentColor.current,
) {
    val key = topic.lowercase()
    if (key !in Tokens.Topics.supported) return
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val sw = 1.7f.dp.toPx()
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (key) {
            "security" -> drawSecurity(color, stroke)
            "privacy" -> drawPrivacy(color, stroke)
            "networking" -> drawNetworking(color, stroke)
            "ai" -> drawAi(color, stroke)
            "notes" -> drawNotes(color, stroke)
            "audio" -> drawAudio(color)
            "video" -> drawVideo(color, stroke)
            "photo" -> drawPhoto(color, stroke)
            "reader" -> drawReader(color, stroke)
            "messaging" -> drawMessaging(color, stroke)
            "browser" -> drawBrowser(color, stroke)
            "self-hosted" -> drawSelfHosted(color, stroke)
            "backup" -> drawBackup(color, stroke)
            "social" -> drawSocial(color, stroke)
            "launcher" -> drawLauncher(color)
        }
    }
}

private fun DrawScope.scaled(viewBoxValue: Float) = viewBoxValue / 24f * size.minDimension

private fun DrawScope.drawSecurity(c: Color, s: Stroke) {

    val shackle = Path().apply {
        moveTo(scaled(8f), scaled(11f))
        lineTo(scaled(8f), scaled(8f))
        cubicTo(scaled(8f), scaled(4f), scaled(16f), scaled(4f), scaled(16f), scaled(8f))
        lineTo(scaled(16f), scaled(11f))
    }
    drawPath(shackle, c, style = s)
    drawRoundRect(
        color = c,
        topLeft = Offset(scaled(5.5f), scaled(11f)),
        size = Size(scaled(13f), scaled(9f)),
        cornerRadius = CornerRadius(scaled(1.5f), scaled(1.5f)),
        style = s,
    )

    drawCircle(color = c, radius = scaled(1.3f), center = Offset(scaled(12f), scaled(15f)))
}

private fun DrawScope.drawPrivacy(c: Color, s: Stroke) {

    val eye = Path().apply {
        moveTo(scaled(3f), scaled(12f))
        quadraticTo(scaled(12f), scaled(4.5f), scaled(21f), scaled(12f))
        quadraticTo(scaled(12f), scaled(19.5f), scaled(3f), scaled(12f))
        close()
    }
    drawPath(eye, c, style = s)
    drawCircle(color = c, radius = scaled(2.2f), center = Offset(scaled(12f), scaled(12f)))
    drawLine(
        color = c,
        start = Offset(scaled(4f), scaled(20f)),
        end = Offset(scaled(20f), scaled(4f)),
        strokeWidth = s.width * 1.4f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawNetworking(c: Color, s: Stroke) {

    drawArc(
        color = c,
        startAngle = 215f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(scaled(3f), scaled(7f)),
        size = Size(scaled(18f), scaled(18f)),
        style = s,
    )
    drawArc(
        color = c,
        startAngle = 215f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(scaled(6f), scaled(10f)),
        size = Size(scaled(12f), scaled(12f)),
        style = s,
    )
    drawArc(
        color = c,
        startAngle = 215f,
        sweepAngle = 110f,
        useCenter = false,
        topLeft = Offset(scaled(9f), scaled(13f)),
        size = Size(scaled(6f), scaled(6f)),
        style = s,
    )
    drawCircle(color = c, radius = scaled(1f), center = Offset(scaled(12f), scaled(19f)))
}

private fun DrawScope.drawAi(c: Color, s: Stroke) {

    val spark = Path().apply {
        moveTo(scaled(12f), scaled(3f))
        lineTo(scaled(14f), scaled(10f))
        lineTo(scaled(21f), scaled(12f))
        lineTo(scaled(14f), scaled(14f))
        lineTo(scaled(12f), scaled(21f))
        lineTo(scaled(10f), scaled(14f))
        lineTo(scaled(3f), scaled(12f))
        lineTo(scaled(10f), scaled(10f))
        close()
    }
    drawPath(spark, c, style = s)
}

private fun DrawScope.drawNotes(c: Color, s: Stroke) {

    drawRoundRect(
        color = c,
        topLeft = Offset(scaled(4f), scaled(5f)),
        size = Size(scaled(12f), scaled(16f)),
        cornerRadius = CornerRadius(scaled(1.5f), scaled(1.5f)),
        style = s,
    )
    val pencil = Path().apply {
        moveTo(scaled(15f), scaled(8f))
        lineTo(scaled(21f), scaled(2f))
        lineTo(scaled(22.5f), scaled(3.5f))
        lineTo(scaled(16.5f), scaled(9.5f))
        close()
    }
    drawPath(pencil, c, style = s)
    drawLine(
        color = c,
        start = Offset(scaled(7f), scaled(11f)),
        end = Offset(scaled(13f), scaled(11f)),
        strokeWidth = s.width,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = c,
        start = Offset(scaled(7f), scaled(15f)),
        end = Offset(scaled(13f), scaled(15f)),
        strokeWidth = s.width,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawMessaging(c: Color, s: Stroke) {

    drawRoundRect(
        color = c,
        topLeft = Offset(scaled(3f), scaled(4f)),
        size = Size(scaled(18f), scaled(13f)),
        cornerRadius = CornerRadius(scaled(2.5f), scaled(2.5f)),
        style = s,
    )
    val tail = Path().apply {
        moveTo(scaled(7f), scaled(17f))
        lineTo(scaled(6f), scaled(21f))
        lineTo(scaled(11f), scaled(17f))
        close()
    }
    drawPath(tail, c, style = s)
}

private fun DrawScope.drawBrowser(c: Color, s: Stroke) {

    drawCircle(color = c, radius = scaled(8f), center = Offset(scaled(12f), scaled(12f)), style = s)
    val needle = Path().apply {
        moveTo(scaled(12f), scaled(6f))
        lineTo(scaled(14.5f), scaled(13f))
        lineTo(scaled(12f), scaled(11.5f))
        lineTo(scaled(9.5f), scaled(13f))
        close()
    }
    drawPath(needle, c)
}

private fun DrawScope.drawSocial(c: Color, s: Stroke) {

    drawCircle(color = c, radius = scaled(2.4f), center = Offset(scaled(9f), scaled(8f)), style = s)
    drawCircle(color = c, radius = scaled(2.4f), center = Offset(scaled(16f), scaled(9f)), style = s)
    val shoulderA = Path().apply {
        moveTo(scaled(4.5f), scaled(20f))
        cubicTo(scaled(4.5f), scaled(14f), scaled(13.5f), scaled(14f), scaled(13.5f), scaled(20f))
    }
    drawPath(shoulderA, c, style = s)
    val shoulderB = Path().apply {
        moveTo(scaled(11.5f), scaled(20f))
        cubicTo(scaled(11.5f), scaled(15.5f), scaled(20.5f), scaled(15.5f), scaled(20.5f), scaled(20f))
    }
    drawPath(shoulderB, c, style = s)
}

private fun DrawScope.drawLauncher(c: Color) {

    val r = scaled(1.5f)
    listOf(7f, 12f, 17f).forEach { cx ->
        listOf(7f, 12f, 17f).forEach { cy ->
            drawCircle(color = c, radius = r, center = Offset(scaled(cx), scaled(cy)))
        }
    }
}

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
