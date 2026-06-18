package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.overlays.KomiSheet
import zed.rainxch.core.presentation.components.overlays.KomiSheetPlacement
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.cancel
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_browsing_history_section
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_clear_viewed_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_clear_viewed_dialog_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_clear_viewed_dialog_confirm
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_clear_viewed_dialog_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_clear_viewed_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_clipboard_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_clipboard_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_hide_seen_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_hide_seen_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_hidden_repos_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_privacy_hidden_repos_title
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState

@Composable
fun privacySectionContent(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToHiddenRepositories: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ToggleCard(
            title = stringResource(Res.string.tweaks_privacy_clipboard_title),
            subtitle = stringResource(Res.string.tweaks_privacy_clipboard_body),
            checked = state.autoDetectClipboardLinks,
            onCheckedChange = {
                onAction(TweaksAction.OnAutoDetectClipboardToggled(it))
            },
        )
        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.tweaks_privacy_browsing_history_section),
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
        )

        ToggleCard(
            title = stringResource(Res.string.tweaks_privacy_hide_seen_title),
            subtitle = stringResource(Res.string.tweaks_privacy_hide_seen_body),
            checked = state.isHideSeenEnabled,
            onCheckedChange = { onAction(TweaksAction.OnHideSeenToggled(it)) },
        )
        Spacer(Modifier.height(8.dp))

        DestructiveRow(
            title = stringResource(Res.string.tweaks_privacy_clear_viewed_title),
            subtitle = stringResource(Res.string.tweaks_privacy_clear_viewed_body),
            onClick = { onAction(TweaksAction.OnClearSeenHistoryRequest) },
        )
        Spacer(Modifier.height(8.dp))

        DrillRow(
            icon = Icons.Outlined.VisibilityOff,
            title = stringResource(Res.string.tweaks_privacy_hidden_repos_title),
            subtitle = stringResource(Res.string.tweaks_privacy_hidden_repos_body),
            accent = MaterialTheme.colorScheme.primary,
            onClick = onNavigateToHiddenRepositories,
        )
    }

    if (state.isClearSeenHistoryDialogVisible) {
        val onDismiss = { onAction(TweaksAction.OnClearSeenHistoryDismiss) }
        KomiSheet(
            onDismiss = onDismiss,
            placement = KomiSheetPlacement.Center,
            title = stringResource(Res.string.tweaks_privacy_clear_viewed_dialog_title),
            footer = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KomiButton(
                        onClick = onDismiss,
                        label = stringResource(Res.string.cancel),
                        variant = KomiButtonVariant.Text,
                    )
                    KomiButton(
                        onClick = { onAction(TweaksAction.OnClearSeenHistoryConfirm) },
                        label = stringResource(Res.string.tweaks_privacy_clear_viewed_dialog_confirm),
                        variant = KomiButtonVariant.Destructive,
                    )
                }
            },
        ) {
            KomiText(
                text = stringResource(Res.string.tweaks_privacy_clear_viewed_dialog_body),
                role = KomiTextRole.Body,
            )
        }
    }
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val rowShape = RoundedCornerShape(LocalPersonality.current.shape.corner)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .clickable { onCheckedChange(!checked) },
        shape = rowShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        }
    }
}

@Composable
private fun DrillRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accent: Color = Color.Unspecified,
) {
    val tileBg = if (accent == Color.Unspecified) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        accent.copy(alpha = 0.14f)
    }
    val tint = if (accent == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        accent
    }
    val rowShape = RoundedCornerShape(LocalPersonality.current.shape.corner)
    val chipShape = RoundedCornerShape(LocalPersonality.current.shape.cornerSmall)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .clickable(onClick = onClick),
        shape = rowShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(chipShape)
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DestructiveRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val rowShape = RoundedCornerShape(LocalPersonality.current.shape.corner)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .clickable(onClick = onClick),
        shape = rowShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
