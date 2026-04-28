package zed.rainxch.details.presentation

import zed.rainxch.core.domain.model.GithubAsset
import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.core.domain.model.GithubRepoSummary
import zed.rainxch.core.domain.model.GithubUserProfile
import zed.rainxch.core.domain.model.InstalledApp
import zed.rainxch.core.domain.model.SystemArchitecture
import zed.rainxch.core.domain.model.isEffectivelyPreRelease
import zed.rainxch.details.domain.model.ReleaseCategory
import zed.rainxch.details.domain.model.RepoStats
import zed.rainxch.details.presentation.model.AttestationStatus
import zed.rainxch.details.presentation.model.DowngradeWarning
import zed.rainxch.details.presentation.model.DownloadStage
import zed.rainxch.details.presentation.model.InstallLogItem
import zed.rainxch.details.presentation.model.SigningKeyWarning
import zed.rainxch.details.presentation.model.TranslationState
import zed.rainxch.details.presentation.model.TranslationTarget

data class DetailsState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val userProfile: GithubUserProfile? = null,
    val repository: GithubRepoSummary? = null,
    // state for assets
    val primaryAsset: GithubAsset? = null,
    val installableAssets: List<GithubAsset> = emptyList(),
    // state for releases
    val selectedRelease: GithubRelease? = null,
    val allReleases: List<GithubRelease> = emptyList(),
    val releasesLoadFailed: Boolean = false,
    val isRetryingReleases: Boolean = false,
    val isReleaseSelectorVisible: Boolean = false,
    val selectedReleaseCategory: ReleaseCategory = ReleaseCategory.STABLE,
    val isVersionPickerVisible: Boolean = false,
    val stats: RepoStats? = null,
    val readmeMarkdown: String? = null,
    val readmeLanguage: String? = null,
    val installLogs: List<InstallLogItem> = emptyList(),
    val isDownloading: Boolean = false,
    val downloadProgressPercent: Int? = null,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val isInstalling: Boolean = false,
    val downloadError: String? = null,
    val installError: String? = null,
    val downloadStage: DownloadStage = DownloadStage.IDLE,
    val systemArchitecture: SystemArchitecture = SystemArchitecture.UNKNOWN,
    val isObtainiumAvailable: Boolean = false,
    val isObtainiumEnabled: Boolean = false,
    val isInstallDropdownExpanded: Boolean = false,
    val isAppManagerAvailable: Boolean = false,
    val isAppManagerEnabled: Boolean = false,
    val installedApp: InstalledApp? = null,
    val isFavourite: Boolean = false,
    val isStarred: Boolean = false,
    val isTrackingApp: Boolean = false,
    val isAboutExpanded: Boolean = false,
    val isWhatsNewExpanded: Boolean = false,
    val isLiquidGlassEnabled: Boolean = true,
    val aboutTranslation: TranslationState = TranslationState(),
    val whatsNewTranslation: TranslationState = TranslationState(),
    val isLanguagePickerVisible: Boolean = false,
    val languagePickerTarget: TranslationTarget? = null,
    val deviceLanguageCode: String = "en",
    val isComingFromUpdate: Boolean = false,
    val downgradeWarning: DowngradeWarning? = null,
    val signingKeyWarning: SigningKeyWarning? = null,
    val showExternalInstallerPrompt: Boolean = false,
    val pendingInstallFilePath: String? = null,
    val showUninstallConfirmation: Boolean = false,
    val showUnlinkConfirmation: Boolean = false,
    val attestationStatus: AttestationStatus = AttestationStatus.UNCHECKED,
    /**
     * Days since the most recent stable release when the project is
     * actively shipping pre-releases on top of it. `null` means
     * either healthy (recent stable) or no applicable signal
     * (project has no stable releases at all). Set by the ViewModel
     * from `latestStable.publishedAt` vs `Clock.now()` when releases
     * load. See release UX #6.
     */
    val stalledStableSinceDays: Int? = null,
    /**
     * Concatenated release notes for every release newer than the
     * user's `installedApp.installedVersion`, most-recent-first.
     * Populated when the user is tracking the app and at least one
     * newer release exists. Null when there's no installed version
     * or no newer releases. See release UX #4.
     */
    val mergedChangelog: String? = null,
    /**
     * Release tag for the head of [mergedChangelog] (the version the
     * user would jump from). Used to title the merged section as
     * "What's changed since v1.2.3".
     */
    val mergedChangelogBaseTag: String? = null,
    /**
     * Whether [latestStableRelease] has at least one asset that the
     * platform installer can handle. Computed by the ViewModel
     * whenever `allReleases` changes — we can't compute it here
     * because the installer's per-platform asset-extension policy
     * lives outside the data model. Gates [canSwitchToStable] so
     * the rollback chip never advertises an action that would
     * silently no-op for releases that ship only source tarballs.
     */
    val latestStableHasInstallableAsset: Boolean = false,
) {
    val filteredReleases: List<GithubRelease>
        get() =
            when (selectedReleaseCategory) {
                ReleaseCategory.STABLE -> allReleases.filter { !it.isEffectivelyPreRelease() }
                ReleaseCategory.PRE_RELEASE -> allReleases.filter { it.isEffectivelyPreRelease() }
                ReleaseCategory.ALL -> allReleases
            }

    /**
     * Most recent non-pre-release release, or `null` when the
     * project has no stable releases in the current window. Drives
     * the "Switch to stable vX.Y.Z" rollback action.
     */
    val latestStableRelease: GithubRelease?
        get() =
            allReleases
                .filter { !it.isEffectivelyPreRelease() }
                .maxByOrNull { it.publishedAt }

    /**
     * True when the install button should expose a "switch to
     * stable" rollback affordance: the user is tracking this app,
     * is currently on a release that's effectively a pre-release,
     * a distinct stable release exists, AND that stable release has
     * at least one installable asset on the current platform. The
     * handler (`DetailsAction.SwitchToStable`) selects the stable
     * release and invokes the normal install path.
     */
    val canSwitchToStable: Boolean
        get() {
            val app = installedApp ?: return false
            val stable = latestStableRelease ?: return false
            if (!latestStableHasInstallableAsset) return false
            val installedIsPreRelease =
                allReleases.firstOrNull { it.tagName == app.installedVersion }
                    ?.isEffectivelyPreRelease() == true
            if (!installedIsPreRelease) return false
            // Don't offer the button if the stable release IS the
            // one the user has already (same tag string).
            return stable.tagName != app.installedVersion
        }

    /**
     * True when the currently-tracked app has a *parked* install file
     * that matches the user's current selection (release tag + asset
     * name). The install button can short-circuit the download phase
     * and dispatch the dialog/install flow on the parked file directly.
     *
     * This is the data-layer match — the VM also re-checks the file
     * exists on disk before actually using it (in [parkedFilePathIfMatches]).
     */
    val isPendingInstallReady: Boolean
        get() {
            val app = installedApp ?: return false
            val parkedVersion = app.pendingInstallVersion ?: return false
            val parkedAsset = app.pendingInstallAssetName ?: return false
            if (app.pendingInstallFilePath.isNullOrBlank()) return false
            val tag = selectedRelease?.tagName ?: return false
            val assetName = primaryAsset?.name ?: return false
            return parkedVersion == tag && parkedAsset == assetName
        }
}
