package zed.rainxch.tweaks.presentation.privacy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.components.overlays.GhsConfirmDialog
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.theme.tokens.GhsAccents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.tweaks_entry_privacy
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
import zed.rainxch.tweaks.presentation.TweaksViewModel
import zed.rainxch.tweaks.presentation.components.TweaksSubScreenScaffold

@Composable
fun TweaksPrivacyRoot(
    onNavigateBack: () -> Unit,
    onNavigateToHiddenRepositories: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }

    TweaksSubScreenScaffold(
        title = stringResource(Res.string.tweaks_entry_privacy),
        onNavigateBack = onNavigateBack,
        snackbarState = snackbarState,
        restartReasons = state.needsRestartReasons,
        onRestartNow = { viewModel.onAction(TweaksAction.OnRestartNowClick) },
        onRestartLater = { viewModel.onAction(TweaksAction.OnRestartLaterClick) },
        showRestartBanner = state.restartBannerVisible,
    ) {

        item(key = "clipboard_card") {
            ToggleCard(
                title = stringResource(Res.string.tweaks_privacy_clipboard_title),
                subtitle = stringResource(Res.string.tweaks_privacy_clipboard_body),
                checked = state.autoDetectClipboardLinks,
                onCheckedChange = {
                    viewModel.onAction(TweaksAction.OnAutoDetectClipboardToggled(it))
                },
            )
            Spacer(Modifier.height(12.dp))
        }

        item(key = "history_header") {
            Text(
                text = stringResource(Res.string.tweaks_privacy_browsing_history_section),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
            )
        }

        item(key = "hide_seen_card") {
            ToggleCard(
                title = stringResource(Res.string.tweaks_privacy_hide_seen_title),
                subtitle = stringResource(Res.string.tweaks_privacy_hide_seen_body),
                checked = state.isHideSeenEnabled,
                onCheckedChange = { viewModel.onAction(TweaksAction.OnHideSeenToggled(it)) },
            )
            Spacer(Modifier.height(8.dp))
        }

        item(key = "clear_history_row") {
            DestructiveRow(
                title = stringResource(Res.string.tweaks_privacy_clear_viewed_title),
                subtitle = stringResource(Res.string.tweaks_privacy_clear_viewed_body),
                onClick = { viewModel.onAction(TweaksAction.OnClearSeenHistoryRequest) },
            )
            Spacer(Modifier.height(8.dp))
        }

        item(key = "hidden_repos_row") {
            DrillRow(
                icon = Icons.Outlined.VisibilityOff,
                title = stringResource(Res.string.tweaks_privacy_hidden_repos_title),
                subtitle = stringResource(Res.string.tweaks_privacy_hidden_repos_body),
                accent = GhsAccents.Periwinkle,
                onClick = onNavigateToHiddenRepositories,
            )
        }
    }

    if (state.isClearSeenHistoryDialogVisible) {
        GhsConfirmDialog(
            title = stringResource(Res.string.tweaks_privacy_clear_viewed_dialog_title),
            body = stringResource(Res.string.tweaks_privacy_clear_viewed_dialog_body),
            confirmLabel = stringResource(Res.string.tweaks_privacy_clear_viewed_dialog_confirm),
            destructive = true,
            onConfirm = { viewModel.onAction(TweaksAction.OnClearSeenHistoryConfirm) },
            onDismiss = { viewModel.onAction(TweaksAction.OnClearSeenHistoryDismiss) },
        )
    }
}

@Composable
private fun ToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.row)
            .clickable { onCheckedChange(!checked) },
        shape = Radii.row,
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.row)
            .clickable(onClick = onClick),
        shape = Radii.row,
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
                    .clip(Radii.chip)
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.row)
            .clickable(onClick = onClick),
        shape = Radii.row,
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
