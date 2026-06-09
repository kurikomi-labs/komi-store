package zed.rainxch.details.presentation.components.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Update
import zed.rainxch.core.presentation.components.overlays.GhsDropdownMenu
import zed.rainxch.core.presentation.components.overlays.GhsDropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.details.presentation.DetailsAction
import zed.rainxch.details.presentation.DetailsState
import zed.rainxch.details.presentation.components.AppHeader
import zed.rainxch.details.presentation.components.LinkedRepoBanner
import zed.rainxch.details.presentation.components.ReleaseAssetsPicker
import zed.rainxch.details.presentation.components.ReleasesStatus
import zed.rainxch.details.presentation.components.ReleasesStatusCard
import zed.rainxch.core.domain.model.installation.InstallSource
import zed.rainxch.core.domain.model.installation.isReallyInstalled
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
            SelectionContainer {
                AppHeader(
                    author = state.userProfile,
                    release = state.selectedRelease,
                    repository = state.repository,
                    installedApp = state.installedApp,
                    stats = state.stats,
                    downloadStage = state.downloadStage,
                    downloadProgress = state.downloadProgressPercent,
                    isCurrentUserOwner = state.isCurrentUserOwner,
                    onPlatformClick = { platform ->
                        onAction(DetailsAction.OnPlatformChipClick(platform))
                    },
                    onOwnerClick = {
                        onAction(
                            DetailsAction.OpenDeveloperProfile(
                                state.repository.owner.login,
                            ),
                        )
                    },
                )
            }
        }
    }

    val installedApp = state.installedApp
    val repository = state.repository
    if (installedApp?.installSource == InstallSource.MANUAL && repository != null) {
        item {
            LinkedRepoBanner(
                owner = repository.owner.login,
                repo = repository.name,
                onUnlink = { onAction(DetailsAction.OnUnlinkExternalApp) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

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

        if (state.allReleases.isNotEmpty()) {
            item {
                VersionTypePicker(
                    selectedCategory = state.selectedReleaseCategory,
                    onAction = onAction,
                    modifier = Modifier.fillMaxWidth().animateItem(),
                )
            }
        }

        if (state.allReleases.isNotEmpty() || state.installableAssets.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    val crossPlatformAssets =
                        androidx.compose.runtime.remember(state.selectedRelease) {
                            state.selectedRelease
                                ?.assets
                                ?.filter {
                                    zed.rainxch.core.domain.utils
                                        .assetPlatformOf(it.name) != null
                                }
                                .orEmpty()
                        }

                    val pinnedVariantLabel =
                        state.installedApp?.preferredAssetVariant?.let { stored ->
                            state.primaryAsset?.name?.let { name ->
                                zed.rainxch.core.domain.utils.AssetVariant.extract(name)
                                    ?.takeIf { it.isNotBlank() }
                            } ?: stored
                        }
                    ReleaseAssetsPicker(
                        assetsList = state.installableAssets,
                        selectedAsset = state.primaryAsset,
                        isPickerVisible = state.isReleaseSelectorVisible,
                        pinnedVariant = pinnedVariantLabel,
                        showAllPlatforms = state.showAllPlatforms,
                        crossPlatformAssets = crossPlatformAssets,
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

            val canInspectApk = state.installedApp?.isReallyInstalled() == true

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

            GhsDropdownMenu(
                expanded = state.isInstallDropdownExpanded,
                onDismissRequest = {
                    onAction(DetailsAction.OnToggleInstallDropdown)
                },
                offset = DpOffset(x = 0.dp, y = 20.dp),
            ) {
                GhsDropdownMenuItem(
                    text = stringResource(Res.string.open_in_obtainium),
                    subtitle = stringResource(Res.string.obtainium_description),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = { onAction(DetailsAction.OpenInObtainium) },
                )
                GhsDropdownMenuItem(
                    text = stringResource(Res.string.inspect_with_appmanager),
                    subtitle = stringResource(Res.string.appmanager_description),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = { onAction(DetailsAction.OpenInAppManager) },
                )
                GhsDropdownMenuItem(
                    text = stringResource(Res.string.open_with_external_installer),
                    subtitle = stringResource(Res.string.external_installer_description),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    onClick = { onAction(DetailsAction.InstallWithExternalApp) },
                )
            }
        }
    }
    }
}
