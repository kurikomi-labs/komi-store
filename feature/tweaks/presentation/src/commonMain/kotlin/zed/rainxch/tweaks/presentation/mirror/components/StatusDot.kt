package zed.rainxch.tweaks.presentation.mirror.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import zed.rainxch.core.domain.model.mirror.MirrorStatus

@Composable
fun StatusDot(
    status: MirrorStatus,
    modifier: Modifier = Modifier,
) {
    val color =
        when (status) {
            MirrorStatus.OK -> MaterialTheme.colorScheme.primary
            MirrorStatus.DEGRADED -> MaterialTheme.colorScheme.tertiary
            MirrorStatus.DOWN -> MaterialTheme.colorScheme.error
            MirrorStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
        }
    Box(
        modifier =
            modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
    )
}
