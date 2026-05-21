package zed.rainxch.core.presentation.vocabulary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.LocalStatusColors

/** Signing-fingerprint trust state. */
enum class WaxSealState { INTACT, CRACKED, OPEN }

/**
 * Wax-stamp glyph for binary trust (DESIGN.md §7.8). INTACT = solid brown circle +
 * inner check; CRACKED = red circle + lightning split (the ONLY aggressive red);
 * OPEN = dashed grey ring (signing-fingerprint unknown). 22dp default; 36-44dp on
 * Detail trust card.
 */
@Composable
fun WaxSeal(
    state: WaxSealState,
    modifier: Modifier = Modifier,
    sizeDp: Int = 22,
) {
    val status = LocalStatusColors.current
    Canvas(modifier = modifier.size(sizeDp.dp)) {
        val s = size.minDimension
        val r = s * (9.5f / 24f)
        val center = Offset(size.width / 2f, size.height / 2f)
        when (state) {
            WaxSealState.INTACT -> {
                drawCircle(color = status.waxIntact, radius = r, center = center)
                drawCircle(
                    color = Color(0xFF5E2F18),
                    radius = r,
                    center = center,
                    style = Stroke(width = 0.5f.dp.toPx()),
                )
                drawCircle(
                    color = Color(0xFFFCE8C8).copy(alpha = 0.55f),
                    radius = s * (6f / 24f),
                    center = center,
                    style = Stroke(width = 0.6f.dp.toPx()),
                )
                // Inner check mark
                val path = Path().apply {
                    moveTo(size.width * (8.5f / 24f), size.height * (12f / 24f))
                    lineTo(size.width * (11f / 24f), size.height * (14.3f / 24f))
                    lineTo(size.width * (15.5f / 24f), size.height * (9.7f / 24f))
                }
                drawPath(
                    path = path,
                    color = Color(0xFFFCE8C8),
                    style = Stroke(
                        width = 1.8f.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
            WaxSealState.CRACKED -> {
                drawCircle(color = status.waxCracked, radius = r, center = center)
                val crackPath = Path().apply {
                    moveTo(size.width * (9f / 24f), size.height * (4f / 24f))
                    lineTo(size.width * (13f / 24f), size.height * (11f / 24f))
                    lineTo(size.width * (8f / 24f), size.height * (14f / 24f))
                    lineTo(size.width * (14f / 24f), size.height * (20f / 24f))
                }
                drawPath(
                    path = crackPath,
                    color = Color(0xFFFBE2DD),
                    style = Stroke(
                        width = 2f.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
            WaxSealState.OPEN -> {
                val dashEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(2.5f.dp.toPx(), 2f.dp.toPx()),
                )
                drawCircle(
                    color = status.waxOpen,
                    radius = s * (9f / 24f),
                    center = center,
                    style = Stroke(width = 1.6f.dp.toPx(), pathEffect = dashEffect),
                )
                drawCircle(
                    color = status.waxOpen.copy(alpha = 0.5f),
                    radius = s * (2f / 24f),
                    center = center,
                )
            }
        }
    }
}
