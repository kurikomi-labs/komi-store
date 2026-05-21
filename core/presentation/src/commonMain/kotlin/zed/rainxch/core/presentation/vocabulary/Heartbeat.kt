package zed.rainxch.core.presentation.vocabulary

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.LocalMotion
import zed.rainxch.core.presentation.theme.LocalStatusColors

@Composable
fun Heartbeat(
    daysSinceCommit: Int,
    modifier: Modifier = Modifier,
    sizeDp: Int = 8,
) {
    val status = LocalStatusColors.current
    val motion = LocalMotion.current
    val periodMs = heartbeatPeriodMs(daysSinceCommit)
    val color = heartbeatColor(daysSinceCommit, status)

    if (periodMs == null) {

        Box(
            modifier = modifier
                .size(sizeDp.dp)
                .background(color.copy(alpha = 0.4f), CircleShape),
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "heartbeat")
    val scale by transition.animateFloat(
        initialValue = motion.heartbeatScaleFrom,
        targetValue = motion.heartbeatScaleTo,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "heartbeat-scale",
    )
    val haloScale by transition.animateFloat(
        initialValue = motion.heartbeatHaloFromScale,
        targetValue = motion.heartbeatHaloToScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "heartbeat-halo-scale",
    )
    val haloAlpha by transition.animateFloat(
        initialValue = motion.heartbeatHaloFromAlpha,
        targetValue = motion.heartbeatHaloToAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "heartbeat-halo-alpha",
    )

    Box(
        modifier = modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center,
    ) {

        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .drawBehind {
                    val r = (size.minDimension / 2f) * haloScale
                    drawCircle(
                        color = color.copy(alpha = haloAlpha),
                        radius = r,
                        center = Offset(size.width / 2f, size.height / 2f),
                    )
                },
        )

        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(color, CircleShape),
        )
    }
}

private fun heartbeatPeriodMs(days: Int): Int? = when {
    days <= 1 -> 1400
    days <= 7 -> 2400
    days <= 30 -> 4200
    else -> null
}

private fun heartbeatColor(days: Int, status: zed.rainxch.core.presentation.theme.StatusColors): Color = when {
    days <= 7 -> status.freshnessFresh
    days <= 30 -> status.freshnessWarm
    else -> status.freshnessDormant
}
