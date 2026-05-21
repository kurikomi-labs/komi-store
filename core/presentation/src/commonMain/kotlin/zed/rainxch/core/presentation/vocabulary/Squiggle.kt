package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Hand-drawn wavy underline. Default ~40×5 dp, 1.6dp stroke, primary color at 60%.
 * One per section heading (DESIGN.md §4.3). Path translated from
 * `tokens.json.shape.squiggle.path` (viewBox 40×5).
 */
@Composable
fun Squiggle(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
) {
    Canvas(modifier = modifier.size(width = 40.dp, height = 5.dp)) {
        val sx = size.width / 40f
        val sy = size.height / 5f
        val path = Path().apply {
            moveTo(1f * sx, 3f * sy)
            // Initial quadratic + four smooth quadratics with reflected control points
            // (T command in SVG): C1=(5,0.5), then reflect through each end-point.
            quadraticTo(5f * sx, 0.5f * sy, 9f * sx, 3f * sy)
            smoothQuadTo(17f * sx, 3f * sy, prevControl = Offset(5f * sx, 0.5f * sy), prevEnd = Offset(9f * sx, 3f * sy))
            smoothQuadTo(25f * sx, 3f * sy, prevControl = Offset(13f * sx, 5.5f * sy), prevEnd = Offset(17f * sx, 3f * sy))
            smoothQuadTo(33f * sx, 3f * sy, prevControl = Offset(21f * sx, 0.5f * sy), prevEnd = Offset(25f * sx, 3f * sy))
            smoothQuadTo(39f * sx, 3f * sy, prevControl = Offset(29f * sx, 5.5f * sy), prevEnd = Offset(33f * sx, 3f * sy))
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 1.6f.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

private fun Path.smoothQuadTo(toX: Float, toY: Float, prevControl: Offset, prevEnd: Offset) {
    val reflectedX = 2f * prevEnd.x - prevControl.x
    val reflectedY = 2f * prevEnd.y - prevControl.y
    quadraticTo(reflectedX, reflectedY, toX, toY)
}
