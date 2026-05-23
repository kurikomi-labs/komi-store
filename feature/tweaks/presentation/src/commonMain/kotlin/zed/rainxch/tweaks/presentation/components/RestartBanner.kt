package zed.rainxch.tweaks.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.RestartReason
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.restart_banner_body
import zed.rainxch.githubstore.core.presentation.res.restart_banner_later
import zed.rainxch.githubstore.core.presentation.res.restart_banner_reason_language
import zed.rainxch.githubstore.core.presentation.res.restart_banner_reason_telemetry
import zed.rainxch.githubstore.core.presentation.res.restart_banner_reason_theme
import zed.rainxch.githubstore.core.presentation.res.restart_banner_reasons_prefix
import zed.rainxch.githubstore.core.presentation.res.restart_banner_restart_now

@Composable
fun RestartBanner(
    reasons: Set<RestartReason>,
    onRestartNow: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (reasons.isEmpty()) return

    val reasonLabels = reasons.map { reason ->
        stringResource(
            when (reason) {
                RestartReason.LANGUAGE -> Res.string.restart_banner_reason_language
                RestartReason.THEME_MIGRATION -> Res.string.restart_banner_reason_theme
                RestartReason.TELEMETRY_TOGGLE -> Res.string.restart_banner_reason_telemetry
            },
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.restart_banner_body),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (reasonLabels.isNotEmpty()) {
                Text(
                    text = stringResource(
                        Res.string.restart_banner_reasons_prefix,
                        reasonLabels.joinToString(", "),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GhsButton(
                    onClick = onLater,
                    label = stringResource(Res.string.restart_banner_later),
                    variant = GhsButtonVariant.Text,
                    size = GhsButtonSize.Sm,
                )
                Spacer(Modifier.height(0.dp))
                GhsButton(
                    onClick = onRestartNow,
                    label = stringResource(Res.string.restart_banner_restart_now),
                    variant = GhsButtonVariant.Primary,
                    size = GhsButtonSize.Sm,
                )
            }
        }
    }
}
