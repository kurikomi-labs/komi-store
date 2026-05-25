package zed.rainxch.tweaks.presentation.feedback.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.feedback_diagnostics_header
import zed.rainxch.githubstore.core.presentation.res.feedback_diagnostics_include
import zed.rainxch.tweaks.presentation.feedback.model.DiagnosticsInfo
import zed.rainxch.tweaks.presentation.feedback.model.FeedbackChannel

@Composable
fun DiagnosticsPreview(
    diagnostics: DiagnosticsInfo?,
    channel: FeedbackChannel,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = stringResource(Res.string.feedback_diagnostics_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.feedback_diagnostics_include),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle() },
                )
            }

            if (enabled && diagnostics != null) {
                Text(
                    text = formatDiagnostics(diagnostics, channel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}

private fun formatDiagnostics(d: DiagnosticsInfo, channel: FeedbackChannel): String {
    val sb = StringBuilder()
    sb.append("- App: GitHub Store v").append(d.appVersion).append('\n')
    sb.append("- Platform: ").append(d.platform).append(' ').append(d.osVersion).append('\n')
    sb.append("- Locale: ").append(d.locale).append('\n')
    sb.append("- Theme: ").append(d.themePalette).append(" / ").append(d.themeMode)
    d.installerType?.let { sb.append('\n').append("- Installer: ").append(it) }
    if (channel == FeedbackChannel.GITHUB) {
        d.githubUsername?.let { sb.append('\n').append("- GitHub user: @").append(it) }
    }
    return sb.toString()
}
