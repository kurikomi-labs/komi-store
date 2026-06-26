package zed.rainxch.tweaks.presentation.mirror.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import zed.rainxch.core.domain.model.mirror.MirrorStatus
import zed.rainxch.core.presentation.locals.LocalPersonality

@Composable
fun StatusDot(
    status: MirrorStatus,
    modifier: Modifier = Modifier,
) {
    val personality = LocalPersonality.current
    val colors = personality.colors
    val color =
        when (status) {
            MirrorStatus.OK -> colors.primary
            MirrorStatus.DEGRADED -> colors.primary
            MirrorStatus.DOWN -> colors.error
            MirrorStatus.UNKNOWN -> colors.outline
        }
    Box(
        modifier =
            modifier
                .size(8.dp)
                .clip(RoundedCornerShape(personality.shape.cornerSmall))
                .background(color),
    )
}
