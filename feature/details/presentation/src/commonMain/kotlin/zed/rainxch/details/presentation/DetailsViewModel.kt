package zed.rainxch.details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.ApkPackageInfo
import zed.rainxch.core.domain.model.FavoriteRepo
import zed.rainxch.core.domain.model.GithubAsset
import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.core.domain.model.InstalledApp
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.domain.model.RateLimitException
import zed.rainxch.core.domain.model.isEffectivelyPreRelease
import zed.rainxch.core.domain.network.Downloader
import zed.rainxch.core.domain.repository.ExternalImportRepository
import zed.rainxch.core.domain.repository.FavouritesRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.SeenReposRepository
import zed.rainxch.core.domain.repository.StarredRepository
import zed.rainxch.core.domain.repository.TelemetryRepository
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.system.DownloadOrchestrator
import zed.rainxch.core.domain.system.DownloadSpec
import zed.rainxch.core.domain.system.DownloadStage as OrchestratorStage
import zed.rainxch.core.domain.system.InstallOutcome
import zed.rainxch.core.domain.system.InstallPolicy
import zed.rainxch.core.domain.system.Installer
import zed.rainxch.core.domain.model.InstallerType
import zed.rainxch.core.domain.system.PackageMonitor
import zed.rainxch.core.domain.use_cases.SyncInstalledAppsUseCase
import zed.rainxch.core.domain.util.AssetVariant
import zed.rainxch.core.domain.util.VersionMath
import zed.rainxch.core.domain.utils.BrowserHelper
import zed.rainxch.core.domain.utils.ShareManager
import zed.rainxch.details.domain.model.ApkValidationResult
import zed.rainxch.details.domain.model.FingerprintCheckResult
import zed.rainxch.details.domain.model.ReleaseCategory
import zed.rainxch.details.domain.model.SaveInstalledAppParams
import zed.rainxch.details.domain.model.UpdateInstalledAppParams
import zed.rainxch.details.domain.repository.DetailsRepository
import zed.rainxch.details.domain.repository.TranslationRepository
import zed.rainxch.details.domain.system.AttestationVerifier
import zed.rainxch.details.domain.system.VerificationResult
import zed.rainxch.details.domain.system.InstallationManager
import zed.rainxch.details.domain.util.VersionHelper
import zed.rainxch.details.presentation.model.AttestationStatus
import zed.rainxch.details.presentation.model.DowngradeWarning
import zed.rainxch.details.presentation.model.DownloadStage
import zed.rainxch.details.presentation.model.InstallLogItem
import zed.rainxch.details.presentation.model.LogResult
import zed.rainxch.details.presentation.model.LogResult.Error
import zed.rainxch.details.presentation.model.SigningKeyWarning
import zed.rainxch.details.presentation.model.SupportedLanguages
import zed.rainxch.details.presentation.model.TranslationState
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.added_to_favourites
import zed.rainxch.githubstore.core.presentation.res.details_unlink_external_app_failure
import zed.rainxch.githubstore.core.presentation.res.details_unlink_external_app_success
import zed.rainxch.githubstore.core.presentation.res.failed_to_open_app
import zed.rainxch.githubstore.core.presentation.res.failed_to_share_link
import zed.rainxch.githubstore.core.presentation.res.failed_to_uninstall
import zed.rainxch.githubstore.core.presentation.res.installer_saved_downloads
import zed.rainxch.githubstore.core.presentation.res.link_copied_to_clipboard
import zed.rainxch.githubstore.core.presentation.res.rate_limit_exceeded
import zed.rainxch.githubstore.core.presentation.res.removed_from_favourites
import zed.rainxch.githubstore.core.presentation.res.translation_failed
import zed.rainxch.githubstore.core.presentation.res.update_package_mismatch
import zed.rainxch.githubstore.core.presentation.res.variant_first_pin_toast
import zed.rainxch.githubstore.core.presentation.res.variant_first_pin_toast_generic
import zed.rainxch.githubstore.core.presentation.res.variant_unpinned_toast
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime

class DetailsViewModel(
    private val repositoryId: Long,
    private val ownerParam: String,
    private val repoParam: String,
    private val detailsRepository: DetailsRepository,
    private val downloader: Downloader,
    private val installer: Installer,
    private val platform: Platform,
    private val helper: BrowserHelper,
    private val shareManager: ShareManager,
    private val installedAppsRepository: InstalledAppsRepository,
    private val favouritesRepository: FavouritesRepository,
    private val starredRepository: StarredRepository,
    private val packageMonitor: PackageMonitor,
    private val syncInstalledAppsUseCase: SyncInstalledAppsUseCase,
    private val translationRepository: TranslationRepository,
    private val logger: GitHubStoreLogger,
    private val isComingFromUpdate: Boolean,
    private val tweaksRepository: TweaksRepository,
    private val seenReposRepository: SeenReposRepository,
    private val installationManager: InstallationManager,
    private val attestationVerifier: AttestationVerifier,
    private val downloadOrchestrator: DownloadOrchestrator,
    private val telemetryRepository: TelemetryRepository,
    private val externalImportRepository: ExternalImportRepository,
) : ViewModel() {
    private var hasLoadedInitialData = false
    private var currentDownloadJob: Job? = null
    private var currentAssetName: String? = null
    private var aboutTranslationJob: Job? = null
    private var whatsNewTranslationJob: Job? = null

    private val _state = MutableStateFlow(DetailsState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    loadInitial()
                    observeLiquidGlassEnabled()

                    hasLoadedInitialData = true
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                DetailsState(),
            )

    private val _events = Channel<DetailsEvent>()
    val events = _events.receiveAsFlow()

    private val rateLimited = AtomicBoolean(false)

    fun confirmUninstall() {
        _state.update { it.copy(showUninstallConfirmation = false) }
        val installedApp = _state.value.installedApp ?: return
        logger.debug("Uninstalling app (confirmed): ${installedApp.packageName}")
        viewModelScope.launch {
            try {
                installer.uninstall(installedApp.packageName)
                _state.value.repository?.id?.let { telemetryRepository.recordUninstalled(it) }
            } catch (e: Exception) {
                logger.error("Failed to request uninstall for ${installedApp.packageName}: ${e.message}")
                _events.send(
                    DetailsEvent.OnMessage(
                        getString(Res.string.failed_to_uninstall, installedApp.packageName),
                    ),
                )
            }
        }
    }

    private fun confirmUnlinkExternalApp() {
        _state.update { it.copy(showUnlinkConfirmation = false) }
        val installedApp = _state.value.installedApp ?: return
        val packageName = installedApp.packageName
        logger.debug("Unlinking externally-imported app: $packageName")
        viewModelScope.launch {
            try {
                // installed_apps + external_links must move together so the
                // next scan re-proposes a match instead of treating the row
                // as a healthy tracked app on a stale link.
                installedAppsRepository.executeInTransaction {
                    externalImportRepository.unlink(packageName)
                    installedAppsRepository.deleteInstalledApp(packageName)
                }
                runCatching { telemetryRepository.importUnlinkedFromDetails() }
                _events.send(
                    DetailsEvent.OnMessage(
                        getString(Res.string.details_unlink_external_app_success),
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to unlink $packageName: ${e.message}")
                _events.send(
                    DetailsEvent.OnMessage(
                        getString(Res.string.details_unlink_external_app_failure),
                    ),
                )
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun onAction(action: DetailsAction) {
        when (action) {
            DetailsAction.Retry -> {
                hasLoadedInitialData = false
                loadInitial()
            }

            DetailsAction.RetryReleases -> retryReleases()

            DetailsAction.OnDismissDowngradeWarning -> {
                dismissDowngradeWarning()
            }

            DetailsAction.OnDismissSigningKeyWarning -> {
                _state.update {
                    it.copy(
                        signingKeyWarning = null,
                        downloadStage = DownloadStage.IDLE,
                    )
                }
                currentAssetName = null
            }

            DetailsAction.OnOverrideSigningKeyWarning -> {
                overrideSigningKeyWarning()
            }

            DetailsAction.InstallPrimary -> {
                install()
            }

            DetailsAction.OnRequestUninstall -> {
                _state.update { it.copy(showUninstallConfirmation = true) }
            }

            DetailsAction.OnDismissUninstallConfirmation -> {
                _state.update { it.copy(showUninstallConfirmation = false) }
            }

            DetailsAction.OnConfirmUninstall -> {
                confirmUninstall()
            }

            DetailsAction.UninstallApp -> {
                uninstallApp()
            }

            DetailsAction.OnUnlinkExternalApp -> {
                _state.update { it.copy(showUnlinkConfirmation = true) }
            }

            DetailsAction.OnDismissUnlinkConfirmation -> {
                _state.update { it.copy(showUnlinkConfirmation = false) }
            }

            DetailsAction.OnConfirmUnlinkExternalApp -> {
                confirmUnlinkExternalApp()
            }

            is DetailsAction.DownloadAsset -> {
                val release = _state.value.selectedRelease
                downloadAsset(
                    downloadUrl = action.downloadUrl,
                    assetName = action.assetName,
                    sizeBytes = action.sizeBytes,
                    releaseTag = release?.tagName ?: "",
                )
            }

            DetailsAction.CancelCurrentDownload -> {
                cancelCurrentDownload()
            }

            DetailsAction.OnToggleFavorite -> {
                toggleFavourite()
            }

            DetailsAction.OnShareClick -> {
                share()
            }

            DetailsAction.UpdateApp -> {
                update()
            }

            DetailsAction.OpenApp -> {
                openApp()
            }

            DetailsAction.OpenRepoInBrowser -> {
                _state.value.repository?.htmlUrl?.let {
                    helper.openUrl(url = it)
                }
            }

            DetailsAction.OpenAuthorInBrowser -> {
                _state.value.userProfile?.htmlUrl?.let {
                    helper.openUrl(url = it)
                }
            }

            DetailsAction.OpenInObtainium -> {
                openObtainium()
            }

            DetailsAction.OpenInAppManager -> {
                openAppManager()
            }

            DetailsAction.OnToggleInstallDropdown -> {
                _state.update {
                    it.copy(isInstallDropdownExpanded = !it.isInstallDropdownExpanded)
                }
            }

            is DetailsAction.SelectReleaseCategory -> {
                selectReleaseCategory(action)
            }

            is DetailsAction.SelectRelease -> {
                val release = action.release
                val (installable, primary) = recomputeAssetsForRelease(release)
                whatsNewTranslationJob?.cancel()

                _state.update {
                    it.copy(
                        selectedRelease = release,
                        installableAssets = installable,
                        primaryAsset = primary,
                        isVersionPickerVisible = false,
                        whatsNewTranslation = TranslationState(),
                    )
                }
            }

            DetailsAction.ToggleVersionPicker -> {
                _state.update {
                    it.copy(isVersionPickerVisible = !it.isVersionPickerVisible)
                }
            }

            DetailsAction.ToggleAboutExpanded -> {
                _state.update {
                    it.copy(isAboutExpanded = !it.isAboutExpanded)
                }
            }

            DetailsAction.ToggleWhatsNewExpanded -> {
                _state.update {
                    it.copy(isWhatsNewExpanded = !it.isWhatsNewExpanded)
                }
            }

            is DetailsAction.TranslateAbout -> {
                val readme = _state.value.readmeMarkdown ?: return
                aboutTranslationJob?.cancel()
                aboutTranslationJob =
                    translateContent(
                        text = readme,
                        targetLanguageCode = action.targetLanguageCode,
                        updateState = { ts -> _state.update { it.copy(aboutTranslation = ts) } },
                        getCurrentState = { _state.value.aboutTranslation },
                    )
            }

            is DetailsAction.TranslateWhatsNew -> {
                val description = _state.value.selectedRelease?.description ?: return
                whatsNewTranslationJob?.cancel()
                whatsNewTranslationJob =
                    translateContent(
                        text = description,
                        targetLanguageCode = action.targetLanguageCode,
                        updateState = { ts -> _state.update { it.copy(whatsNewTranslation = ts) } },
                        getCurrentState = { _state.value.whatsNewTranslation },
                    )
            }

            DetailsAction.ToggleAboutTranslation -> {
                _state.update {
                    val current = it.aboutTranslation
                    it.copy(aboutTranslation = current.copy(isShowingTranslation = !current.isShowingTranslation))
                }
            }

            DetailsAction.ToggleWhatsNewTranslation -> {
                _state.update {
                    val current = it.whatsNewTranslation
                    it.copy(whatsNewTranslation = current.copy(isShowingTranslation = !current.isShowingTranslation))
                }
            }

            is DetailsAction.ShowLanguagePicker -> {
                _state.update {
                    it.copy(
                        isLanguagePickerVisible = true,
                        languagePickerTarget = action.target,
                    )
                }
            }

            DetailsAction.DismissLanguagePicker -> {
                _state.update {
                    it.copy(isLanguagePickerVisible = false, languagePickerTarget = null)
                }
            }

            DetailsAction.OpenWithExternalInstaller -> {
                openExternalInstaller()
            }

            DetailsAction.DismissExternalInstallerPrompt -> {
                _state.value =
                    _state.value.copy(
                        showExternalInstallerPrompt = false,
                        pendingInstallFilePath = null,
                    )
            }

            DetailsAction.InstallWithExternalApp -> {
                installViaExternalApp()
            }

            DetailsAction.OnNavigateBackClick -> {
                // Handled in composable
            }

            is DetailsAction.OpenDeveloperProfile -> {
                // Handled in composable
            }

            is DetailsAction.OnMessage -> {
                // Handled in composable
            }

            is DetailsAction.SelectDownloadAsset -> {
                _state.update { state -> state.copy(primaryAsset = action.release) }
                persistPreferredVariantOnPick(action.release)
            }

            DetailsAction.ToggleReleaseAssetsPicker -> {
                _state.update { state -> state.copy(isReleaseSelectorVisible = !state.isReleaseSelectorVisible) }
            }

            DetailsAction.UnpinPreferredVariant -> {
                unpinPreferredVariant()
            }

            DetailsAction.ToggleIncludeBetas -> {
                toggleIncludeBetas()
            }

            DetailsAction.SwitchToStable -> {
                switchToStable()
            }
        }
    }

    /**
     * Derived signals surfaced in the Details UX for pre-release
     * handling (release UX #4 and #6). Computed once per release-list
     * load and re-used across the two call sites that update state
     * with a fresh `allReleases`.
     */
    private data class ReleaseInsights(
        val stalledStableSinceDays: Int?,
        val mergedChangelog: String?,
        val mergedChangelogBaseTag: String?,
        val latestStableHasInstallableAsset: Boolean,
    )

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun computeReleaseInsights(
        allReleases: List<GithubRelease>,
        installedApp: InstalledApp?,
    ): ReleaseInsights {
        // Merged "What's changed since v…": concatenate release notes
        // for every release strictly newer than the installed tag,
        // most-recent-first. Mirrors what app stores do when the user
        // skips versions between updates — they deserve to see every
        // intermediate changelog, not just the head one.
        val (merged, mergedBase) =
            if (installedApp != null && allReleases.size > 1) {
                val installedTag = installedApp.installedVersion
                val newer =
                    allReleases.filter { release ->
                        VersionMath.isVersionNewer(release.tagName, installedTag)
                    }
                if (newer.size >= 2) {
                    val body =
                        newer.joinToString(separator = "\n\n") { release ->
                            val heading = "— ${release.tagName} —"
                            val notes = release.description?.trim().orEmpty()
                            if (notes.isEmpty()) heading else "$heading\n$notes"
                        }
                    body to installedTag
                } else {
                    null to null
                }
            } else {
                null to null
            }

        val latestStable =
            allReleases
                .filter { !it.isEffectivelyPreRelease() }
                .maxByOrNull { it.publishedAt }

        // Stalled-project warning: the project has at least one stable
        // release, has shipped pre-releases on top of it, and the last
        // stable is older than [STALLED_STABLE_THRESHOLD_DAYS]. That's
        // the "beta spiral with no stabilisation" signal that warrants
        // a heads-up before the user opts into betas.
        val stalledDays: Int? =
            run {
                val stable = latestStable ?: return@run null
                val preReleasesAfter =
                    allReleases.any { release ->
                        release.isEffectivelyPreRelease() &&
                            VersionMath.isVersionNewer(release.tagName, stable.tagName)
                    }
                if (!preReleasesAfter) return@run null
                val days = daysSinceIso(stable.publishedAt) ?: return@run null
                if (days >= STALLED_STABLE_THRESHOLD_DAYS) days else null
            }

        val latestStableHasInstallableAsset =
            latestStable?.assets?.any { installer.isAssetInstallable(it.name) } == true

        return ReleaseInsights(
            stalledStableSinceDays = stalledDays,
            mergedChangelog = merged,
            mergedChangelogBaseTag = mergedBase,
            latestStableHasInstallableAsset = latestStableHasInstallableAsset,
        )
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun daysSinceIso(isoTimestamp: String?): Int? {
        if (isoTimestamp.isNullOrBlank()) return null
        return try {
            val published = kotlin.time.Instant.parse(isoTimestamp)
            val now = System.now()
            val diffMs = now.toEpochMilliseconds() - published.toEpochMilliseconds()
            if (diffMs < 0) null else (diffMs / MILLIS_PER_DAY).toInt()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Flips the per-app `includePreReleases` flag via
     * [InstalledAppsRepository.setIncludePreReleases]. Kicks off a
     * fresh `checkForUpdates` so the new channel takes effect
     * immediately on-screen instead of waiting for the next
     * periodic cycle.
     */
    private fun toggleIncludeBetas() {
        val app = _state.value.installedApp ?: return
        val newValue = !app.includePreReleases
        viewModelScope.launch {
            try {
                installedAppsRepository.setIncludePreReleases(
                    packageName = app.packageName,
                    enabled = newValue,
                )
                // Re-validate against the new channel immediately so
                // the user sees the result of the toggle in the next
                // frame (the DB observer will also refresh state).
                installedAppsRepository.checkForUpdates(app.packageName)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.warn("toggleIncludeBetas failed for ${app.packageName}: ${t.message}")
            }
        }
    }

    /**
     * Switches the currently-tracked app to the latest stable
     * release: selects it as the picked release, and triggers the
     * normal install flow. Reuses the existing `InstallPrimary`
     * path so downgrade warnings, signing-key checks, and asset
     * picking all kick in exactly as they do for a manual version
     * selection.
     */
    private fun switchToStable() {
        val stable = _state.value.latestStableRelease ?: return
        // Defence in depth: the chip should already be hidden when the
        // stable release ships nothing the platform installer can
        // handle, but a stale state could still drive us here. Resolve
        // the primary asset up front and bail before the dispatch chain
        // would otherwise reach `install()` with `primaryAsset = null`
        // and silently no-op.
        val (_, primary) = recomputeAssetsForRelease(stable, _state.value.installedApp)
        if (primary == null) {
            logger.warn(
                "switchToStable: stable ${stable.tagName} has no installable asset; skipping",
            )
            return
        }
        onAction(DetailsAction.SelectRelease(stable))
        onAction(DetailsAction.InstallPrimary)
    }

    /**
     * Persists the multi-layer fingerprint of [picked] when:
     *  - the app is already tracked (otherwise there's no row to update —
     *    the link flow will derive the fingerprint at install time)
     *  - the picked asset has a non-null fingerprint (single-asset releases
     *    and unparseable filenames return null)
     *  - the new fingerprint differs from what's currently stored, OR the
     *    stale flag is set (re-picking the same variant after a stale event
     *    must clear the flag)
     *
     * Emits a one-time "remembered" toast when the app had no fingerprint
     * before this pick — that's the user's first time pinning, and the
     * implicit behaviour deserves to be made explicit.
     */
    private fun persistPreferredVariantOnPick(picked: GithubAsset) {
        val installedApp = _state.value.installedApp ?: return
        val installable = _state.value.installableAssets
        val fingerprint =
            AssetVariant.fingerprintFromPickedAsset(
                pickedAssetName = picked.name,
                siblingAssetCount = installable.size,
            ) ?: return

        val serializedTokens = AssetVariant.serializeTokens(fingerprint.tokens)
        val pickedIndex = installable.indexOfFirst { it.id == picked.id }.takeIf { it >= 0 }

        val currentVariant = installedApp.preferredAssetVariant
        val currentTokens = installedApp.preferredAssetTokens
        val currentGlob = installedApp.assetGlobPattern
        val newSiblingCount = installable.size.takeIf { it > 0 }
        val sameVariant =
            if (fingerprint.variant == null && currentVariant == null) {
                true
            } else {
                fingerprint.variant?.equals(currentVariant, ignoreCase = true) == true
            }
        val isSameFingerprint =
            sameVariant &&
                serializedTokens == currentTokens &&
                fingerprint.glob == currentGlob &&
                pickedIndex == installedApp.pickedAssetIndex &&
                newSiblingCount == installedApp.pickedAssetSiblingCount

        // Treat the app as "previously unpinned" only when *all* identity
        // layers are blank — otherwise we'd nag every time the user
        // re-picked the same variant after the resolver populated the
        // legacy tail field.
        val isFirstPin =
            currentVariant.isNullOrBlank() &&
                currentTokens.isNullOrBlank() &&
                currentGlob.isNullOrBlank()

        val shouldSave = !isSameFingerprint || installedApp.preferredVariantStale
        if (!shouldSave) return

        viewModelScope.launch {
            try {
                installedAppsRepository.setPreferredVariant(
                    packageName = installedApp.packageName,
                    variant = fingerprint.variant,
                    tokens = serializedTokens,
                    glob = fingerprint.glob,
                    pickedIndex = pickedIndex,
                    siblingCount = newSiblingCount,
                )
                if (isFirstPin) {
                    val label = fingerprint.variant
                        ?: fingerprint.tokens.firstOrNull()
                        ?: ""
                    val message =
                        if (label.isNotEmpty()) {
                            getString(Res.string.variant_first_pin_toast, label)
                        } else {
                            getString(Res.string.variant_first_pin_toast_generic)
                        }
                    _events.send(DetailsEvent.OnMessage(message))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(
                    "Failed to persist preferred variant for " +
                        "${installedApp.packageName}: ${e.message}",
                )
            }
        }
    }

    private fun unpinPreferredVariant() {
        val installedApp = _state.value.installedApp ?: return
        viewModelScope.launch {
            try {
                installedAppsRepository.clearPreferredVariant(installedApp.packageName)
                _events.send(
                    DetailsEvent.OnMessage(getString(Res.string.variant_unpinned_toast)),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error(
                    "Failed to clear preferred variant for " +
                        "${installedApp.packageName}: ${e.message}",
                )
            }
        }
    }

    private fun observeLiquidGlassEnabled() {
        viewModelScope.launch {
        }
    }

    private fun retryReleases() {
        val repo = _state.value.repository ?: return
        if (_state.value.isRetryingReleases) return
        viewModelScope.launch {
            val prevCategory = _state.value.selectedReleaseCategory
            _state.update { it.copy(isRetryingReleases = true, releasesLoadFailed = false) }
            try {
                val releases =
                    detailsRepository.getAllReleases(
                        owner = repo.owner.login,
                        repo = repo.name,
                        defaultBranch = repo.defaultBranch,
                    )
                // Prefer a release that matches the user's previous category.
                // Only fall back to the generic "first stable, else first" rule
                // when no release exists in that category — in which case reset
                // the category too so the UI doesn't end up with a category
                // selected but no matching release.
                val byPrevCategory = when (prevCategory) {
                    ReleaseCategory.STABLE -> releases.firstOrNull { !it.isEffectivelyPreRelease() }
                    ReleaseCategory.PRE_RELEASE -> releases.firstOrNull { it.isEffectivelyPreRelease() }
                    ReleaseCategory.ALL -> releases.firstOrNull()
                }
                val selected = byPrevCategory
                    ?: releases.firstOrNull { !it.isEffectivelyPreRelease() }
                    ?: releases.firstOrNull()
                // When the previous category yields nothing, derive the
                // category from the actually-selected release so the
                // filter matches what's on screen — otherwise a
                // pre-release-only project leaves the user with category
                // STABLE and an empty filtered list.
                val resolvedCategory = when {
                    byPrevCategory != null -> prevCategory
                    selected?.isEffectivelyPreRelease() == true -> ReleaseCategory.PRE_RELEASE
                    else -> ReleaseCategory.STABLE
                }
                val (installable, primary) =
                    recomputeAssetsForRelease(selected, _state.value.installedApp)
                val insights = computeReleaseInsights(releases, _state.value.installedApp)
                _state.update {
                    it.copy(
                        allReleases = releases,
                        releasesLoadFailed = false,
                        isRetryingReleases = false,
                        selectedRelease = selected,
                        selectedReleaseCategory = resolvedCategory,
                        installableAssets = installable,
                        primaryAsset = primary,
                        stalledStableSinceDays = insights.stalledStableSinceDays,
                        mergedChangelog = insights.mergedChangelog,
                        mergedChangelogBaseTag = insights.mergedChangelogBaseTag,
                        latestStableHasInstallableAsset =
                            insights.latestStableHasInstallableAsset,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: RateLimitException) {
                _state.update {
                    it.copy(isRetryingReleases = false, releasesLoadFailed = true)
                }
            } catch (t: Throwable) {
                logger.warn("Retry failed to load releases: ${t.message}")
                _state.update {
                    it.copy(isRetryingReleases = false, releasesLoadFailed = true)
                }
            }
        }
    }

    private fun recomputeAssetsForRelease(
        release: GithubRelease?,
        installedAppOverride: InstalledApp? = _state.value.installedApp,
    ): Pair<List<GithubAsset>, GithubAsset?> {
        val installable =
            release
                ?.assets
                ?.filter { asset ->
                    installer.isAssetInstallable(asset.name)
                }.orEmpty()

        val trackedApp = installedAppOverride
        val variantMatch =
            AssetVariant.resolvePreferredAsset(
                assets = installable,
                pinnedVariant = trackedApp?.preferredAssetVariant,
                pinnedTokens = AssetVariant.deserializeTokens(trackedApp?.preferredAssetTokens),
                pinnedGlob = trackedApp?.assetGlobPattern,
            )
        val samePositionMatch =
            if (variantMatch == null) {
                AssetVariant.resolveBySamePosition(
                    assets = installable,
                    originalIndex = trackedApp?.pickedAssetIndex,
                    siblingCountAtPickTime = trackedApp?.pickedAssetSiblingCount,
                )
            } else {
                null
            }
        val primary = variantMatch ?: samePositionMatch ?: installer.choosePrimaryAsset(installable)
        return installable to primary
    }

    private fun observeInstalledApp(repoId: Long) {
        viewModelScope.launch {
            installedAppsRepository
                .getAppByRepoIdAsFlow(repoId)
                .distinctUntilChanged()
                .collect { app ->
                    // Recompute merged changelog + stalled signals
                    // against the new installed version — if the
                    // user just updated externally, the installed
                    // tag flips and what they've "missed" changes.
                    val insights = computeReleaseInsights(_state.value.allReleases, app)
                    _state.update {
                        it.copy(
                            installedApp = app,
                            mergedChangelog = insights.mergedChangelog,
                            mergedChangelogBaseTag = insights.mergedChangelogBaseTag,
                            stalledStableSinceDays = insights.stalledStableSinceDays,
                            latestStableHasInstallableAsset =
                                insights.latestStableHasInstallableAsset,
                        )
                    }
                }
        }
    }

    private fun installViaExternalApp() {
        currentDownloadJob?.cancel()
        val job =
            viewModelScope.launch {
                try {
                    val primary = _state.value.primaryAsset
                    val release = _state.value.selectedRelease

                    if (primary != null && release != null) {
                        currentAssetName = primary.name

                        appendLog(
                            assetName = primary.name,
                            size = primary.size,
                            tag = release.tagName,
                            result = LogResult.DownloadStarted,
                        )

                        _state.value =
                            _state.value.copy(
                                downloadError = null,
                                installError = null,
                                downloadProgressPercent = null,
                                downloadStage = DownloadStage.DOWNLOADING,
                            )

                        downloader
                            .download(primary.downloadUrl, primary.name)
                            .collect { p ->
                                _state.value =
                                    _state.value.copy(downloadProgressPercent = p.percent)
                                if (p.percent == 100) {
                                    _state.value =
                                        _state.value.copy(downloadStage = DownloadStage.VERIFYING)
                                }
                            }

                        val filePath =
                            downloader.getDownloadedFilePath(primary.name)
                                ?: throw IllegalStateException("Downloaded file not found")

                        appendLog(
                            assetName = primary.name,
                            size = primary.size,
                            tag = release.tagName,
                            result = LogResult.Downloaded,
                        )

                        _state.value = _state.value.copy(downloadStage = DownloadStage.IDLE)
                        currentAssetName = null

                        installer.openWithExternalInstaller(filePath)

                        appendLog(
                            assetName = primary.name,
                            size = primary.size,
                            tag = release.tagName,
                            result = LogResult.OpenedInExternalInstaller,
                        )
                    }
                } catch (e: CancellationException) {
                    logger.debug("Install with external app cancelled")
                    _state.value = _state.value.copy(downloadStage = DownloadStage.IDLE)
                    currentAssetName = null
                    throw e
                } catch (t: Throwable) {
                    logger.error("Failed to install with external app: ${t.message}")
                    _state.value =
                        _state.value.copy(
                            downloadStage = DownloadStage.IDLE,
                            installError = t.message,
                        )
                    currentAssetName = null

                    _state.value.primaryAsset?.let { asset ->
                        _state.value.selectedRelease?.let { release ->
                            appendLog(
                                assetName = asset.name,
                                size = asset.size,
                                tag = release.tagName,
                                result = Error(t.message),
                            )
                        }
                    }
                }
            }

        currentDownloadJob = job
        job.invokeOnCompletion {
            if (currentDownloadJob === job) {
                currentDownloadJob = null
            }
        }

        _state.update {
            it.copy(isInstallDropdownExpanded = false)
        }
    }

    private fun openExternalInstaller() {
        val filePath = _state.value.pendingInstallFilePath
        if (filePath != null) {
            try {
                installer.openWithExternalInstaller(filePath)
                _state.value.primaryAsset?.let { asset ->
                    _state.value.selectedRelease?.let { release ->
                        appendLog(
                            assetName = asset.name,
                            size = asset.size,
                            tag = release.tagName,
                            result = LogResult.OpenedInExternalInstaller,
                        )
                    }
                }
            } catch (t: Throwable) {
                logger.error("Failed to open with external installer: ${t.message}")
                _state.value = _state.value.copy(installError = t.message)
            }
        }
        _state.value =
            _state.value.copy(
                showExternalInstallerPrompt = false,
                pendingInstallFilePath = null,
            )
    }

    private fun selectReleaseCategory(action: DetailsAction.SelectReleaseCategory) {
        val newCategory = action.category
        val filtered =
            when (newCategory) {
                ReleaseCategory.STABLE -> _state.value.allReleases.filter { !it.isEffectivelyPreRelease() }
                ReleaseCategory.PRE_RELEASE -> _state.value.allReleases.filter { it.isEffectivelyPreRelease() }
                ReleaseCategory.ALL -> _state.value.allReleases
            }
        val newSelected = filtered.firstOrNull()
        val (installable, primary) = recomputeAssetsForRelease(newSelected)

        whatsNewTranslationJob?.cancel()
        _state.update {
            it.copy(
                selectedReleaseCategory = newCategory,
                selectedRelease = newSelected,
                installableAssets = installable,
                primaryAsset = primary,
                whatsNewTranslation = TranslationState(),
            )
        }
    }

    private fun openAppManager() {
        viewModelScope.launch {
            try {
                val primary = _state.value.primaryAsset
                val release = _state.value.selectedRelease

                if (primary != null && release != null) {
                    currentAssetName = primary.name

                    appendLog(
                        assetName = primary.name,
                        size = primary.size,
                        tag = release.tagName,
                        result = LogResult.PreparingForAppManager,
                    )

                    _state.value =
                        _state.value.copy(
                            downloadError = null,
                            installError = null,
                            downloadProgressPercent = null,
                            downloadStage = DownloadStage.DOWNLOADING,
                        )

                    downloader.download(primary.downloadUrl, primary.name).collect { p ->
                        _state.value =
                            _state.value.copy(downloadProgressPercent = p.percent)
                        if (p.percent == 100) {
                            _state.value =
                                _state.value.copy(downloadStage = DownloadStage.VERIFYING)
                        }
                    }

                    val filePath =
                        downloader.getDownloadedFilePath(primary.name)
                            ?: throw IllegalStateException("Downloaded file not found")

                    appendLog(
                        assetName = primary.name,
                        size = primary.size,
                        tag = release.tagName,
                        result = LogResult.Downloaded,
                    )

                    _state.value = _state.value.copy(downloadStage = DownloadStage.IDLE)
                    currentAssetName = null

                    installer.openInAppManager(
                        filePath = filePath,
                        onOpenInstaller = {
                            viewModelScope.launch {
                                _events.send(
                                    DetailsEvent.OnOpenRepositoryInApp(APP_MANAGER_REPO_ID),
                                )
                            }
                        },
                    )

                    appendLog(
                        assetName = primary.name,
                        size = primary.size,
                        tag = release.tagName,
                        result = LogResult.OpenedInAppManager,
                    )
                }
            } catch (t: Throwable) {
                logger.error("Failed to open in AppManager: ${t.message}")
                _state.value =
                    _state.value.copy(
                        downloadStage = DownloadStage.IDLE,
                        installError = t.message,
                    )
                currentAssetName = null

                _state.value.primaryAsset?.let { asset ->
                    _state.value.selectedRelease?.let { release ->
                        appendLog(
                            assetName = asset.name,
                            size = asset.size,
                            tag = release.tagName,
                            result = Error(t.message),
                        )
                    }
                }
            }
        }
        _state.update {
            it.copy(isInstallDropdownExpanded = false)
        }
    }

    private fun openObtainium() {
        val repo = _state.value.repository
        repo?.owner?.login?.let {
            installer.openInObtainium(
                repoOwner = it,
                repoName = repo.name,
                onOpenInstaller = {
                    viewModelScope.launch {
                        _events.send(
                            DetailsEvent.OnOpenRepositoryInApp(OBTAINIUM_REPO_ID),
                        )
                    }
                },
            )
        }
        _state.update {
            it.copy(isInstallDropdownExpanded = false)
        }
    }

    private fun openApp() {
        val installedApp = _state.value.installedApp ?: return
        val launched = installer.openApp(installedApp.packageName)
        if (launched && platform == Platform.ANDROID) {
            _state.value.repository?.id?.let { telemetryRepository.recordAppOpenedAfterInstall(it) }
        }
        if (!launched) {
            viewModelScope.launch {
                _events.send(
                    DetailsEvent.OnMessage(
                        getString(
                            Res.string.failed_to_open_app,
                            installedApp.appName,
                        ),
                    ),
                )
            }
        }
    }

    private fun update() {
        val installedApp = _state.value.installedApp
        val selectedRelease = _state.value.selectedRelease

        if (installedApp != null && selectedRelease != null && installedApp.isUpdateAvailable) {
            val latestAsset =
                _state.value.primaryAsset
                    ?: _state.value.installableAssets.firstOrNull {
                        it.name == installedApp.latestAssetName
                    }
                    ?: _state.value.installableAssets.firstOrNull {
                        it.name == installedApp.installedAssetName
                    }

            if (latestAsset != null) {
                installAsset(
                    downloadUrl = latestAsset.downloadUrl,
                    assetName = latestAsset.name,
                    sizeBytes = latestAsset.size,
                    releaseTag = selectedRelease.tagName,
                    isUpdate = true,
                )
            }
        }
    }

    private fun share() {
        viewModelScope.launch {
            _state.value.repository?.let { repo ->
                runCatching {
                    shareManager.shareText("https://github-store.org/app?repo=${repo.fullName}")
                }.onFailure { t ->
                    logger.error("Failed to share link: ${t.message}")
                    _events.send(
                        DetailsEvent.OnMessage(getString(Res.string.failed_to_share_link)),
                    )
                    return@launch
                }

                if (platform != Platform.ANDROID) {
                    _events.send(DetailsEvent.OnMessage(getString(Res.string.link_copied_to_clipboard)))
                }
            }
        }
    }

    private fun toggleFavourite() {
        viewModelScope.launch {
            try {
                val repo = _state.value.repository ?: return@launch
                val selectedRelease = _state.value.selectedRelease

                val favoriteRepo =
                    FavoriteRepo(
                        repoId = repo.id,
                        repoName = repo.name,
                        repoOwner = repo.owner.login,
                        repoOwnerAvatarUrl = repo.owner.avatarUrl,
                        repoDescription = repo.description,
                        primaryLanguage = repo.language,
                        repoUrl = repo.htmlUrl,
                        latestVersion = selectedRelease?.tagName,
                        latestReleaseUrl = selectedRelease?.htmlUrl,
                        addedAt = System.now().toEpochMilliseconds(),
                        lastSyncedAt = System.now().toEpochMilliseconds(),
                    )

                favouritesRepository.toggleFavorite(favoriteRepo)

                val newFavoriteState = favouritesRepository.isFavoriteSync(repo.id)
                _state.value = _state.value.copy(isFavourite = newFavoriteState)

                if (newFavoriteState) {
                    telemetryRepository.recordFavorited(repo.id)
                } else {
                    telemetryRepository.recordUnfavorited(repo.id)
                }

                _events.send(
                    element =
                        DetailsEvent.OnMessage(
                            message =
                                getString(
                                    resource =
                                        if (newFavoriteState) {
                                            Res.string.added_to_favourites
                                        } else {
                                            Res.string.removed_from_favourites
                                        },
                                ),
                        ),
                )
            } catch (t: Throwable) {
                logger.error("Failed to toggle favorite: ${t.message}")
            }
        }
    }

    private fun cancelCurrentDownload() {
        currentDownloadJob?.cancel()
        currentDownloadJob = null

        val packageKey = orchestratorKey()
        viewModelScope.launch {
            try {
                downloadOrchestrator.cancel(packageKey)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.error("Failed to cancel orchestrator download: ${t.message}")
            }
        }

        val assetName = currentAssetName
        if (assetName != null) {
            val releaseTag = _state.value.selectedRelease?.tagName ?: ""
            val totalSize = _state.value.totalBytes ?: _state.value.downloadedBytes
            appendLog(
                assetName = assetName,
                tag = releaseTag,
                size = totalSize,
                result = LogResult.Cancelled,
            )
            logger.debug("Download cancelled via orchestrator: $assetName")
        }

        currentAssetName = null
        _state.value =
            _state.value.copy(
                isDownloading = false,
                downloadProgressPercent = null,
                downloadStage = DownloadStage.IDLE,
            )
    }

    private fun uninstallApp() {
        val installedApp = _state.value.installedApp ?: return
        logger.debug("Uninstalling app: ${installedApp.packageName}")
        viewModelScope.launch {
            try {
                installer.uninstall(installedApp.packageName)
                _state.value.repository?.id?.let { telemetryRepository.recordUninstalled(it) }
            } catch (e: Exception) {
                logger.error("Failed to request uninstall for ${installedApp.packageName}: ${e.message}")
                _events.send(
                    DetailsEvent.OnMessage(
                        getString(Res.string.failed_to_uninstall, installedApp.packageName),
                    ),
                )
            }
        }
    }

    private fun install() {
        val primary = _state.value.primaryAsset
        val release = _state.value.selectedRelease
        val installedApp = _state.value.installedApp

        if (primary != null && release != null) {
            if (installedApp != null &&
                !installedApp.isPendingInstall &&
                VersionHelper.normalizeVersion(release.tagName) !=
                VersionHelper.normalizeVersion(
                    installedApp.installedVersion,
                ) &&
                platform == Platform.ANDROID
            ) {
                val isDowngrade =
                    VersionHelper.isDowngradeVersion(
                        candidate = release.tagName,
                        current = installedApp.installedVersion,
                        allReleases = _state.value.allReleases,
                    )

                if (isDowngrade) {
                    _state.update {
                        it.copy(
                            downgradeWarning =
                                DowngradeWarning(
                                    packageName = installedApp.packageName,
                                    currentVersion = installedApp.installedVersion,
                                    targetVersion = release.tagName,
                                ),
                        )
                    }
                    return
                }
            }

            installAsset(
                downloadUrl = primary.downloadUrl,
                assetName = primary.name,
                sizeBytes = primary.size,
                releaseTag = release.tagName,
            )
        }
    }

    private fun overrideSigningKeyWarning() {
        val warning = _state.value.signingKeyWarning ?: return
        _state.update { it.copy(signingKeyWarning = null) }
        dismissDowngradeWarning()
        viewModelScope.launch {
            try {
                val ext = warning.pendingAssetName.substringAfterLast('.', "").lowercase()
                val installOutcome = installer.install(warning.pendingFilePath, ext)

                if (platform == Platform.ANDROID) {
                    saveInstalledAppToDatabase(
                        apkInfo = warning.pendingApkInfo,
                        assetName = warning.pendingAssetName,
                        assetUrl = warning.pendingDownloadUrl,
                        assetSize = warning.pendingSizeBytes,
                        releaseTag = warning.pendingReleaseTag,
                        isUpdate = warning.pendingIsUpdate,
                        installOutcome = installOutcome,
                    )
                }

                _state.value = _state.value.copy(downloadStage = DownloadStage.IDLE)
                currentAssetName = null
                appendLog(
                    assetName = warning.pendingAssetName,
                    size = warning.pendingSizeBytes,
                    tag = warning.pendingReleaseTag,
                    result = if (warning.pendingIsUpdate) LogResult.Updated else LogResult.Installed,
                )
            } catch (t: Throwable) {
                logger.error("Install after override failed: ${t.message}")
                _state.value =
                    _state.value.copy(
                        downloadStage = DownloadStage.IDLE,
                        installError = t.message,
                    )
                currentAssetName = null
            }
        }
    }

    private fun dismissDowngradeWarning() {
        _state.update {
            it.copy(
                downgradeWarning = null,
            )
        }
    }

    /**
     * Entry point for "download + install" from the install button.
     *
     * Hands the actual download off to [downloadOrchestrator] (so it
     * survives this screen being destroyed) and then observes the
     * orchestrator's state to mirror progress into [DetailsState] and
     * to dispatch the install dialog flow when bytes are on disk.
     *
     * Install policy is decided by installer type:
     *  - **Shizuku**: [InstallPolicy.AlwaysInstall] — orchestrator
     *    runs the install in its own scope. The user gets a silent
     *    install whether they stay on this screen or not. The
     *    PackageEventReceiver picks up `PACKAGE_REPLACED` and the
     *    installed-apps DB syncs without further work from the VM.
     *  - **Regular installer**: [InstallPolicy.InstallWhileForeground]
     *    — orchestrator parks the file at `AwaitingInstall` and the
     *    foreground VM (this one) runs the existing dialog flow
     *    (validation → fingerprint check → installer.install → DB
     *    save). If the screen leaves before bytes are done, the
     *    VM's `onCleared` calls [DownloadOrchestrator.downgradeToDeferred],
     *    the orchestrator notifies the user, and the apps row picks
     *    up the deferred install.
     */
    private fun installAsset(
        downloadUrl: String,
        assetName: String,
        sizeBytes: Long,
        releaseTag: String,
        isUpdate: Boolean = false,
    ) {
        // Cancel the existing observation job (if any) — but not the
        // orchestrator entry itself. A user re-tapping install for a
        // different asset should preempt the *previous* observer, not
        // the in-flight download (the orchestrator dedupes by
        // package name).
        currentDownloadJob?.cancel()
        val packageKey = orchestratorKey()
        val asset = _state.value.primaryAsset
        val repository = _state.value.repository
        if (asset == null || repository == null) {
            logger.warn("installAsset called with missing primaryAsset/repository")
            return
        }
        currentAssetName = assetName

        // ────────────────────────────────────────────────────────
        // SHORT-CIRCUIT: parked file matches what the user picked
        // ────────────────────────────────────────────────────────
        // If the user already deferred a download for this exact
        // (releaseTag, assetName) pair (e.g. they navigated away
        // from Details mid-download, the file got parked, and now
        // they're back), skip the orchestrator entirely and
        // dispatch the existing install dialog flow on the parked
        // file directly. Saves the network round-trip and the
        // disk space of a duplicate download.
        val parkedFilePath = parkedFilePathIfMatches(releaseTag, assetName)
        if (parkedFilePath != null) {
            logger.debug("Reusing parked file for $releaseTag / $assetName")
            currentDownloadJob =
                viewModelScope.launch {
                    try {
                        appendLog(
                            assetName = assetName,
                            size = sizeBytes,
                            tag = releaseTag,
                            result = LogResult.Downloaded,
                        )
                        installAsset(
                            isUpdate = isUpdate,
                            filePath = parkedFilePath,
                            assetName = assetName,
                            downloadUrl = downloadUrl,
                            sizeBytes = sizeBytes,
                            releaseTag = releaseTag,
                        )
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        logger.error("Install of parked file failed: ${t.message}")
                        _state.value =
                            _state.value.copy(
                                downloadStage = DownloadStage.IDLE,
                                installError = t.message,
                            )
                        currentAssetName = null
                        appendLog(
                            assetName = assetName,
                            size = sizeBytes,
                            tag = releaseTag,
                            result = Error(t.message),
                        )
                    }
                }
            return
        }

        appendLog(
            assetName = assetName,
            size = sizeBytes,
            tag = releaseTag,
            result =
                if (isUpdate) {
                    LogResult.UpdateStarted
                } else {
                    LogResult.DownloadStarted
                },
        )

        currentDownloadJob =
            viewModelScope.launch {
                try {
                    val installerType =
                        try {
                            tweaksRepository.getInstallerType().first()
                        } catch (e: kotlinx.coroutines.CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            InstallerType.DEFAULT
                        }

                    val policy =
                        when (installerType) {
                            InstallerType.SHIZUKU -> InstallPolicy.AlwaysInstall
                            InstallerType.DEFAULT -> InstallPolicy.InstallWhileForeground
                        }

                    downloadOrchestrator.enqueue(
                        DownloadSpec(
                            packageName = packageKey,
                            repoOwner = repository.owner.login,
                            repoName = repository.name,
                            asset = asset,
                            displayAppName = repository.name,
                            installPolicy = policy,
                            releaseTag = releaseTag,
                        ),
                    )

                    _state.value =
                        _state.value.copy(
                            downloadError = null,
                            installError = null,
                            downloadProgressPercent = null,
                            downloadStage = DownloadStage.DOWNLOADING,
                            downloadedBytes = 0L,
                            totalBytes = sizeBytes,
                            attestationStatus = AttestationStatus.UNCHECKED,
                        )

                    observeOrchestratorEntry(
                        packageKey = packageKey,
                        downloadUrl = downloadUrl,
                        assetName = assetName,
                        sizeBytes = sizeBytes,
                        releaseTag = releaseTag,
                        isUpdate = isUpdate,
                    )
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    logger.error("Install failed: ${t.message}")
                    t.printStackTrace()
                    _state.value =
                        _state.value.copy(
                            downloadStage = DownloadStage.IDLE,
                            installError = t.message,
                        )
                    currentAssetName = null
                    appendLog(
                        assetName = assetName,
                        size = sizeBytes,
                        tag = releaseTag,
                        result = Error(t.message),
                    )
                }
            }
    }

    /**
     * Returns the path of a parked install file iff the currently-tracked
     * app has one AND it represents *this exact* (releaseTag, assetName)
     * pair AND the file still exists on disk.
     *
     * Used as the short-circuit gate in [installAsset] to skip the
     * orchestrator round-trip when the bytes are already on disk.
     * Returns `null` (= "do a fresh download") in any of these cases:
     *  - app not tracked
     *  - no parked file
     *  - parked file represents a different version or asset
     *  - parked file no longer exists (manually deleted, etc.)
     */
    private fun parkedFilePathIfMatches(
        releaseTag: String,
        assetName: String,
    ): String? {
        val installedApp = _state.value.installedApp ?: return null
        val parkedPath = installedApp.pendingInstallFilePath ?: return null
        val parkedVersion = installedApp.pendingInstallVersion ?: return null
        val parkedAsset = installedApp.pendingInstallAssetName ?: return null
        if (parkedVersion != releaseTag) return null
        if (parkedAsset != assetName) return null
        // Verify the file still exists. If a user manually cleared
        // their downloads dir between parking and re-opening Details,
        // the column points at a stale path and we'd hand the
        // installer a missing file.
        return try {
            val file = File(parkedPath)
            if (file.exists() && file.length() > 0) parkedPath else null
        } catch (t: Throwable) {
            logger.warn("Failed to stat parked install file: ${t.message}")
            null
        }
    }

    /**
     * Stable orchestrator key for the currently-displayed app.
     *
     * Tracked apps key by `packageName` so the apps list and the
     * details screen point at the same orchestrator entry. Untracked
     * apps (fresh installs) key by `owner/repo` synthetic — real
     * package names never contain `/`, so there's no collision risk.
     *
     * After a fresh install completes, the InstalledApp row is created
     * with the real package name; subsequent updates use the real
     * key. The synthetic key is one-shot and short-lived.
     */
    private fun orchestratorKey(): String {
        val packageName = _state.value.installedApp?.packageName
        if (packageName != null) return packageName
        val owner = _state.value.repository?.owner?.login ?: return "unknown"
        val name = _state.value.repository?.name ?: return "unknown"
        return "$owner/$name"
    }

    /**
     * Subscribes to the orchestrator's entry for [packageKey] and
     * mirrors its state into [DetailsState]. When the entry reaches
     * [OrchestratorStage.AwaitingInstall] *and* the install policy is
     * [InstallPolicy.InstallWhileForeground] (i.e. not the Shizuku
     * silent path), kicks off the existing install dialog flow on
     * the file path.
     *
     * Suspends until the entry reaches a terminal state (`Completed`,
     * `Cancelled`, `Failed`, or removed from the map). Cancellation
     * of the *observer* doesn't cancel the orchestrator — that's the
     * whole point.
     */
    private suspend fun observeOrchestratorEntry(
        packageKey: String,
        downloadUrl: String,
        assetName: String,
        sizeBytes: Long,
        releaseTag: String,
        isUpdate: Boolean,
    ) {
        var installFired = false
        var telemetryStartFired = false
        downloadOrchestrator.observe(packageKey).collect { entry ->
            if (entry == null) {
                // Orchestrator dropped the entry (cancelled or
                // dismissed elsewhere). Tear down our local UI state
                // and exit the observer.
                if (_state.value.downloadStage != DownloadStage.IDLE) {
                    _state.value =
                        _state.value.copy(
                            downloadStage = DownloadStage.IDLE,
                            downloadProgressPercent = null,
                        )
                }
                currentAssetName = null
                return@collect
            }

            // Mirror progress into local state for the UI. Update
            // bytes too — the live byte counter is what users see
            // when content-length is small enough that the percent
            // doesn't tick smoothly. The orchestrator emits both on
            // every chunk so the UI gets a continuous update.
            _state.value =
                _state.value.copy(
                    downloadProgressPercent = entry.progressPercent,
                    downloadedBytes = entry.bytesDownloaded,
                    totalBytes = entry.totalBytes ?: sizeBytes,
                )

            when (entry.stage) {
                OrchestratorStage.Queued -> {
                    // Nothing UI-visible — same as DOWNLOADING placeholder
                    _state.value = _state.value.copy(downloadStage = DownloadStage.DOWNLOADING)
                }

                OrchestratorStage.Downloading -> {
                    _state.value = _state.value.copy(downloadStage = DownloadStage.DOWNLOADING)
                }

                OrchestratorStage.Installing -> {
                    // Either the orchestrator's bare-install path
                    // (Shizuku) or our own install fired below. Either
                    // way, surface the INSTALLING stage.
                    _state.value = _state.value.copy(downloadStage = DownloadStage.INSTALLING)

                    if (!telemetryStartFired) {
                        telemetryStartFired = true
                        _state.value.repository?.id?.let { id ->
                            telemetryRepository.recordReleaseDownloaded(id)
                            telemetryRepository.recordInstallStarted(id)
                        }
                    }
                }

                OrchestratorStage.AwaitingInstall -> {
                    // Bytes are on disk. For the foreground path
                    // (regular installer), this is our cue to run
                    // the existing dialog/validation/install flow.
                    // For the Shizuku path the orchestrator already
                    // moved past Installing → Completed before we
                    // ever see AwaitingInstall (it doesn't park).
                    if (installFired) return@collect
                    installFired = true
                    val filePath = entry.filePath ?: return@collect
                    _state.value =
                        _state.value.copy(downloadStage = DownloadStage.VERIFYING)
                    appendLog(
                        assetName = assetName,
                        size = sizeBytes,
                        tag = releaseTag,
                        result = LogResult.Downloaded,
                    )
                    if (!telemetryStartFired) {
                        telemetryStartFired = true
                        _state.value.repository?.id?.let { id ->
                            telemetryRepository.recordReleaseDownloaded(id)
                            telemetryRepository.recordInstallStarted(id)
                        }
                    }
                    // Run the existing install dialog flow on the
                    // downloaded file. This is the unchanged
                    // validation + fingerprint + installer + DB save
                    // path that the VM has always owned.
                    try {
                        installAsset(
                            isUpdate = isUpdate,
                            filePath = filePath,
                            assetName = assetName,
                            downloadUrl = downloadUrl,
                            sizeBytes = sizeBytes,
                            releaseTag = releaseTag,
                        )
                        _state.value.repository?.id?.let { telemetryRepository.recordInstallSucceeded(it) }
                        // Successful install — release the entry
                        // from the orchestrator so the apps row
                        // doesn't keep showing "ready to install".
                        downloadOrchestrator.dismiss(packageKey)
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (t: Throwable) {
                        logger.error("Foreground install failed: ${t.message}")
                        _state.value =
                            _state.value.copy(
                                downloadStage = DownloadStage.IDLE,
                                installError = t.message,
                            )
                        appendLog(
                            assetName = assetName,
                            size = sizeBytes,
                            tag = releaseTag,
                            result = Error(t.message),
                        )
                        _state.value.repository?.id?.let {
                            telemetryRepository.recordInstallFailed(it, t.message)
                        }
                    }
                }

                OrchestratorStage.Completed -> {
                    // Shizuku/AlwaysInstall path: orchestrator
                    // installed silently. Persist the DB row here —
                    // PackageEventReceiver only patches existing rows
                    // and would skip a fresh Shizuku install.
                    _state.value = _state.value.copy(downloadStage = DownloadStage.IDLE)
                    currentAssetName = null
                    appendLog(
                        assetName = assetName,
                        size = sizeBytes,
                        tag = releaseTag,
                        result = if (isUpdate) LogResult.Updated else LogResult.Installed,
                    )
                    _state.value.repository?.id?.let { telemetryRepository.recordInstallSucceeded(it) }

                    if (platform == Platform.ANDROID) {
                        val filePath = entry.filePath
                        if (filePath != null) {
                            runCatching {
                                val validation = installationManager.validateApk(
                                    filePath = filePath,
                                    isUpdate = isUpdate,
                                    trackedPackageName = _state.value.installedApp?.packageName,
                                )
                                if (validation is ApkValidationResult.Valid) {
                                    saveInstalledAppToDatabase(
                                        apkInfo = validation.apkInfo,
                                        assetName = assetName,
                                        assetUrl = downloadUrl,
                                        assetSize = sizeBytes,
                                        releaseTag = releaseTag,
                                        isUpdate = isUpdate,
                                        installOutcome = InstallOutcome.COMPLETED,
                                    )
                                } else {
                                    logger.warn(
                                        "Shizuku install completed but APK validation failed: $validation",
                                    )
                                }
                            }.onFailure { t ->
                                logger.error("Failed to persist Shizuku install: ${t.message}")
                            }
                        } else {
                            logger.warn("Shizuku install completed but filePath is null; DB not updated")
                        }
                    }

                    downloadOrchestrator.dismiss(packageKey)
                    return@collect
                }

                OrchestratorStage.Cancelled -> {
                    _state.value =
                        _state.value.copy(
                            downloadStage = DownloadStage.IDLE,
                            downloadProgressPercent = null,
                        )
                    currentAssetName = null
                    appendLog(
                        assetName = assetName,
                        size = sizeBytes,
                        tag = releaseTag,
                        result = LogResult.Cancelled,
                    )
                    return@collect
                }

                OrchestratorStage.Failed -> {
                    _state.value =
                        _state.value.copy(
                            downloadStage = DownloadStage.IDLE,
                            downloadError = entry.errorMessage,
                        )
                    currentAssetName = null
                    appendLog(
                        assetName = assetName,
                        size = sizeBytes,
                        tag = releaseTag,
                        result = Error(entry.errorMessage),
                    )
                    _state.value.repository?.id?.let {
                        telemetryRepository.recordInstallFailed(it, entry.errorMessage)
                    }
                    downloadOrchestrator.dismiss(packageKey)
                    return@collect
                }
            }
        }
    }

    private suspend fun installAsset(
        isUpdate: Boolean,
        filePath: String,
        assetName: String,
        downloadUrl: String,
        sizeBytes: Long,
        releaseTag: String,
    ) {
        _state.value = _state.value.copy(downloadStage = DownloadStage.INSTALLING)

        val ext = assetName.substringAfterLast('.', "").lowercase()
        val isApk = ext == "apk"
        var validatedApkInfo: ApkPackageInfo? = null

        if (isApk) {
            val validationResult =
                installationManager.validateApk(
                    filePath = filePath,
                    isUpdate = isUpdate,
                    trackedPackageName = _state.value.installedApp?.packageName,
                )

            when (validationResult) {
                is ApkValidationResult.ExtractionFailed -> {
                    // Don't block installation — proceed without
                    // validation (same as the Shizuku path).
                    // PackageEventReceiver will sync the DB post-install.
                    logger.warn(
                        "Could not extract APK info for $assetName, " +
                            "proceeding with unvalidated install",
                    )
                }

                is ApkValidationResult.PackageMismatch -> {
                    logger.error(
                        "Package name mismatch on update: " +
                            "APK=${validationResult.apkPackageName}, " +
                            "installed=${validationResult.installedPackageName}",
                    )
                    _state.value =
                        _state.value.copy(
                            downloadStage = DownloadStage.IDLE,
                            installError =
                                getString(
                                    Res.string.update_package_mismatch,
                                    validationResult.apkPackageName,
                                    validationResult.installedPackageName,
                                ),
                        )
                    currentAssetName = null
                    appendLog(
                        assetName = assetName,
                        size = sizeBytes,
                        tag = releaseTag,
                        result = Error("Package name mismatch"),
                    )
                    return
                }

                is ApkValidationResult.Valid -> {
                    validatedApkInfo = validationResult.apkInfo
                    val fpResult =
                        installationManager.checkSigningFingerprint(validationResult.apkInfo)
                    if (fpResult is FingerprintCheckResult.Mismatch) {
                        _state.update { state ->
                            state.copy(
                                signingKeyWarning =
                                    SigningKeyWarning(
                                        packageName = validationResult.apkInfo.packageName,
                                        expectedFingerprint = fpResult.expectedFingerprint,
                                        actualFingerprint = fpResult.actualFingerprint,
                                        pendingDownloadUrl = downloadUrl,
                                        pendingAssetName = assetName,
                                        pendingSizeBytes = sizeBytes,
                                        pendingReleaseTag = releaseTag,
                                        pendingIsUpdate = isUpdate,
                                        pendingFilePath = filePath,
                                        pendingApkInfo = validationResult.apkInfo,
                                    ),
                            )
                        }
                        appendLog(
                            assetName = assetName,
                            size = sizeBytes,
                            tag = releaseTag,
                            result = Error("Signing key changed"),
                        )
                        return
                    }
                }
            }
        }

        val installOutcome = installer.install(filePath, ext)

        // Launch attestation check asynchronously (non-blocking)
        launchAttestationCheck(filePath)

        if (platform == Platform.ANDROID && validatedApkInfo != null) {
            saveInstalledAppToDatabase(
                apkInfo = validatedApkInfo,
                assetName = assetName,
                assetUrl = downloadUrl,
                assetSize = sizeBytes,
                releaseTag = releaseTag,
                isUpdate = isUpdate,
                installOutcome = installOutcome,
            )
        } else if (platform != Platform.ANDROID) {
            viewModelScope.launch {
                _events.send(DetailsEvent.OnMessage(getString(Res.string.installer_saved_downloads)))
            }
        }

        _state.value = _state.value.copy(downloadStage = DownloadStage.IDLE)
        currentAssetName = null
        appendLog(
            assetName = assetName,
            size = sizeBytes,
            tag = releaseTag,
            result =
                if (isUpdate) {
                    LogResult.Updated
                } else {
                    LogResult.Installed
                },
        )
    }

    private fun launchAttestationCheck(filePath: String) {
        val repo = _state.value.repository ?: return
        val owner = repo.owner.login
        val repoName = repo.name

        _state.update { it.copy(attestationStatus = AttestationStatus.CHECKING) }

        viewModelScope.launch {
            val result = attestationVerifier.verify(owner, repoName, filePath)
            _state.update {
                it.copy(
                    attestationStatus = when (result) {
                        is VerificationResult.Verified -> AttestationStatus.VERIFIED
                        is VerificationResult.Unverified -> AttestationStatus.UNVERIFIED
                        is VerificationResult.Error -> AttestationStatus.UNABLE_TO_VERIFY
                    },
                )
            }
        }
    }

    private suspend fun saveInstalledAppToDatabase(
        apkInfo: ApkPackageInfo,
        assetName: String,
        assetUrl: String,
        assetSize: Long,
        releaseTag: String,
        isUpdate: Boolean,
        installOutcome: InstallOutcome,
    ) {
        val repo = _state.value.repository ?: return

        if (isUpdate) {
            installationManager.updateInstalledAppVersion(
                UpdateInstalledAppParams(
                    apkInfo = apkInfo,
                    assetName = assetName,
                    assetUrl = assetUrl,
                    releaseTag = releaseTag,
                    isPendingInstall = installOutcome != InstallOutcome.COMPLETED,
                ),
            )
        } else {
            // Snapshot the installable list as the user saw it at install
            // time — this is the reference the variant fingerprint is
            // relative to (pinning "the same kind of APK" means the same
            // choice among these specific siblings).
            val installable = _state.value.installableAssets
            val pickedIndex = installable
                .indexOfFirst { it.name == assetName }
                .takeIf { it >= 0 }
            val reloaded =
                installationManager.saveNewInstalledApp(
                    SaveInstalledAppParams(
                        repo = repo,
                        apkInfo = apkInfo,
                        assetName = assetName,
                        assetUrl = assetUrl,
                        assetSize = assetSize,
                        releaseTag = releaseTag,
                        isPendingInstall = installOutcome != InstallOutcome.COMPLETED,
                        isFavourite = _state.value.isFavourite,
                        siblingAssetCount = installable.size,
                        pickedAssetIndex = pickedIndex,
                    ),
                )
            _state.value = _state.value.copy(installedApp = reloaded)
        }
    }

    /**
     * "Download only" entry point — used by the action that
     * downloads an asset without auto-installing it (e.g. for users
     * who want to side-load via a different installer). Routes
     * through the orchestrator with [InstallPolicy.DeferUntilUserAction]
     * so the file is parked at AwaitingInstall and the user can pick
     * it up from the apps row whenever they're ready.
     */
    private fun downloadAsset(
        downloadUrl: String,
        assetName: String,
        sizeBytes: Long,
        releaseTag: String,
    ) {
        currentDownloadJob?.cancel()
        val packageKey = orchestratorKey()
        val repository = _state.value.repository ?: return
        // Use the exact asset the user tapped, not the auto-picked primary.
        val asset = _state.value.selectedRelease?.assets
            ?.find { it.downloadUrl == downloadUrl }
            ?: _state.value.primaryAsset
            ?: return
        currentAssetName = assetName

        appendLog(
            assetName = assetName,
            size = sizeBytes,
            tag = releaseTag,
            result = LogResult.DownloadStarted,
        )
        _state.value =
            _state.value.copy(
                isDownloading = true,
                downloadError = null,
                installError = null,
                downloadProgressPercent = null,
            )

        currentDownloadJob =
            viewModelScope.launch {
                try {
                    downloadOrchestrator.enqueue(
                        DownloadSpec(
                            packageName = packageKey,
                            repoOwner = repository.owner.login,
                            repoName = repository.name,
                            asset = asset,
                            displayAppName = repository.name,
                            installPolicy = InstallPolicy.DeferUntilUserAction,
                            releaseTag = releaseTag,
                        ),
                    )
                    observeOrchestratorEntry(
                        packageKey = packageKey,
                        downloadUrl = downloadUrl,
                        assetName = assetName,
                        sizeBytes = sizeBytes,
                        releaseTag = releaseTag,
                        isUpdate = false,
                    )
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (t: Throwable) {
                    _state.value =
                        _state.value.copy(
                            isDownloading = false,
                            downloadError = t.message,
                        )
                    currentAssetName = null
                    appendLog(
                        assetName = assetName,
                        size = sizeBytes,
                        tag = releaseTag,
                        result = LogResult.Error(t.message),
                    )
                }
            }
    }

    @OptIn(ExperimentalTime::class)
    private fun appendLog(
        assetName: String,
        size: Long,
        tag: String,
        result: LogResult,
    ) {
        val now =
            System
                .now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .format(
                    LocalDateTime.Format {
                        year()
                        char('-')
                        monthNumber()
                        char('-')
                        day()
                        char(' ')
                        hour()
                        char(':')
                        minute()
                        char(':')
                        second()
                    },
                )
        val newItem =
            InstallLogItem(
                timeIso = now,
                assetName = assetName,
                assetSizeBytes = size,
                releaseTag = tag,
                result = result,
            )
        _state.value =
            _state.value.copy(
                installLogs = listOf(newItem) + _state.value.installLogs,
            )
    }

    override fun onCleared() {
        super.onCleared()
        // Cancel the orchestrator OBSERVER (not the orchestrator
        // entry itself). The download keeps running in the
        // application-scoped orchestrator scope.
        currentDownloadJob?.cancel()

        // Tell the orchestrator that the foreground watcher is gone:
        // any in-flight download with policy InstallWhileForeground
        // should switch to DeferUntilUserAction so the file gets
        // parked + the user gets a notification when bytes are done.
        // Race-safe — the orchestrator handles "already past park
        // time" by retroactively notifying.
        //
        // NonCancellable so the call runs to completion even though
        // viewModelScope is being torn down around us.
        val packageKey = orchestratorKey()
        viewModelScope.launch(NonCancellable) {
            try {
                downloadOrchestrator.downgradeToDeferred(packageKey)
            } catch (t: Throwable) {
                logger.error("Failed to downgrade orchestrator on screen leave: ${t.message}")
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun loadInitial() {
        viewModelScope.launch {
            try {
                rateLimited.set(false)

                _state.value = _state.value.copy(isLoading = true, errorMessage = null)

                val syncResult = syncInstalledAppsUseCase()
                if (syncResult.isFailure) {
                    logger.warn("Sync had issues but continuing: ${syncResult.exceptionOrNull()?.message}")
                }

                val repo =
                    if (ownerParam.isNotEmpty() && repoParam.isNotEmpty()) {
                        detailsRepository.getRepositoryByOwnerAndName(ownerParam, repoParam)
                    } else {
                        detailsRepository.getRepositoryById(repositoryId)
                    }
                launch { seenReposRepository.markAsSeen(repo) }

                val isFavoriteDeferred =
                    async {
                        try {
                            favouritesRepository.isFavoriteSync(repo.id)
                        } catch (_: RateLimitException) {
                            rateLimited.set(true)
                            null
                        } catch (t: Throwable) {
                            logger.error("Failed to load if repo is favourite: ${t.localizedMessage}")
                            false
                        }
                    }
                val isFavorite = isFavoriteDeferred.await()
                val isStarredDeferred =
                    async {
                        try {
                            starredRepository.isStarred(repo.id)
                        } catch (_: RateLimitException) {
                            rateLimited.set(true)
                            null
                        } catch (t: Throwable) {
                            logger.error("Failed to load if repo is starred: ${t.localizedMessage}")
                            false
                        }
                    }
                val isStarred = isStarredDeferred.await()

                val owner = repo.owner.login
                val name = repo.name

                _state.value =
                    _state.value.copy(
                        repository = repo,
                        isFavourite = isFavorite == true,
                        isStarred = isStarred == true,
                    )

                val allReleasesDeferred =
                    async {
                        try {
                            detailsRepository.getAllReleases(
                                owner = owner,
                                repo = name,
                                defaultBranch = repo.defaultBranch,
                            ) to false
                        } catch (_: RateLimitException) {
                            rateLimited.set(true)
                            emptyList<GithubRelease>() to true
                        } catch (t: Throwable) {
                            logger.warn("Failed to load releases: ${t.message}")
                            emptyList<GithubRelease>() to true
                        }
                    }

                val statsDeferred =
                    async {
                        try {
                            detailsRepository.getRepoStats(owner, name)
                        } catch (_: RateLimitException) {
                            rateLimited.set(true)
                            null
                        } catch (_: Throwable) {
                            null
                        }
                    }

                val readmeDeferred =
                    async {
                        try {
                            detailsRepository.getReadme(
                                owner = owner,
                                repo = name,
                                defaultBranch = repo.defaultBranch,
                            )
                        } catch (_: RateLimitException) {
                            rateLimited.set(true)
                            null
                        } catch (_: Throwable) {
                            null
                        }
                    }

                val userProfileDeferred =
                    async {
                        try {
                            detailsRepository.getUserProfile(owner)
                        } catch (_: RateLimitException) {
                            rateLimited.set(true)
                            null
                        } catch (t: Throwable) {
                            logger.warn("Failed to load user profile: ${t.message}")
                            null
                        }
                    }

                val installedAppDeferred =
                    async {
                        try {
                            val dbApp = installedAppsRepository.getAppByRepoId(repo.id)

                            if (dbApp != null) {
                                if (dbApp.isPendingInstall &&
                                    packageMonitor.isPackageInstalled(dbApp.packageName)
                                ) {
                                    installedAppsRepository.updatePendingStatus(
                                        dbApp.packageName,
                                        false,
                                    )
                                    installedAppsRepository.getAppByPackage(dbApp.packageName)
                                } else {
                                    dbApp
                                }
                            } else {
                                null
                            }
                        } catch (_: RateLimitException) {
                            rateLimited.set(true)
                            null
                        } catch (t: Throwable) {
                            logger.error("Failed to load installed app: ${t.message}")
                            null
                        }
                    }

                val isObtainiumEnabled = platform == Platform.ANDROID
                val isAppManagerEnabled = platform == Platform.ANDROID

                val (allReleases, releasesFailed) = allReleasesDeferred.await()
                val stats = statsDeferred.await()
                val readme = readmeDeferred.await()
                val userProfile = userProfileDeferred.await()
                val installedApp = installedAppDeferred.await()

                if (rateLimited.get()) {
                    // Any deferred tripping the rate-limit flag leaves the UI
                    // in an incomplete state. Flag the releases section as
                    // failed so it renders its FAILED card with a Retry
                    // affordance instead of the misleading EMPTY card ("no
                    // releases published yet") — the default would be EMPTY
                    // because allReleases stays at its initial empty list.
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        releasesLoadFailed = true,
                    )
                    return@launch
                }

                val selectedRelease =
                    allReleases.firstOrNull { !it.isEffectivelyPreRelease() }
                        ?: allReleases.firstOrNull()

                val (installable, primary) = recomputeAssetsForRelease(selectedRelease, installedApp)

                val isObtainiumAvailable = installer.isObtainiumInstalled()
                val isAppManagerAvailable = installer.isAppManagerInstalled()

                val liquidGlassEnabled = tweaksRepository.getLiquidGlassEnabled().first()

                logger.debug("Loaded repo: ${repo.name}, installedApp: ${installedApp?.packageName}")

                val insights = computeReleaseInsights(allReleases, installedApp)

                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        errorMessage = null,
                        repository = repo,
                        allReleases = allReleases,
                        releasesLoadFailed = releasesFailed,
                        isRetryingReleases = false,
                        selectedRelease = selectedRelease,
                        selectedReleaseCategory = ReleaseCategory.STABLE,
                        stats = stats,
                        readmeMarkdown = readme?.first,
                        readmeLanguage = readme?.second,
                        installableAssets = installable,
                        primaryAsset = primary,
                        userProfile = userProfile,
                        systemArchitecture = installer.detectSystemArchitecture(),
                        isObtainiumAvailable = isObtainiumAvailable,
                        isObtainiumEnabled = isObtainiumEnabled,
                        isAppManagerAvailable = isAppManagerAvailable,
                        isAppManagerEnabled = isAppManagerEnabled,
                        installedApp = installedApp,
                        deviceLanguageCode = translationRepository.getDeviceLanguageCode(),
                        isComingFromUpdate = isComingFromUpdate,
                        isLiquidGlassEnabled = liquidGlassEnabled,
                        stalledStableSinceDays = insights.stalledStableSinceDays,
                        mergedChangelog = insights.mergedChangelog,
                        mergedChangelogBaseTag = insights.mergedChangelogBaseTag,
                        latestStableHasInstallableAsset =
                            insights.latestStableHasInstallableAsset,
                    )

                telemetryRepository.recordRepoViewed(repo.id)

                observeInstalledApp(repo.id)
            } catch (e: RateLimitException) {
                logger.error("Rate limited: ${e.message}")
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        errorMessage = getString(Res.string.rate_limit_exceeded),
                    )
            } catch (t: Throwable) {
                logger.error("Details load failed: ${t.message}")
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Failed to load details",
                    )
            }
        }
    }

    private fun translateContent(
        text: String,
        targetLanguageCode: String,
        updateState: (TranslationState) -> Unit,
        getCurrentState: () -> TranslationState,
    ): Job =
        viewModelScope.launch {
            try {
                updateState(
                    getCurrentState().copy(
                        isTranslating = true,
                        error = null,
                        targetLanguageCode = targetLanguageCode,
                    ),
                )

                val result =
                    translationRepository.translate(
                        text = text,
                        targetLanguage = targetLanguageCode,
                    )

                val langDisplayName =
                    SupportedLanguages.all
                        .find { it.code == targetLanguageCode }
                        ?.displayName
                        ?: targetLanguageCode

                updateState(
                    TranslationState(
                        isTranslating = false,
                        translatedText = result.translatedText,
                        isShowingTranslation = true,
                        targetLanguageCode = targetLanguageCode,
                        targetLanguageDisplayName = langDisplayName,
                        detectedSourceLanguage = result.detectedSourceLanguage,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Translation failed: ${e.message}")
                updateState(
                    getCurrentState().copy(
                        isTranslating = false,
                        error = e.message,
                    ),
                )
                _events.send(
                    DetailsEvent.OnMessage(getString(Res.string.translation_failed)),
                )
            }
        }

    private companion object {
        const val OBTAINIUM_REPO_ID: Long = 523534328
        const val APP_MANAGER_REPO_ID: Long = 268006778
        const val STALLED_STABLE_THRESHOLD_DAYS = 180
        const val MILLIS_PER_DAY = 86_400_000L
    }
}
