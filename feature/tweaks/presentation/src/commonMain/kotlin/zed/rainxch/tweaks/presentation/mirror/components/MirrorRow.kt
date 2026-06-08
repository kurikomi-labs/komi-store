package zed.rainxch.tweaks.presentation.mirror.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.mirror.MirrorConfig
import zed.rainxch.core.domain.model.mirror.MirrorStatus
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.mirror_status_degraded
import zed.rainxch.githubstore.core.presentation.res.mirror_status_down
import zed.rainxch.githubstore.core.presentation.res.mirror_status_ok
import zed.rainxch.githubstore.core.presentation.res.mirror_status_unknown

@Composable
fun MirrorRow(
    mirror: MirrorConfig,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
                .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        StatusDot(status = mirror.status)
        Text(
            text = mirror.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 8.dp),
        )
        val label =
            when (mirror.status) {
                MirrorStatus.OK -> mirror.latencyMs?.let { stringResource(Res.string.mirror_status_ok, it) }
                MirrorStatus.DEGRADED -> mirror.latencyMs?.let { stringResource(Res.string.mirror_status_degraded, it) }
                MirrorStatus.DOWN -> stringResource(Res.string.mirror_status_down)
                MirrorStatus.UNKNOWN -> stringResource(Res.string.mirror_status_unknown)
            }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
