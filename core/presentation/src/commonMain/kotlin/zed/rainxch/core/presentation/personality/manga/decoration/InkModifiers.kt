package zed.rainxch.core.presentation.personality.manga.decoration

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

fun Modifier.hardShadow(
    offset: DpOffset,
    color: Color,
    shape: Shape = RectangleShape,
): Modifier =
    drawBehind {
        val dx = offset.x.toPx()
        val dy = offset.y.toPx()
        val outline = shape.createOutline(size, layoutDirection, this)
        translate(dx, dy) {
            drawOutline(outline = outline, color = color)
        }
    }

fun Modifier.hardShadow(
    color: Color,
    shape: Shape = RectangleShape,
    offset: DrawScope.() -> Offset,
): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        onDrawBehind {
            val o = offset()
            translate(o.x, o.y) {
                drawOutline(outline = outline, color = color)
            }
        }
    }

fun Modifier.screentone(
    color: Color,
    opacity: Float,
    dotRadius: Float = 1.1f,
    spacing: Float = 5f,
): Modifier =
    drawWithCache {
        onDrawWithContent {
            drawContent()
            var y = 0f
            while (y <= size.height) {
                var x = 0f
                while (x <= size.width) {
                    drawCircle(color = color, radius = dotRadius, center = Offset(x, y), alpha = opacity)
                    x += spacing
                }
                y += spacing
            }
        }
    }

fun Modifier.speedLines(
    color: Color,
    spokes: Int = 120,
    opacity: Float = 0.12f,
    strokeWidth: Float = 1.2f,
): Modifier =
    drawWithCache {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val reach = hypot(size.width, size.height)
        onDrawBehind {
            for (i in 0 until spokes) {
                val angle = (2.0 * kotlin.math.PI * i / spokes).toFloat()
                drawLine(
                    color = color,
                    start = Offset(cx, cy),
                    end = Offset(cx + cos(angle) * reach, cy + sin(angle) * reach),
                    strokeWidth = strokeWidth,
                    alpha = opacity,
                )
            }
        }
    }

// Top-right corner wash: a dot lattice radial-masked from the top-right corner (opaque at
// the corner, fading to nothing by 70% of the region diagonal). Draws behind content.
fun Modifier.screentoneCorner(
    color: Color,
    opacity: Float,
    boost: Float = 1f,
    regionWidth: Dp = 120.dp,
    regionHeight: Dp = 90.dp,
    dotRadius: Float = 1.1f,
    spacing: Float = 5f,
): Modifier =
    drawWithCache {
        val w = regionWidth.toPx()
        val h = regionHeight.toPx()
        val left = (size.width - w).coerceAtLeast(0f)
        val maskRadius = hypot(w, h) * 0.7f
        onDrawBehind {
            var y = 0f
            while (y <= h && y <= size.height) {
                var x = left
                while (x <= size.width) {
                    val falloff = (1f - hypot(size.width - x, y) / maskRadius).coerceIn(0f, 1f)
                    if (falloff > 0f) {
                        drawCircle(color = color, radius = dotRadius, center = Offset(x, y), alpha = opacity * boost * falloff)
                    }
                    x += spacing
                }
                y += spacing
            }
        }
    }

// Even dot field across the whole surface (stat-slab look). Draws behind content.
fun Modifier.screentoneFill(
    color: Color,
    opacity: Float,
    dotRadius: Float = 1f,
    spacing: Float = 6f,
): Modifier =
    drawWithCache {
        onDrawBehind {
            var y = 0f
            while (y <= size.height) {
                var x = 0f
                while (x <= size.width) {
                    drawCircle(color = color, radius = dotRadius, center = Offset(x, y), alpha = opacity)
                    x += spacing
                }
                y += spacing
            }
        }
    }
