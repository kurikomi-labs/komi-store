package zed.rainxch.core.presentation.personality.manga.decoration

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Grid-paper backdrop: faint 1px onSurface lines on a square cell, drawn behind content
// (Manga Design Spec — the "paper" the panels sit on). Opacity is the paper's gridOpacity.
fun Modifier.gridPaper(
    color: Color,
    opacity: Float,
    cell: Dp = 26.dp,
): Modifier =
    drawBehind {
        val step = cell.toPx()
        val stroke = 1.dp.toPx()
        var x = 0f
        while (x <= size.width) {
            drawLine(color = color, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = stroke, alpha = opacity)
            x += step
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(color = color, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = stroke, alpha = opacity)
            y += step
        }
    }
