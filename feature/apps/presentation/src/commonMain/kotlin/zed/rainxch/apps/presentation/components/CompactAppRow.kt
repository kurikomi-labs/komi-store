@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalTime::class)

package zed.rainxch.apps.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.model.AppItem
import zed.rainxch.apps.presentation.model.UpdateState
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.advanced_settings_open
import zed.rainxch.githubstore.core.presentation.res.apps_compact_more_actions
import zed.rainxch.githubstore.core.presentation.res.apps_compact_status_filter_active
import zed.rainxch.githubstore.core.presentation.res.apps_compact_status_pending_install
import zed.rainxch.githubstore.core.presentation.res.apps_compact_status_pre_release_on
import zed.rainxch.githubstore.core.presentation.res.apps_compact_status_ready_to_install
import zed.rainxch.githubstore.core.presentation.res.apps_compact_status_variant_pinned
import zed.rainxch.githubstore.core.presentation.res.apps_compact_status_variant_stale
import zed.rainxch.githubstore.core.presentation.res.apps_ignore_updates
import zed.rainxch.githubstore.core.presentation.res.apps_skip_version_unskip
import zed.rainxch.githubstore.core.presentation.res.install
import zed.rainxch.githubstore.core.presentation.res.open
import zed.rainxch.githubstore.core.presentation.res.pre_release_badge
import zed.rainxch.githubstore.core.presentation.res.uninstall
import zed.rainxch.githubstore.core.presentation.res.discard_pending_install
import zed.rainxch.githubstore.core.presentation.res.variant_picker_open
import kotlin.time.ExperimentalTime

/**
 * 64dp single-line row used in the "Up to date" section. Drops the verbose
 * controls of [zed.rainxch.apps.presentation.AppItemCard] (filter / variant /
 * pre-release / inline status text). Per-app configuration moves into the
 * trailing overflow menu, which routes to the existing bottom sheet.
 *
 * Accessibility: the entire row carries a single merged semantic name that
 * surfaces every "hidden" flag (filter active / variant pinned / pre-release
 * on / variant stale / pending install / ready to install) so screen-reader
 * users don't lose context that the dot cluster encodes visually.
 */
@Composable
fun CompactAppRow(
    appItem: AppItem,
    onOpenClick: () -> Unit,
    onInstallPendingClick: () -> Unit,
    onDiscardPendingClick: () -> Unit,
    onAdvancedSettingsClick: () -> Unit,
    onPickVariantClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onTogglePreReleases: (Boolean) -> Unit,
    onToggleUpdateCheck: (Boolean) -> Unit,
    onUnskipVersionClick: () -> Unit,
    onRowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = appItem.installedApp
    val isBusy =
        appItem.updateState is UpdateState.Downloading ||
            appItem.updateState is UpdateState.Installing ||
            appItem.updateState is UpdateState.CheckingUpdate

    val flags = rememberCompactStatusFlags(appItem)
    val rowSemanticName = buildCompactRowSemantics(app.appName, app.installedVersion, flags)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onRowClick)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = rowSemanticName
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        InstalledAppIcon(
            packageName = app.packageName,
            appName = app.appName,
            modifier =
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp)),
            apkFilePath = app.pendingInstallFilePath,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                app.sourceHost?.let { SourceChip(host = it) }
                Text(
                    text = app.installedVersion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                StatusDotCluster(flags = flags)
            }
        }

        if (app.pendingInstallFilePath != null) {
            // One-tap path for a parked download — surface the install
            // primary CTA even in compact mode because the file is on disk
            // and finishing the install is the user's expected action.
            Button(
                onClick = onInstallPendingClick,
                enabled = !isBusy,
                contentPadding = PaddingValues(horizontal = 12.dp),
                modifier = Modifier.height(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Update,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(Res.string.install),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Spacer(Modifier.width(4.dp))
        } else if (app.isPendingInstall) {
            // Pending row whose file isn't (or is no longer) on disk.
            // The app isn't installed; suppress the Open shortcut so we
            // don't dead-end on a launch failure. Discard is reachable
            // from the overflow.
        } else if (!isBusy) {
            // Subtle Open shortcut keeps the most-frequent action one tap
            // away even though the row itself opens the repo on tap.
            IconButton(
                onClick = onOpenClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = stringResource(Res.string.open),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        CompactRowOverflow(
            appName = app.appName,
            isBusy = isBusy,
            isPending = app.isPendingInstall,
            isUpdateAvailable = app.isUpdateAvailable,
            isPreReleaseEnabled = app.includePreReleases,
            isUpdateCheckEnabled = app.updateCheckEnabled,
            hasSkippedReleaseTag = app.skippedReleaseTag != null,
            onAdvancedSettingsClick = onAdvancedSettingsClick,
            onPickVariantClick = onPickVariantClick,
            onUninstallClick = onUninstallClick,
            onTogglePreReleases = onTogglePreReleases,
            onToggleUpdateCheck = onToggleUpdateCheck,
            onDiscardPendingClick = onDiscardPendingClick,
            onUnskipVersionClick = onUnskipVersionClick,
        )
    }
}

@Composable
private fun CompactRowOverflow(
    appName: String,
    isBusy: Boolean,
    isPending: Boolean,
    isUpdateAvailable: Boolean,
    isPreReleaseEnabled: Boolean,
    isUpdateCheckEnabled: Boolean,
    hasSkippedReleaseTag: Boolean,
    onAdvancedSettingsClick: () -> Unit,
    onPickVariantClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onTogglePreReleases: (Boolean) -> Unit,
    onToggleUpdateCheck: (Boolean) -> Unit,
    onDiscardPendingClick: () -> Unit,
    onUnskipVersionClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val moreActionsLabel = stringResource(Res.string.apps_compact_more_actions, appName)

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier =
                Modifier
                    .size(40.dp)
                    .semantics {
                        contentDescription = moreActionsLabel
                        role = Role.Button
                    },
            enabled = !isBusy,
        ) {
            Icon(
                imageVector = Icons.Outlined.MoreVert,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.advanced_settings_open)) },
                onClick = {
                    expanded = false
                    onAdvancedSettingsClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.variant_picker_open)) },
                onClick = {
                    expanded = false
                    onPickVariantClick()
                },
            )
            DropdownMenuItem(
                text = {
                    val baseLabel = stringResource(Res.string.pre_release_badge)
                    Text(text = if (isPreReleaseEnabled) "$baseLabel  ✓" else baseLabel)
                },
                onClick = {
                    expanded = false
                    onTogglePreReleases(!isPreReleaseEnabled)
                },
            )
            DropdownMenuItem(
                text = {
                    val baseLabel = stringResource(Res.string.apps_ignore_updates)
                    Text(text = if (!isUpdateCheckEnabled) "$baseLabel  ✓" else baseLabel)
                },
                onClick = {
                    expanded = false
                    onToggleUpdateCheck(!isUpdateCheckEnabled)
                },
            )
            if (hasSkippedReleaseTag) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.apps_skip_version_unskip)) },
                    onClick = {
                        expanded = false
                        onUnskipVersionClick()
                    },
                )
            }
            if (isPending) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(Res.string.discard_pending_install),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        expanded = false
                        onDiscardPendingClick()
                    },
                )
            } else {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(Res.string.uninstall),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        expanded = false
                        onUninstallClick()
                    },
                )
            }
        }
    }
    // Suppress unused-parameter warning; isUpdateAvailable reserved for
    // future Update CTA in compact mode if we ever want it.
    @Suppress("UNUSED_EXPRESSION") isUpdateAvailable
}
