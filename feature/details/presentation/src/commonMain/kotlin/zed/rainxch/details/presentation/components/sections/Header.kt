package zed.rainxch.details.presentation.components.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.details.presentation.DetailsAction
import zed.rainxch.details.presentation.DetailsState
import zed.rainxch.details.presentation.components.AppHeader
import zed.rainxch.details.presentation.components.ReleaseAssetsPicker
import zed.rainxch.details.presentation.components.ReleasesStatus
import zed.rainxch.details.presentation.components.ReleasesStatusCard
import zed.rainxch.core.domain.model.isReallyInstalled
import zed.rainxch.details.presentation.components.ApkInspectSheet
import zed.rainxch.details.presentation.components.InspectApkButton
import zed.rainxch.details.presentation.components.SmartInstallButton
import zed.rainxch.details.presentation.components.VersionPicker
import zed.rainxch.details.presentation.components.VersionTypePicker
import zed.rainxch.details.presentation.model.DownloadStage
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.appmanager_description
import zed.rainxch.githubstore.core.presentation.res.external_installer_description
import zed.rainxch.githubstore.core.presentation.res.inspect_with_appmanager
import zed.rainxch.githubstore.core.presentation.res.obtainium_description
import zed.rainxch.githubstore.core.presentation.res.open_in_obtainium
import zed.rainxch.githubstore.core.presentation.res.open_with_external_installer

fun LazyListScope.header(
    state: DetailsState,
    onAction: (DetailsAction) -> Unit,
) {
    item {
        if (state.repository != null) {
            AppHeader(
                author = state.userProfile,
                release = state.selectedRelease,
                repository = state.repository,
                installedApp = state.installedApp,
                downloadStage = state.downloadStage,
                downloadProgress = state.downloadProgressPercent,
                isCurrentUserOwner = state.isCurrentUserOwner,
            )
        }
    }

    // Status card replaces the pickers + install button in three cases:
    //   1. releases fetch failed — show error + Retry
    //   2. retry in flight — show spinner
    //   3. repo truly has no releases — show "no releases published" empty state
    // Initial page load (isLoading) is intentionally excluded — the top-level
    // loading spinner covers it, no need to double up. Same for repository
    // not loaded yet: the release-specific states only make sense once we
    // know the repo exists (matches the VM's retryReleases() guard).
    val releasesStatus: ReleasesStatus? =
        when {
            state.repository == null -> null
            state.releasesLoadFailed -> ReleasesStatus.FAILED
            state.isRetryingReleases -> ReleasesStatus.RETRYING
            !state.isLoading && state.allReleases.isEmpty() -> ReleasesStatus.EMPTY
            else -> null
        }

    if (releasesStatus != null) {
        item {
            ReleasesStatusCard(
                status = releasesStatus,
                onRetry = { onAction(DetailsAction.RetryReleases) },
                modifier = Modifier.animateItem(),
            )
        }
    } else {
        // versions type list
        if (state.allReleases.isNotEmpty()) {
            item {
                VersionTypePicker(
                    selectedCategory = state.selectedReleaseCategory,
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth().animateItem(),
                )
            }
        }

        // version and installable release
        if (state.allReleases.isNotEmpty() || state.installableAssets.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReleaseAssetsPicker(
                        assetsList = state.installableAssets,
                        selectedAsset = state.primaryAsset,
                        isPickerVisible = state.isReleaseSelectorVisible,
                        pinnedVariant = state.installedApp?.preferredAssetVariant,
                        onAction = onAction,
                        modifier = Modifier.weight(.65f),
                    )
                    VersionPicker(
                        selectedRelease = state.selectedRelease,
                        filteredReleases = state.filteredReleases,
                        isPickerVisible = state.isVersionPickerVisible,
                        onAction = onAction,
                        modifier = Modifier.weight(.35f),
                    )
                }
            }
        }

        item {
            // Inspect button only surfaces once the package is genuinely
            // installed on device (`isReallyInstalled()` filters out
            // pending-install rows whose `installedApp` is non-null but
            // the system hasn't confirmed the install). This avoids
            // popping the icon in at the exact frame the system install
            // prompt appears, which is the user's peak-attention moment.
            val canInspectApk = state.installedApp?.isReallyInstalled() == true
            // Even when visible, the coachmark animation only fires
            // during a calm moment — never while a download or install
            // is mid-flight, never with the inspect sheet already open.
            val coachmarkActive =
                state.isApkInspectCoachmarkPending &&
                    canInspectApk &&
                    !state.isDownloading &&
                    !state.isInstalling &&
                    state.downloadStage == DownloadStage.IDLE &&
                    !state.isApkInspectSheetVisible
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SmartInstallButton(
                        isDownloading = state.isDownloading,
                        isInstalling = state.isInstalling,
                        progress = state.downloadProgressPercent,
                        primaryAsset = state.primaryAsset,
                        state = state,
                        onAction = onAction,
                        modifier = Modifier.weight(1f),
                    )
                    if (canInspectApk) {
                        InspectApkButton(
                            showCoachmark = coachmarkActive,
                            onClick = { onAction(DetailsAction.OnInspectApk) },
                            onCoachmarkDismiss = {
                                onAction(DetailsAction.OnAcknowledgeApkInspectCoachmark)
                            },
                        )
                    }
                }

            DropdownMenu(
                expanded = state.isInstallDropdownExpanded,
                onDismissRequest = {
                    onAction(DetailsAction.OnToggleInstallDropdown)
                },
                offset = DpOffset(x = 0.dp, y = 20.dp),
            ) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = stringResource(Res.string.open_in_obtainium),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.obtainium_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onAction(DetailsAction.OpenInObtainium)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                )

                Spacer(Modifier.height(8.dp))

                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = stringResource(Res.string.inspect_with_appmanager),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.appmanager_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onAction(DetailsAction.OpenInAppManager)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                )

                Spacer(Modifier.height(8.dp))

                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = stringResource(Res.string.open_with_external_installer),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(Res.string.external_installer_description),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        onAction(DetailsAction.InstallWithExternalApp)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                )
            }
        }
    }
    }
}
