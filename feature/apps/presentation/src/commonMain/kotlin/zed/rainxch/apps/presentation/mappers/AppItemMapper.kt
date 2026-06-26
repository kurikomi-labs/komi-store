package zed.rainxch.apps.presentation.mappers

import org.jetbrains.compose.resources.getString
import zed.rainxch.apps.presentation.model.AppItem
import zed.rainxch.apps.presentation.model.InstalledAppUi
import zed.rainxch.apps.presentation.model.UpdateState
import zed.rainxch.core.presentation.utils.formatEpochDate
import zed.rainxch.core.presentation.utils.formatIsoDate
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.apps_version_dated
import zed.rainxch.githubstore.core.presentation.res.apps_version_update

fun computeIsBusy(isPendingInstall: Boolean, updateState: UpdateState): Boolean =
    isPendingInstall ||
        updateState is UpdateState.Downloading ||
        updateState is UpdateState.Installing ||
        updateState is UpdateState.CheckingUpdate

suspend fun InstalledAppUi.toAppItem(
    updateState: UpdateState = UpdateState.Idle,
    downloadProgress: Int? = null,
    error: String? = null,
): AppItem {
    val canSkipVersion = isUpdateAvailable &&
        !(latestVersion ?: latestVersionName).isNullOrBlank()

    return AppItem(
        installedApp = this,
        updateState = updateState,
        downloadProgress = downloadProgress,
        error = error,
        isBusy = computeIsBusy(isPendingInstall, updateState),
        hasFilter = !assetFilterRegex.isNullOrBlank() || fallbackToOlderReleases,
        hasPin = !preferredAssetVariant.isNullOrBlank(),
        canSkipVersion = canSkipVersion,
        versionLabel = buildVersionLabel(
            installedVersion = installedVersion,
            latestVersion = latestVersion,
            latestReleasePublishedAt = latestReleasePublishedAt,
            lastUpdatedAt = lastUpdatedAt,
        ),
        idleVersionLabel = buildVersionLabel(
            installedVersion = installedVersion,
            latestVersion = null,
            latestReleasePublishedAt = null,
            lastUpdatedAt = lastUpdatedAt,
        ),
    )
}

private suspend fun buildVersionLabel(
    installedVersion: String,
    latestVersion: String?,
    latestReleasePublishedAt: String?,
    lastUpdatedAt: Long,
): String {
    val displayDate = if (latestVersion != null) {
        formatIsoDate(latestReleasePublishedAt)
    } else {
        formatEpochDate(lastUpdatedAt)
    }

    val base = if (latestVersion != null) {
        getString(Res.string.apps_version_update, installedVersion, latestVersion)
    } else {
        installedVersion
    }

    return if (displayDate != null) {
        getString(Res.string.apps_version_dated, base, displayDate)
    } else {
        base
    }
}
