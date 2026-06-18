@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalTime::class)

package zed.rainxch.apps.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import zed.rainxch.core.presentation.locals.LocalPersonality
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.overlays.KomiDropdown
import zed.rainxch.core.presentation.components.overlays.KomiMenuItem
import zed.rainxch.core.presentation.components.overlays.KomiMenuTone
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.model.AppItem
import zed.rainxch.apps.presentation.model.UpdateState
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.advanced_settings_open
import zed.rainxch.githubstore.core.presentation.res.apps_compact_more_actions
import zed.rainxch.githubstore.core.presentation.res.apps_ignore_updates
import zed.rainxch.githubstore.core.presentation.res.apps_skip_version_unskip
import zed.rainxch.githubstore.core.presentation.res.install
import zed.rainxch.githubstore.core.presentation.res.open
import zed.rainxch.githubstore.core.presentation.res.pre_release_badge
import zed.rainxch.githubstore.core.presentation.res.uninstall
import zed.rainxch.githubstore.core.presentation.res.discard_pending_install
import zed.rainxch.githubstore.core.presentation.res.variant_picker_open
import kotlin.time.ExperimentalTime

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
    val rowShape = RoundedCornerShape(LocalPersonality.current.shape.corner)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .clip(rowShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = rowShape,
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onRowClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
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
            KomiButton(
                onClick = onInstallPendingClick,
                label = stringResource(Res.string.install),
                variant = KomiButtonVariant.Primary,
                size = KomiButtonSize.Sm,
                enabled = !isBusy,
                leadingIcon = Icons.Default.Update,
            )

            Spacer(Modifier.width(4.dp))
        } else if (app.isPendingInstall) {
        } else if (!isBusy) {
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
    val moreActionsLabel = stringResource(Res.string.apps_compact_more_actions, appName)

    val preReleaseBase = stringResource(Res.string.pre_release_badge)
    val ignoreUpdatesBase = stringResource(Res.string.apps_ignore_updates)
    val advancedSettingsLabel = stringResource(Res.string.advanced_settings_open)
    val variantPickerLabel = stringResource(Res.string.variant_picker_open)
    val unskipLabel = stringResource(Res.string.apps_skip_version_unskip)
    val discardLabel = stringResource(Res.string.discard_pending_install)
    val uninstallLabel = stringResource(Res.string.uninstall)

    val entries = buildList {
        add(KomiMenuItem(id = "advanced_settings", label = advancedSettingsLabel))
        add(KomiMenuItem(id = "variant_picker", label = variantPickerLabel))
        add(
            KomiMenuItem(
                id = "toggle_pre_releases",
                label = if (isPreReleaseEnabled) "$preReleaseBase  ✓" else preReleaseBase,
            ),
        )
        add(
            KomiMenuItem(
                id = "toggle_update_check",
                label = if (!isUpdateCheckEnabled) "$ignoreUpdatesBase  ✓" else ignoreUpdatesBase,
            ),
        )
        if (hasSkippedReleaseTag) {
            add(KomiMenuItem(id = "unskip_version", label = unskipLabel))
        }
        if (isPending) {
            add(
                KomiMenuItem(
                    id = "discard_pending",
                    label = discardLabel,
                    icon = Icons.Outlined.DeleteOutline,
                    tone = KomiMenuTone.Danger,
                ),
            )
        } else {
            add(
                KomiMenuItem(
                    id = "uninstall",
                    label = uninstallLabel,
                    icon = Icons.Outlined.DeleteOutline,
                    tone = KomiMenuTone.Danger,
                ),
            )
        }
    }.toImmutableList()

    KomiDropdown(
        entries = entries,
        onSelect = { item ->
            when (item.id) {
                "advanced_settings" -> onAdvancedSettingsClick()
                "variant_picker" -> onPickVariantClick()
                "toggle_pre_releases" -> onTogglePreReleases(!isPreReleaseEnabled)
                "toggle_update_check" -> onToggleUpdateCheck(!isUpdateCheckEnabled)
                "unskip_version" -> onUnskipVersionClick()
                "discard_pending" -> onDiscardPendingClick()
                "uninstall" -> onUninstallClick()
            }
        },
        trigger = { onClick ->
            IconButton(
                onClick = onClick,
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
        },
    )

    @Suppress("UNUSED_EXPRESSION") isUpdateAvailable
}
