package zed.rainxch.core.presentation.vocabulary

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * 9-petal Material 3 Expressive flower silhouette. Used at exactly 3 touchpoints
 * (DESIGN.md §4.3 + §5.3): brand "G" mark, user avatar tile, active bottom-nav tab.
 *
 * Path translated from `tokens.json.shape.cookie.path` (viewBox 100×100), rescaled
 * to fill the [Size] passed by Compose. Used via `Modifier.clip(CookieShape)` or as
 * a `Shape` parameter on `Surface`/`Box`.
 */
object CookieShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val sx = size.width / 100f
        val sy = size.height / 100f
        val path = Path().apply {
            moveTo(50f * sx, 4f * sy)
            cubicTo(62f * sx, 4f * sy, 66f * sx, 12f * sy, 76f * sx, 12f * sy)
            cubicTo(86f * sx, 12f * sy, 91f * sx, 22f * sy, 91f * sx, 32f * sy)
            cubicTo(95f * sx, 40f * sy, 100f * sx, 50f * sy, 94f * sx, 58f * sy)
            cubicTo(96f * sx, 70f * sy, 90f * sx, 82f * sy, 80f * sx, 86f * sy)
            cubicTo(72f * sx, 90f * sy, 64f * sx, 96f * sy, 54f * sx, 96f * sy)
            cubicTo(44f * sx, 96f * sy, 36f * sx, 95f * sy, 26f * sx, 92f * sy)
            cubicTo(16f * sx, 90f * sy, 10f * sx, 80f * sy, 8f * sx, 70f * sy)
            cubicTo(4f * sx, 62f * sy, 0f, 54f * sy, 6f * sx, 46f * sy)
            cubicTo(6f * sx, 34f * sy, 12f * sx, 22f * sy, 22f * sx, 18f * sy)
            cubicTo(32f * sx, 12f * sy, 38f * sx, 4f * sy, 50f * sx, 4f * sy)
            close()
        }
        return Outline.Generic(path)
    }
}
