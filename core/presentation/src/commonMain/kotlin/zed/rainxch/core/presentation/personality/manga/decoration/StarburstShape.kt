package zed.rainxch.core.presentation.personality.manga.decoration

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class StarburstShape(
    private val spikes: Int = 12,
    private val innerRatio: Float = 0.66f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outer = min(cx, cy)
        val inner = outer * innerRatio
        // alternate outer/inner vertices, π/spikes apart, starting at top (−90°)
        val step = (PI / spikes).toFloat()
        var angle = (-PI / 2.0).toFloat()
        val path = Path()
        for (i in 0 until spikes * 2) {
            val r = if (i % 2 == 0) outer else inner
            val x = cx + cos(angle) * r
            val y = cy + sin(angle) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            angle += step
        }
        path.close()
        return Outline.Generic(path)
    }
}
