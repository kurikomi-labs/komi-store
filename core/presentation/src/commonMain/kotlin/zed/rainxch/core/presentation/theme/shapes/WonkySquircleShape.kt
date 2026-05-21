package zed.rainxch.core.presentation.theme.shapes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Hand-shaped asymmetric corner with elliptical (x ≠ y) radii — the "wonky squircle"
 * from DESIGN.md §5.2 / §7.7 / §16.x. Compose's `RoundedCornerShape` cannot express
 * elliptical corners (it's circular only), so this `Shape` builds a [Path] manually
 * using `arcTo` with rectangular bounds whose width and height are independent.
 *
 * Each corner takes `(rxDp, ryDp)` — the horizontal and vertical sweep of that corner's
 * ellipse arc. The CSS notation `border-radius: 20 14 22 16 / 16 22 14 20` translates to:
 *   topStart  = (20, 16)
 *   topEnd    = (14, 22)
 *   bottomEnd = (22, 14)
 *   bottomStart = (16, 20)
 *
 * Three preset constants ([CtaPrimary], [CtaAlt], [Search]) match the tokens.json
 * `shape.wonkySquircle` block. For ad-hoc corners (sheets / dialogs / toasts), use
 * the [WonkySquircleShape] constructor directly.
 */
class WonkySquircleShape(
    private val topStart: CornerRadii,
    private val topEnd: CornerRadii,
    private val bottomEnd: CornerRadii,
    private val bottomStart: CornerRadii,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        with(density) {
            val tsX = topStart.rx.toPx().coerceAtMost(size.width / 2f)
            val tsY = topStart.ry.toPx().coerceAtMost(size.height / 2f)
            val teX = topEnd.rx.toPx().coerceAtMost(size.width / 2f)
            val teY = topEnd.ry.toPx().coerceAtMost(size.height / 2f)
            val beX = bottomEnd.rx.toPx().coerceAtMost(size.width / 2f)
            val beY = bottomEnd.ry.toPx().coerceAtMost(size.height / 2f)
            val bsX = bottomStart.rx.toPx().coerceAtMost(size.width / 2f)
            val bsY = bottomStart.ry.toPx().coerceAtMost(size.height / 2f)

            val path = Path().apply {
                // Start at top-start corner end-point (after sweeping in)
                moveTo(tsX, 0f)
                // Top edge → top-end corner
                lineTo(size.width - teX, 0f)
                arcToCorner(
                    cornerCenter = Offset(size.width - teX, teY),
                    radiusX = teX,
                    radiusY = teY,
                    startAngle = 270f,
                    sweep = 90f,
                )
                // Right edge → bottom-end corner
                lineTo(size.width, size.height - beY)
                arcToCorner(
                    cornerCenter = Offset(size.width - beX, size.height - beY),
                    radiusX = beX,
                    radiusY = beY,
                    startAngle = 0f,
                    sweep = 90f,
                )
                // Bottom edge → bottom-start corner
                lineTo(bsX, size.height)
                arcToCorner(
                    cornerCenter = Offset(bsX, size.height - bsY),
                    radiusX = bsX,
                    radiusY = bsY,
                    startAngle = 90f,
                    sweep = 90f,
                )
                // Left edge → top-start corner
                lineTo(0f, tsY)
                arcToCorner(
                    cornerCenter = Offset(tsX, tsY),
                    radiusX = tsX,
                    radiusY = tsY,
                    startAngle = 180f,
                    sweep = 90f,
                )
                close()
            }
            return Outline.Generic(path)
        }
    }

    companion object {
        /** `tokens.json.shape.wonkySquircle.css`: 20 14 22 16 / 16 22 14 20 */
        val CtaPrimary = WonkySquircleShape(
            topStart = CornerRadii(20.dp, 16.dp),
            topEnd = CornerRadii(14.dp, 22.dp),
            bottomEnd = CornerRadii(22.dp, 14.dp),
            bottomStart = CornerRadii(16.dp, 20.dp),
        )

        /** `tokens.json.shape.wonkySquircle.alt`: 22 16 24 18 / 18 24 16 22 */
        val CtaAlt = WonkySquircleShape(
            topStart = CornerRadii(22.dp, 18.dp),
            topEnd = CornerRadii(16.dp, 24.dp),
            bottomEnd = CornerRadii(24.dp, 16.dp),
            bottomStart = CornerRadii(18.dp, 22.dp),
        )

        /** `tokens.json.shape.wonkySquircle.search`: 24 18 26 20 / 18 24 20 26 */
        val Search = WonkySquircleShape(
            topStart = CornerRadii(24.dp, 18.dp),
            topEnd = CornerRadii(18.dp, 24.dp),
            bottomEnd = CornerRadii(26.dp, 20.dp),
            bottomStart = CornerRadii(20.dp, 26.dp),
        )

        /** Sheet — square bottom corners (flush to screen edge). */
        val Sheet = WonkySquircleShape(
            topStart = CornerRadii(24.dp, 18.dp),
            topEnd = CornerRadii(18.dp, 24.dp),
            bottomEnd = CornerRadii(0.dp, 0.dp),
            bottomStart = CornerRadii(0.dp, 0.dp),
        )

        /** Dialog — symmetric-ish wonky. */
        val Dialog = WonkySquircleShape(
            topStart = CornerRadii(28.dp, 22.dp),
            topEnd = CornerRadii(22.dp, 28.dp),
            bottomEnd = CornerRadii(26.dp, 24.dp),
            bottomStart = CornerRadii(24.dp, 26.dp),
        )

        /** Toast — compact wonky. */
        val Toast = WonkySquircleShape(
            topStart = CornerRadii(18.dp, 14.dp),
            topEnd = CornerRadii(14.dp, 22.dp),
            bottomEnd = CornerRadii(22.dp, 16.dp),
            bottomStart = CornerRadii(16.dp, 18.dp),
        )
    }
}

data class CornerRadii(val rx: Dp, val ry: Dp)

private fun Path.arcToCorner(
    cornerCenter: Offset,
    radiusX: Float,
    radiusY: Float,
    startAngle: Float,
    sweep: Float,
) {
    if (radiusX == 0f || radiusY == 0f) return
    val bounds = Rect(
        left = cornerCenter.x - radiusX,
        top = cornerCenter.y - radiusY,
        right = cornerCenter.x + radiusX,
        bottom = cornerCenter.y + radiusY,
    )
    arcTo(
        rect = bounds,
        startAngleDegrees = startAngle,
        sweepAngleDegrees = sweep,
        forceMoveTo = false,
    )
}
