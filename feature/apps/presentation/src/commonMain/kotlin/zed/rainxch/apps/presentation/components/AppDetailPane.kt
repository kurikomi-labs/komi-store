@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package zed.rainxch.apps.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.model.AppItem
import zed.rainxch.apps.presentation.model.UpdateState
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.apps_compact_status_pending_install
import zed.rainxch.githubstore.core.presentation.res.apps_compact_status_pre_release_on
import zed.rainxch.githubstore.core.presentation.res.apps_compact_status_ready_to_install
import zed.rainxch.githubstore.core.presentation.res.apps_compact_status_updates_ignored
import zed.rainxch.githubstore.core.presentation.res.apps_ignore_updates
import zed.rainxch.githubstore.core.presentation.res.apps_two_pane_detail_section_actions
import zed.rainxch.githubstore.core.presentation.res.apps_two_pane_detail_section_settings
import zed.rainxch.githubstore.core.presentation.res.apps_two_pane_detail_section_status
import zed.rainxch.githubstore.core.presentation.res.apps_two_pane_empty_subtitle
import zed.rainxch.githubstore.core.presentation.res.apps_two_pane_empty_title
import zed.rainxch.githubstore.core.presentation.res.apps_two_pane_advanced_settings
import zed.rainxch.githubstore.core.presentation.res.apps_two_pane_installed_label
import zed.rainxch.githubstore.core.presentation.res.apps_two_pane_latest_label
import zed.rainxch.githubstore.core.presentation.res.apps_two_pane_open_repo
import zed.rainxch.githubstore.core.presentation.res.apps_two_pane_pick_variant
import zed.rainxch.githubstore.core.presentation.res.cancel
import zed.rainxch.githubstore.core.presentation.res.discard_pending_install
import zed.rainxch.githubstore.core.presentation.res.downloading
import zed.rainxch.githubstore.core.presentation.res.error_with_message
import zed.rainxch.githubstore.core.presentation.res.install
import zed.rainxch.githubstore.core.presentation.res.installing
import zed.rainxch.githubstore.core.presentation.res.open
import zed.rainxch.githubstore.core.presentation.res.pre_release_badge
import zed.rainxch.githubstore.core.presentation.res.uninstall
import zed.rainxch.githubstore.core.presentation.res.update
import zed.rainxch.githubstore.core.presentation.res.variant_label_inline

@Composable
fun AppDetailPane(
    appItem: AppItem?,
    onOpenApp: () -> Unit,
    onUpdateApp: () -> Unit,
    onCancelUpdate: () -> Unit,
    onUninstall: () -> Unit,
    onOpenRepo: () -> Unit,
    onTogglePreReleases: (Boolean) -> Unit,
    onToggleUpdateCheck: (Boolean) -> Unit,
    onOpenAdvancedSettings: () -> Unit,
    onPickVariant: () -> Unit,
    onInstallPending: () -> Unit,
    onDiscardPending: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (appItem == null) {
        EmptyDetailPane(modifier = modifier)
        return
    }

    val app = appItem.installedApp
    val isBusy = app.isPendingInstall ||
        appItem.updateState is UpdateState.Downloading ||
        appItem.updateState is UpdateState.Installing ||
        appItem.updateState is UpdateState.CheckingUpdate

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.height(8.dp))

        DetailHeader(appItem = appItem)

        Spacer(Modifier.height(16.dp))

        SectionLabel(stringResource(Res.string.apps_two_pane_detail_section_status))

        StatusBlock(appItem = appItem)

        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(Res.string.apps_two_pane_detail_section_actions))

        PrimaryActionsRow(
            appItem = appItem,
            isBusy = isBusy,
            onOpenApp = onOpenApp,
            onUpdateApp = onUpdateApp,
            onCancelUpdate = onCancelUpdate,
            onInstallPending = onInstallPending,
            onDiscardPending = onDiscardPending,
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KomiButton(
                onClick = onOpenRepo,
                label = stringResource(Res.string.apps_two_pane_open_repo),
                variant = KomiButtonVariant.Outline,
                leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                modifier = Modifier.weight(1f),
            )

            KomiButton(
                onClick = onUninstall,
                label = stringResource(Res.string.uninstall),
                variant = KomiButtonVariant.Destructive,
                enabled = !isBusy,
                leadingIcon = Icons.Outlined.DeleteOutline,
            )
        }

        Spacer(Modifier.height(20.dp))

        SectionLabel(stringResource(Res.string.apps_two_pane_detail_section_settings))

        SettingsBlock(
            appItem = appItem,
            onTogglePreReleases = onTogglePreReleases,
            onToggleUpdateCheck = onToggleUpdateCheck,
            onOpenAdvancedSettings = onOpenAdvancedSettings,
            onPickVariant = onPickVariant,
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DetailHeader(appItem: AppItem) {
    val app = appItem.installedApp
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            InstalledAppIcon(
                packageName = app.packageName,
                appName = app.appName,
                apkFilePath = app.pendingInstallFilePath,
                modifier = Modifier.size(56.dp),
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "${app.repoOwner}/${app.repoName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StatusBlock(appItem: AppItem) {
    val app = appItem.installedApp
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            StatusRow(
                label = stringResource(Res.string.apps_two_pane_installed_label),
                value = app.installedVersion,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                modifier = Modifier.padding(vertical = 10.dp),
            )

            StatusRow(
                label = stringResource(Res.string.apps_two_pane_latest_label),
                value = app.latestVersion ?: "—",
            )

            if (app.preferredAssetVariant != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    modifier = Modifier.padding(vertical = 10.dp),
                )

                StatusRow(
                    label = stringResource(Res.string.variant_label_inline),
                    value = app.preferredAssetVariant,
                )
            }

            when (val state = appItem.updateState) {
                is UpdateState.Downloading -> {
                    Spacer(Modifier.height(14.dp))

                    StatusProgress(
                        label = stringResource(Res.string.downloading),
                        progress = null,
                    )
                }
                is UpdateState.Installing -> {
                    Spacer(Modifier.height(14.dp))

                    StatusProgress(
                        label = stringResource(Res.string.installing),
                        progress = null,
                    )
                }
                is UpdateState.Error -> {
                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = stringResource(Res.string.error_with_message, state.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> {}
            }

            val pills = buildList {
                if (app.isPendingInstall) add(stringResource(Res.string.apps_compact_status_pending_install))
                if (app.pendingInstallFilePath != null && !app.isPendingInstall) {
                    add(stringResource(Res.string.apps_compact_status_ready_to_install))
                }
                if (app.includePreReleases) add(stringResource(Res.string.apps_compact_status_pre_release_on))
                if (!app.updateCheckEnabled) add(stringResource(Res.string.apps_compact_status_updates_ignored))
            }
            if (pills.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pills.forEach { label ->
                        StatusPill(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusProgress(
    label: String,
    progress: Float?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(6.dp))

        if (progress != null) {
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatusPill(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun PrimaryActionsRow(
    appItem: AppItem,
    isBusy: Boolean,
    onOpenApp: () -> Unit,
    onUpdateApp: () -> Unit,
    onCancelUpdate: () -> Unit,
    onInstallPending: () -> Unit,
    onDiscardPending: () -> Unit,
) {
    val app = appItem.installedApp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (appItem.updateState) {
            is UpdateState.Downloading, is UpdateState.Installing, is UpdateState.CheckingUpdate -> {
                KomiButton(
                    onClick = onCancelUpdate,
                    label = stringResource(Res.string.cancel),
                    variant = KomiButtonVariant.Destructive,
                    leadingIcon = Icons.Default.Cancel,
                    modifier = Modifier.weight(1f),
                )
            }
            else -> {
                if (app.pendingInstallFilePath != null) {
                    KomiButton(
                        onClick = onInstallPending,
                        label = stringResource(Res.string.install),
                        variant = KomiButtonVariant.Primary,
                        enabled = !isBusy,
                        leadingIcon = Icons.Default.Update,
                        modifier = Modifier.weight(1f),
                    )

                    KomiButton(
                        onClick = onDiscardPending,
                        label = stringResource(Res.string.discard_pending_install),
                        variant = KomiButtonVariant.Outline,
                    )
                } else if (app.isUpdateAvailable && !app.isPendingInstall) {
                    KomiButton(
                        onClick = onUpdateApp,
                        label = stringResource(Res.string.update),
                        variant = KomiButtonVariant.Primary,
                        leadingIcon = Icons.Default.Update,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    KomiButton(
                        onClick = onOpenApp,
                        label = stringResource(Res.string.open),
                        variant = KomiButtonVariant.Primary,
                        enabled = !isBusy,
                        leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsBlock(
    appItem: AppItem,
    onTogglePreReleases: (Boolean) -> Unit,
    onToggleUpdateCheck: (Boolean) -> Unit,
    onOpenAdvancedSettings: () -> Unit,
    onPickVariant: () -> Unit,
) {
    val app = appItem.installedApp
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            SettingsToggleRow(
                title = stringResource(Res.string.pre_release_badge),
                checked = app.includePreReleases,
                onCheckedChange = onTogglePreReleases,
            )

            DividerThin()

            SettingsToggleRow(
                title = stringResource(Res.string.apps_ignore_updates),
                checked = !app.updateCheckEnabled,
                onCheckedChange = { onToggleUpdateCheck(!it) },
            )

            DividerThin()

            SettingsActionRow(
                title = stringResource(Res.string.apps_two_pane_pick_variant),
                onClick = onPickVariant,
            )

            DividerThin()

            SettingsActionRow(
                title = stringResource(Res.string.apps_two_pane_advanced_settings),
                onClick = onOpenAdvancedSettings,
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )

            Spacer(Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun DividerThin() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
    )
}

@Composable
private fun EmptyDetailPane(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(48.dp),
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.apps_two_pane_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(Res.string.apps_two_pane_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
