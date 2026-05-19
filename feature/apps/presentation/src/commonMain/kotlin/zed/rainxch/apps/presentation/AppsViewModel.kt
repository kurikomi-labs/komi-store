package zed.rainxch.apps.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.apps.domain.repository.AppsRepository
import zed.rainxch.apps.presentation.mappers.toDomain
import zed.rainxch.apps.presentation.mappers.toUi
import zed.rainxch.apps.presentation.model.AppItem
import zed.rainxch.apps.presentation.model.AppSortRule
import zed.rainxch.apps.presentation.model.GithubAssetUi
import zed.rainxch.apps.presentation.model.InstalledAppUi
import zed.rainxch.apps.presentation.model.UpdateAllProgress
import zed.rainxch.apps.presentation.model.UpdateState
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.InstalledApp
import zed.rainxch.core.domain.model.InstallerType
import zed.rainxch.core.domain.model.RateLimitException
import zed.rainxch.core.domain.network.Downloader
import zed.rainxch.core.domain.repository.ExternalImportRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.TweaksRepository
import zed.rainxch.core.domain.system.DownloadOrchestrator
import zed.rainxch.core.domain.system.DownloadSpec
import zed.rainxch.core.domain.system.DownloadStage as OrchestratorStage
import zed.rainxch.core.domain.system.InstallPolicy
import zed.rainxch.core.domain.system.Installer
import zed.rainxch.core.domain.system.SystemInstallSerializer
import zed.rainxch.core.domain.use_cases.SyncInstalledAppsUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import zed.rainxch.core.domain.util.AssetFilter
import zed.rainxch.core.domain.util.AssetVariant
import zed.rainxch.core.domain.utils.BrowserHelper
import zed.rainxch.core.domain.utils.ShareManager
import zed.rainxch.githubstore.core.presentation.res.*
import java.io.File

class AppsViewModel(
    private val appsRepository: AppsRepository,
    private val installer: Installer,
    private val downloader: Downloader,
    private val installedAppsRepository: InstalledAppsRepository,
    private val syncInstalledAppsUseCase: SyncInstalledAppsUseCase,
    private val logger: GitHubStoreLogger,
    private val shareManager: ShareManager,
    private val tweaksRepository: TweaksRepository,
    private val downloadOrchestrator: DownloadOrchestrator,
    private val externalImportRepository: ExternalImportRepository,
    private val systemInstallSerializer: SystemInstallSerializer,
    private val browserHelper: BrowserHelper,
) : ViewModel() {
    companion object {
        private const val BANNER_THRESHOLD = 1
        private const val UPDATE_CHECK_COOLDOWN_MS = 30 * 60 * 1000L
        private const val KAO_OPEN_LETTER_URL = "https://keepandroidopen.org/open-letter/"
    }

    private var hasLoadedInitialData = false
    private val activeUpdates = mutableMapOf<String, Job>()
    private var updateAllJob: Job? = null
    private var lastAutoCheckTimestamp: Long = 0L

    // Synchronous mirror of the persisted dismiss watermark. The persisted
    // write goes through DataStore async, so the import-banner flow could
    // re-emit with the OLD watermark and re-flash the banner before the
    // write completes. This in-memory shadow is set BEFORE the launch and
    // OR'd into shouldShow so the suppression is immediate.
    @Volatile
    private var localBannerDismissedAtCount: Int = 0

    /** Debounced re-runs of the live preview in the advanced settings sheet. */
    private var advancedPreviewJob: Job? = null

    private val _state = MutableStateFlow(AppsState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    loadApps()
                    observePendingExternalImports()
                    observeKaoBannerDismissed()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = AppsState(),
            )

    private fun observeKaoBannerDismissed() {
        viewModelScope.launch {
            tweaksRepository.getKaoBannerDismissed().collect { dismissed ->
                _state.update { it.copy(showKaoBanner = !dismissed) }
            }
        }
    }

    private fun observePendingExternalImports() {
        viewModelScope.launch {
            externalImportRepository.pendingCandidateCountFlow()
                .combine(tweaksRepository.getExternalImportBannerDismissedAtCount()) { count, dismissedAt ->
                    count to dismissedAt
                }
                .collect { (count, dismissedAt) ->
                    // The effective watermark is the max of the persisted value and the
                    // synchronous local one. The local shadow is set immediately by the
                    // Review/Dismiss handlers so a flow emission that beats the DataStore
                    // write still sees a watermark that suppresses the banner.
                    val effectiveDismissedAt = maxOf(dismissedAt, localBannerDismissedAtCount)
                    val shouldShow = count >= BANNER_THRESHOLD && count > effectiveDismissedAt
                    _state.update {
                        it.copy(
                            pendingExternalImportCount = count,
                            showImportProposalBanner = shouldShow && !it.isExternalImportInFlight,
                        )
                    }
                }
        }
    }

    private val _events = Channel<AppsEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private fun loadApps() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            try {
                val syncResult = syncInstalledAppsUseCase()
                if (syncResult.isFailure) {
                    logger.error("Sync had issues but continuing: ${syncResult.exceptionOrNull()?.message}")
                }

                combine(
                    appsRepository.getApps(),
                    tweaksRepository.getAppsSortRule(),
                ) { apps, sortStored ->
                    apps to AppSortRule.fromName(sortStored)
                }.collect { (apps, sortRule) ->
                    val appItems =
                        apps
                            .map { it.toUi() }
                            .map { app ->
                                val existing =
                                    _state.value.apps.find {
                                        it.installedApp.packageName == app.packageName
                                    }
                                AppItem(
                                    installedApp = app,
                                    updateState = existing?.updateState ?: UpdateState.Idle,
                                    downloadProgress = existing?.downloadProgress,
                                    error = existing?.error,
                                )
                            }.sortedWith(appComparator(sortRule))
                            .toImmutableList()

                    _state.update {
                        it.copy(
                            apps = appItems,
                            sortRule = sortRule,
                            isLoading = false,
                            updateAllButtonEnabled =
                                appItems.any { item ->
                                    item.installedApp.isUpdateAvailable
                                },
                        )
                    }

                    filterApps()
                }
            } catch (e: Exception) {
                logger.error("Failed to load apps: ${e.message}")
                _state.update {
                    it.copy(isLoading = false)
                }
            }

            autoCheckForUpdatesIfNeeded()
        }
    }

    private fun autoCheckForUpdatesIfNeeded() {
        val now = System.currentTimeMillis()
        if (now - lastAutoCheckTimestamp < UPDATE_CHECK_COOLDOWN_MS) {
            logger.debug("Skipping auto-check: last check was ${(now - lastAutoCheckTimestamp) / 1000}s ago")
            return
        }
        checkAllForUpdates()
    }

    private fun checkAllForUpdates() {
        viewModelScope.launch {
            // Stamp the inflight guard at launch start — a second resume
            // that races the network call finds the cooldown gate already
            // tripped and exits early. `lastCheckedTimestamp` (the UI
            // "last checked" indicator) still only advances on success so
            // a failed check doesn't lie to the user — matches the
            // `refresh()` semantics.
            lastAutoCheckTimestamp = System.currentTimeMillis()
            _state.update { it.copy(isCheckingForUpdates = true) }
            try {
                syncInstalledAppsUseCase()
                installedAppsRepository.checkAllForUpdates()
                _state.update {
                    it.copy(lastCheckedTimestamp = System.currentTimeMillis())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Check all for updates failed: ${e.message}")
            } finally {
                _state.update { it.copy(isCheckingForUpdates = false) }
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            try {
                syncInstalledAppsUseCase()
                installedAppsRepository.checkAllForUpdates()
                val now = System.currentTimeMillis()
                lastAutoCheckTimestamp = now
                _state.update { it.copy(lastCheckedTimestamp = now) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Refresh failed: ${e.message}")
            } finally {
                _state.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onAction(action: AppsAction) {
        when (action) {
            AppsAction.OnNavigateBackClick -> {
            }

            AppsAction.OnAddFromStarredClick -> {
            }

            is AppsAction.OnSearchChange -> {
                _state.update {
                    it.copy(searchQuery = action.query)
                }

                filterApps()
            }

            is AppsAction.OnSortRuleSelected -> {
                _state.update {
                    it.copy(sortRule = action.sortRule)
                }

                filterApps()

                viewModelScope.launch {
                    try {
                        tweaksRepository.setAppsSortRule(action.sortRule.name)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logger.error("Failed to persist apps sort rule: ${e.message}")
                    }
                }
            }

            is AppsAction.OnOpenApp -> {
                openApp(action.app)
            }

            is AppsAction.OnUpdateApp -> {
                // If the app's pinned variant has gone missing in the
                // latest release we don't know what to download — open
                // the picker first and resume the update after the user
                // chooses. Saves them from a wrong-variant install.
                if (action.app.preferredVariantStale) {
                    openVariantPicker(action.app, resumeUpdateAfterPick = true)
                } else {
                    updateSingleApp(action.app)
                }
            }

            is AppsAction.OnInstallPendingApp -> {
                installPendingApp(action.app)
            }

            is AppsAction.OnDiscardPendingInstall -> {
                _state.update { it.copy(appPendingDiscard = action.app) }
            }

            is AppsAction.OnConfirmDiscardPendingInstall -> {
                _state.update { it.copy(appPendingDiscard = null) }
                discardPendingInstall(action.app)
            }

            AppsAction.OnDismissDiscardPendingDialog -> {
                _state.update { it.copy(appPendingDiscard = null) }
            }

            is AppsAction.OnCancelUpdate -> {
                cancelUpdate(action.packageName)
            }

            AppsAction.OnUpdateAll -> {
                updateAllApps()
            }

            AppsAction.OnCancelUpdateAll -> {
                cancelAllUpdates()
            }

            AppsAction.OnCheckAllForUpdates -> {
                checkAllForUpdates()
            }

            AppsAction.OnRefresh -> {
                refresh()
            }

            AppsAction.OnLifecycleResume -> {
                autoCheckForUpdatesIfNeeded()
            }

            AppsAction.OnToggleUpToDateSection -> {
                _state.update { it.copy(isUpToDateSectionExpanded = !it.isUpToDateSectionExpanded) }
            }

            is AppsAction.OnNavigateToRepo -> {
                viewModelScope.launch {
                    _events.send(AppsEvent.NavigateToRepo(action.repoId, action.sourceHost))
                }
            }

            is AppsAction.OnUninstallApp -> {
                _state.update { it.copy(appPendingUninstall = action.app) }
            }

            AppsAction.OnAddByLinkClick -> {
                openLinkSheet()
            }

            AppsAction.OnDismissLinkSheet -> {
                dismissLinkSheet()
            }

            is AppsAction.OnDeviceAppSearchChange -> {
                _state.update { it.copy(deviceAppSearchQuery = action.query) }
            }

            is AppsAction.OnDeviceAppSelected -> {
                startSmartMatch(action.app)
            }

            is AppsAction.OnLinkSuggestionSelected -> {
                _state.update {
                    it.copy(
                        repoUrl = "https://github.com/${action.owner}/${action.repo}",
                        repoValidationError = null,
                    )
                }
                validateAndLinkRepo()
            }

            AppsAction.OnLinkEnterUrlManually -> {
                _state.update {
                    it.copy(
                        linkStep = LinkStep.EnterUrl,
                        repoUrl = "",
                        repoValidationError = null,
                        fetchedRepoInfo = null,
                    )
                }
            }

            AppsAction.OnRetryLinkSearch -> {
                val app = _state.value.selectedDeviceApp ?: return
                startSmartMatch(app)
            }

            is AppsAction.OnRepoUrlChanged -> {
                _state.update {
                    it.copy(
                        repoUrl = action.url,
                        repoValidationError = null,
                    )
                }
            }

            AppsAction.OnValidateAndLinkRepo -> {
                validateAndLinkRepo()
            }

            AppsAction.OnBackToAppPicker -> {
                _state.update {
                    it.copy(
                        linkStep = LinkStep.PickApp,
                        selectedDeviceApp = null,
                        repoUrl = "",
                        repoValidationError = null,
                        fetchedRepoInfo = null,
                        linkInstallableAssets = persistentListOf(),
                        linkSelectedAsset = null,
                        linkDownloadProgress = null,
                        linkSuggestions = persistentListOf(),
                        linkSearchError = null,
                        linkSearchLoading = false,
                    )
                }
            }

            AppsAction.OnBackToSmartMatch -> {
                _state.update {
                    it.copy(
                        linkStep = LinkStep.SmartMatch,
                        repoUrl = "",
                        repoValidationError = null,
                        fetchedRepoInfo = null,
                        linkInstallableAssets = persistentListOf(),
                        linkSelectedAsset = null,
                        linkDownloadProgress = null,
                    )
                }
            }

            is AppsAction.OnLinkAssetSelected -> {
                validateWithAsset(action.asset)
            }

            AppsAction.OnBackToEnterUrl -> {
                _state.update {
                    it.copy(
                        linkStep = LinkStep.EnterUrl,
                        linkInstallableAssets = persistentListOf(),
                        linkSelectedAsset = null,
                        linkDownloadProgress = null,
                        linkValidationStatus = null,
                        repoValidationError = null,
                        linkAssetFilter = "",
                        linkAssetFilterError = null,
                        linkFallbackToOlder = false,
                    )
                }
            }

            is AppsAction.OnLinkAssetFilterChanged -> {
                onLinkAssetFilterChanged(action.filter)
            }

            is AppsAction.OnLinkFallbackToggled -> {
                _state.update { it.copy(linkFallbackToOlder = action.enabled) }
            }

            is AppsAction.OnTogglePreReleases -> {
                togglePreReleases(action.packageName, action.enabled)
            }

            is AppsAction.OnToggleUpdateCheck -> {
                toggleUpdateCheck(action.packageName, action.enabled)
            }

            is AppsAction.OnSkipReleaseTag -> {
                skipReleaseTag(action.packageName, action.tag)
            }

            is AppsAction.OnUnskipReleaseTag -> {
                unskipReleaseTag(action.packageName)
            }

            is AppsAction.OnOpenAdvancedSettings -> {
                openAdvancedSettings(action.app)
            }

            AppsAction.OnDismissAdvancedSettings -> {
                _state.update {
                    it.copy(
                        advancedSettingsApp = null,
                        advancedFilterDraft = "",
                        advancedFallbackDraft = false,
                        advancedFilterError = null,
                        advancedPreviewLoading = false,
                        advancedPreviewMatched = persistentListOf(),
                        advancedPreviewTag = null,
                        advancedPreviewMessage = null,
                        advancedSavingFilter = false,
                    )
                }
                advancedPreviewJob?.cancel()
                advancedPreviewJob = null
            }

            is AppsAction.OnAdvancedFilterChanged -> {
                onAdvancedFilterChanged(action.filter)
            }

            is AppsAction.OnAdvancedFallbackToggled -> {
                _state.update { it.copy(advancedFallbackDraft = action.enabled) }
                schedulePreviewRefresh()
            }

            AppsAction.OnAdvancedSaveFilter -> {
                saveAdvancedSettings()
            }

            AppsAction.OnAdvancedClearFilter -> {
                _state.update {
                    it.copy(
                        advancedFilterDraft = "",
                        advancedFilterError = null,
                    )
                }
                schedulePreviewRefresh()
            }

            AppsAction.OnAdvancedRefreshPreview -> {
                refreshAdvancedPreview()
            }

            is AppsAction.OnOpenVariantPicker -> {
                openVariantPicker(action.app, action.resumeUpdateAfterPick)
            }

            AppsAction.OnDismissVariantPicker -> {
                _state.update {
                    it.copy(
                        variantPickerApp = null,
                        variantPickerOptions = persistentListOf(),
                        variantPickerCurrentVariant = null,
                        variantPickerError = null,
                        variantPickerLoading = false,
                        variantPickerResumeUpdateAfterPick = false,
                    )
                }
            }

            is AppsAction.OnVariantSelected -> {
                saveVariantSelection(action.variant)
            }

            AppsAction.OnResetVariantToAuto -> {
                saveVariantSelection(null)
            }

            AppsAction.OnExportApps -> {
                exportApps()
            }

            AppsAction.OnExportObtainium -> {
                exportObtainium()
            }

            AppsAction.OnImportApps -> {
                importAppsFromFile()
            }

            AppsAction.OnDismissImportSummary -> {
                _state.update { it.copy(importSummary = null) }
            }

            AppsAction.OnDismissKaoBanner -> {
                viewModelScope.launch {
                    tweaksRepository.setKaoBannerDismissed(true)
                }
            }

            AppsAction.OnKaoLearnMore -> {
                browserHelper.openUrl(KAO_OPEN_LETTER_URL) { error ->
                    logger.warn("Failed to open KAO open letter: $error")
                }
            }

            is AppsAction.OnUninstallConfirmed -> {
                uninstallApp(action.app)
                _state.update { it.copy(appPendingUninstall = null) }
            }

            AppsAction.OnDismissUninstallDialog -> {
                _state.update { it.copy(appPendingUninstall = null) }
            }

            AppsAction.OnImportProposalReview -> {
                val current = _state.value.pendingExternalImportCount
                // Set the local watermark BEFORE the launch so a racing flow
                // emission can't recompute shouldShow=true on the stale persisted
                // watermark. observePendingExternalImports OR's this with the
                // persisted value via maxOf().
                localBannerDismissedAtCount = maxOf(localBannerDismissedAtCount, current)
                _state.update { it.copy(showImportProposalBanner = false) }
                viewModelScope.launch {
                    runCatching { tweaksRepository.setExternalImportBannerDismissedAtCount(current) }
                    _events.send(AppsEvent.NavigateToExternalImport)
                }
            }

            AppsAction.OnImportProposalDismiss -> {
                val current = _state.value.pendingExternalImportCount
                localBannerDismissedAtCount = maxOf(localBannerDismissedAtCount, current)
                _state.update { it.copy(showImportProposalBanner = false) }
                viewModelScope.launch {
                    runCatching { tweaksRepository.setExternalImportBannerDismissedAtCount(current) }
                }
            }

            AppsAction.OnRescanForGithubApps -> {
                // Manual rescan resets the banner watermark so the user sees
                // everything fresh; the wizard's startScanIfIdle then runs
                // a full scan + match cycle on entry.
                localBannerDismissedAtCount = 0
                _state.update { it.copy(showImportProposalBanner = false) }
                viewModelScope.launch {
                    runCatching { tweaksRepository.setExternalImportBannerDismissedAtCount(0) }
                    _events.send(AppsEvent.NavigateToExternalImport)
                }
            }
        }
    }

    private fun filterApps() {
        _state.update { current ->
            current.copy(
                filteredApps = computeFilteredApps(current.apps, current.searchQuery, current.sortRule),
            )
        }
    }

    private fun computeFilteredApps(
        apps: ImmutableList<AppItem>,
        query: String,
        sortRule: AppSortRule = _state.value.sortRule,
    ): ImmutableList<AppItem> =
        if (query.isBlank()) {
            apps
                .sortedWith(appComparator(sortRule))
                .toImmutableList()
        } else {
            apps
                .filter { appItem ->
                    appItem.installedApp.appName.contains(query, ignoreCase = true) ||
                        appItem.installedApp.repoOwner.contains(query, ignoreCase = true)
                }.sortedWith(appComparator(sortRule))
                .toImmutableList()
        }

    private fun appComparator(sortRule: AppSortRule): Comparator<AppItem> {
        val updatesFirst = compareByDescending<AppItem> { it.installedApp.isUpdateAvailable }
        return when (sortRule) {
            AppSortRule.UpdatesFirst ->
                updatesFirst
                    .thenByDescending { it.installedApp.latestReleasePublishedAt ?: "" }
                    .thenBy { it.installedApp.appName.lowercase() }

            AppSortRule.RecentlyUpdated ->
                compareByDescending<AppItem> { it.installedApp.lastUpdatedAt }
                    .thenByDescending { it.installedApp.isUpdateAvailable }
                    .thenBy { it.installedApp.appName.lowercase() }

            AppSortRule.Name ->
                compareBy<AppItem> { it.installedApp.appName.lowercase() }
                    .thenByDescending { it.installedApp.isUpdateAvailable }
        }
    }

    private fun togglePreReleases(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                installedAppsRepository.setIncludePreReleases(packageName, enabled)
                installedAppsRepository.checkForUpdates(packageName)
            } catch (e: Exception) {
                logger.error("Failed to toggle pre-releases for $packageName: ${e.message}")
            }
        }
    }

    private fun toggleUpdateCheck(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            try {
                installedAppsRepository.setUpdateCheckEnabled(packageName, enabled)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to toggle update check for $packageName: ${e.message}")
            }
        }
    }

    private fun skipReleaseTag(packageName: String, tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            try {
                installedAppsRepository.setSkippedReleaseTag(packageName, trimmed)
                _events.send(AppsEvent.ShowSuccess(getString(Res.string.apps_skip_version_snackbar, trimmed)))
                installedAppsRepository.checkForUpdates(packageName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to skip release $trimmed for $packageName: ${e.message}")
                _events.send(AppsEvent.ShowError(getString(Res.string.apps_skip_version_error)))
            }
        }
    }

    private fun unskipReleaseTag(packageName: String) {
        viewModelScope.launch {
            try {
                installedAppsRepository.setSkippedReleaseTag(packageName, null)
                installedAppsRepository.checkForUpdates(packageName)
                _events.send(AppsEvent.ShowSuccess(getString(Res.string.apps_unskip_version_snackbar)))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to unskip release for $packageName: ${e.message}")
                _events.send(AppsEvent.ShowError(getString(Res.string.apps_unskip_version_error)))
            }
        }
    }

    private fun openAdvancedSettings(app: InstalledAppUi) {
        _state.update {
            it.copy(
                advancedSettingsApp = app,
                advancedFilterDraft = app.assetFilterRegex.orEmpty(),
                advancedFallbackDraft = app.fallbackToOlderReleases,
                advancedFilterError = null,
                advancedPreviewLoading = true,
                advancedPreviewMatched = persistentListOf(),
                advancedPreviewTag = null,
                advancedPreviewMessage = null,
                advancedSavingFilter = false,
            )
        }
        refreshAdvancedPreview()
    }

    private fun onAdvancedFilterChanged(value: String) {
        val parseResult = AssetFilter.parse(value)
        val errorKey = parseResult?.exceptionOrNull()?.let { "invalid" }
        _state.update {
            it.copy(
                advancedFilterDraft = value,
                advancedFilterError = errorKey,
            )
        }
        if (errorKey == null) schedulePreviewRefresh()
    }

    /**
     * Debounces preview refresh while the user is typing. We don't want to
     * issue a fresh GitHub releases call on every keystroke — 350ms after
     * input stops is plenty responsive without burning rate limit.
     */
    private fun schedulePreviewRefresh() {
        advancedPreviewJob?.cancel()
        advancedPreviewJob =
            viewModelScope.launch {
                delay(350)
                refreshAdvancedPreview()
            }
    }

    private fun refreshAdvancedPreview() {
        val app = _state.value.advancedSettingsApp ?: return
        val draftFilter = _state.value.advancedFilterDraft
        val draftFallback = _state.value.advancedFallbackDraft

        // Validate locally before hitting the network — invalid regex
        // shows the error inline and aborts the preview.
        val parseResult = AssetFilter.parse(draftFilter)
        if (parseResult != null && parseResult.isFailure) {
            _state.update {
                it.copy(
                    advancedPreviewLoading = false,
                    advancedPreviewMatched = persistentListOf(),
                    advancedPreviewTag = null,
                    advancedPreviewMessage = null,
                    advancedFilterError = "invalid",
                )
            }
            return
        }

        advancedPreviewJob?.cancel()
        advancedPreviewJob =
            viewModelScope.launch {
                _state.update { it.copy(advancedPreviewLoading = true) }
                try {
                    val preview =
                        installedAppsRepository.previewMatchingAssets(
                            owner = app.repoOwner,
                            repo = app.repoName,
                            regex = draftFilter.takeIf { it.isNotBlank() },
                            includePreReleases = app.includePreReleases,
                            fallbackToOlderReleases = draftFallback,
                        )
                    _state.update {
                        it.copy(
                            advancedPreviewLoading = false,
                            advancedPreviewMatched =
                                preview.matchedAssets
                                    .map { asset -> asset.toUi() }
                                    .toImmutableList(),
                            advancedPreviewTag = preview.release?.tagName,
                            advancedPreviewMessage =
                                if (preview.matchedAssets.isEmpty() && preview.regexError == null) {
                                    "no_match"
                                } else {
                                    null
                                },
                            advancedFilterError =
                                if (preview.regexError != null) "invalid" else null,
                        )
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Failed to preview matching assets: ${e.message}")
                    _state.update {
                        it.copy(
                            advancedPreviewLoading = false,
                            advancedPreviewMatched = persistentListOf(),
                            advancedPreviewTag = null,
                            advancedPreviewMessage = "preview_failed",
                        )
                    }
                }
            }
    }

    /**
     * Opens the variant picker for [app]. Fetches the current latest
     * matching release (honouring the per-app filter / fallback) so the
     * dialog can show real, current asset names — not the cached ones
     * which might be stale or wrong. When [resumeUpdateAfterPick] is
     * true, dispatch the update flow as soon as the user picks.
     */
    private fun openVariantPicker(
        app: InstalledAppUi,
        resumeUpdateAfterPick: Boolean,
    ) {
        _state.update {
            it.copy(
                variantPickerApp = app,
                variantPickerLoading = true,
                variantPickerOptions = persistentListOf(),
                variantPickerCurrentVariant = app.preferredAssetVariant,
                variantPickerError = null,
                variantPickerResumeUpdateAfterPick = resumeUpdateAfterPick,
            )
        }
        viewModelScope.launch {
            try {
                val preview =
                    installedAppsRepository.previewMatchingAssets(
                        owner = app.repoOwner,
                        repo = app.repoName,
                        regex = app.assetFilterRegex,
                        includePreReleases = app.includePreReleases,
                        fallbackToOlderReleases = app.fallbackToOlderReleases,
                    )
                // Only assets whose filename has an extractable, non-empty
                // variant tag are pinnable: an empty extract or null means
                // we'd have nothing to remember release-over-release. The
                // dialog filters its own list so users can't tap a row
                // that would silently no-op.
                val pinnableAssets =
                    preview.matchedAssets.filter { asset ->
                        AssetVariant.extract(asset.name)?.isNotEmpty() == true
                    }
                _state.update {
                    it.copy(
                        variantPickerLoading = false,
                        variantPickerOptions =
                            pinnableAssets
                                .map { asset -> asset.toUi() }
                                .toImmutableList(),
                        variantPickerError =
                            when {
                                preview.matchedAssets.isEmpty() -> "no_assets"
                                pinnableAssets.isEmpty() -> "no_pinnable_variants"
                                else -> null
                            },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to load variant picker for ${app.packageName}: ${e.message}")
                _state.update {
                    it.copy(
                        variantPickerLoading = false,
                        variantPickerError = "load_failed",
                    )
                }
            }
        }
    }

    /**
     * Persists the user's variant pick (or null to reset to auto),
     * dismisses the dialog, and — if the picker was opened from a "tap
     * Update on stale variant" flow — kicks the update off automatically
     * with the freshly-resolved cached fields.
     */
    private fun saveVariantSelection(variant: String?) {
        val app = _state.value.variantPickerApp ?: return
        val resume = _state.value.variantPickerResumeUpdateAfterPick

        viewModelScope.launch {
            try {
                installedAppsRepository.setPreferredVariant(
                    packageName = app.packageName,
                    variant = variant,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to save preferred variant for ${app.packageName}: ${e.message}")
                _state.update { it.copy(variantPickerError = "save_failed") }
                return@launch
            }

            // Dismiss the dialog regardless of whether we resume.
            _state.update {
                it.copy(
                    variantPickerApp = null,
                    variantPickerOptions = persistentListOf(),
                    variantPickerCurrentVariant = null,
                    variantPickerError = null,
                    variantPickerLoading = false,
                    variantPickerResumeUpdateAfterPick = false,
                )
            }

            if (resume) {
                // Read the canonical InstalledApp directly from the
                // repository rather than the in-memory state. The Flow
                // that drives `_state.value.apps` propagates DAO writes
                // asynchronously, so reading state right after
                // setPreferredVariant — which itself runs an internal
                // checkForUpdates write — can race and hand us the OLD
                // pre-pick row, leading to an update with the wrong
                // asset URL. A direct DAO read is synchronous and never
                // races against pending Flow emissions.
                val refreshed =
                    runCatching { installedAppsRepository.getAppByPackage(app.packageName) }
                        .getOrNull()
                        ?.toUi()
                if (refreshed != null) {
                    updateSingleApp(refreshed)
                }
            }
        }
    }

    private fun saveAdvancedSettings() {
        val app = _state.value.advancedSettingsApp ?: return
        val draftFilter = _state.value.advancedFilterDraft.trim()
        val draftFallback = _state.value.advancedFallbackDraft

        // Final regex validation — if it's broken we refuse to save.
        val parseResult = AssetFilter.parse(draftFilter)
        if (parseResult != null && parseResult.isFailure) {
            _state.update { it.copy(advancedFilterError = "invalid") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(advancedSavingFilter = true) }
            try {
                // `setAssetFilter` persists and then re-checks internally,
                // so the UI badge is refreshed without a second round-trip.
                installedAppsRepository.setAssetFilter(
                    packageName = app.packageName,
                    regex = draftFilter.takeIf { it.isNotEmpty() },
                    fallbackToOlderReleases = draftFallback,
                )
                _state.update {
                    it.copy(
                        advancedSettingsApp = null,
                        advancedFilterDraft = "",
                        advancedFallbackDraft = false,
                        advancedFilterError = null,
                        advancedPreviewLoading = false,
                        advancedPreviewMatched = persistentListOf(),
                        advancedPreviewTag = null,
                        advancedPreviewMessage = null,
                        advancedSavingFilter = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Failed to save advanced settings: ${e.message}")
                _state.update {
                    it.copy(
                        advancedSavingFilter = false,
                        advancedPreviewMessage = "save_failed",
                    )
                }
            }
        }
    }

    private fun uninstallApp(app: InstalledAppUi) {
        viewModelScope.launch {
            try {
                installer.uninstall(app.packageName)
                logger.debug("Requested uninstall for ${app.packageName}")
            } catch (e: Exception) {
                logger.error("Failed to request uninstall for ${app.packageName}: ${e.message}")
                _events.send(
                    AppsEvent.ShowError(
                        getString(Res.string.failed_to_uninstall, app.appName),
                    ),
                )
            }
        }
    }

    private fun openApp(app: InstalledAppUi) {
        viewModelScope.launch {
            try {
                appsRepository.openApp(
                    installedApp = app.toDomain(),
                    onCantLaunchApp = {
                        viewModelScope.launch {
                            _events.send(
                                AppsEvent.ShowError(
                                    getString(
                                        Res.string.cannot_launch,
                                        app.appName,
                                    ),
                                ),
                            )
                        }
                    },
                )
            } catch (e: Exception) {
                logger.error("Failed to open app: ${e.message}")
                _events.send(
                    AppsEvent.ShowError(
                        getString(
                            Res.string.failed_to_open,
                            app.appName,
                        ),
                    ),
                )
            }
        }
    }

    private fun updateSingleApp(app: InstalledAppUi) {
        if (activeUpdates.containsKey(app.packageName)) {
            logger.debug("Update already in progress for ${app.packageName}")
            return
        }

        val job =
            viewModelScope.launch {
                try {
                    updateAppState(app.packageName, UpdateState.CheckingUpdate)

                    val latestRelease =
                        try {
                            appsRepository.getLatestRelease(
                                owner = app.repoOwner,
                                repo = app.repoName,
                                includePreReleases = app.includePreReleases,
                            )
                        } catch (e: Exception) {
                            logger.error("Failed to fetch latest release: ${e.message}")
                            throw IllegalStateException("Failed to fetch latest release: ${e.message}")
                        }

                    if (latestRelease == null) {
                        throw IllegalStateException("No release found for ${app.appName}")
                    }

                    val installableAssets =
                        latestRelease.assets.filter { asset ->
                            installer.isAssetInstallable(asset.name)
                        }

                    if (installableAssets.isEmpty()) {
                        throw IllegalStateException("No installable assets found for this platform")
                    }

                    // Honour the user's pinned variant first; fall back to
                    // the platform installer's auto-pick if the variant
                    // isn't present in this release. The auto-pick
                    // intentionally never throws here — checkForUpdates
                    // already flipped `preferredVariantStale=true` and the
                    // earlier intercept (see updateSingleApp entrypoint)
                    // would have routed us to the picker dialog instead.
                    val variantMatch =
                        AssetVariant.resolvePreferredAsset(
                            assets = installableAssets,
                            pinnedVariant = app.preferredAssetVariant,
                            pinnedTokens = AssetVariant.deserializeTokens(app.preferredAssetTokens),
                            pinnedGlob = app.assetGlobPattern,
                        )
                    val primaryAsset =
                        variantMatch
                            ?: installer.choosePrimaryAsset(installableAssets)
                            ?: throw IllegalStateException("Could not determine primary asset")

                    logger.debug(
                        "Update: ${app.appName} from ${app.installedVersion} to ${latestRelease.tagName}, " +
                            "asset: ${primaryAsset.name}",
                    )

                    val latestAssetUrl = primaryAsset.downloadUrl
                    val latestAssetName = primaryAsset.name
                    val latestVersion = latestRelease.tagName

                    val ext = latestAssetName.substringAfterLast('.', "").lowercase()
                    installer.ensurePermissionsOrThrow(ext)

                    val existingPath = downloader.getDownloadedFilePath(latestAssetName)
                    if (existingPath != null) {
                        val file = File(existingPath)
                        try {
                            val apkInfo =
                                installer.getApkInfoExtractor().extractPackageInfo(existingPath)
                            val normalizedExisting =
                                apkInfo?.versionName?.removePrefix("v")?.removePrefix("V") ?: ""
                            val normalizedLatest =
                                latestVersion.removePrefix("v").removePrefix("V")
                            if (normalizedExisting != normalizedLatest) {
                                val deleted = file.delete()
                                logger.debug("Deleted mismatched existing file ($normalizedExisting != $normalizedLatest): $deleted")
                            }
                        } catch (e: Exception) {
                            logger.debug("Failed to extract APK info for existing file: ${e.message}")
                            val deleted = file.delete()
                            logger.debug("Deleted unextractable existing file: $deleted")
                        }
                    }

                    updateAppState(app.packageName, UpdateState.Downloading)

                    // Route the download through the orchestrator so
                    // it survives this VM being torn down (user
                    // navigating away from the apps tab). Shizuku
                    // gets AlwaysInstall (silent install regardless
                    // of foreground state); regular installer gets
                    // InstallWhileForeground so the existing dialog/
                    // installer dispatch below stays in charge.
                    val installerType =
                        try {
                            tweaksRepository.getInstallerType().first()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            InstallerType.DEFAULT
                        }
                    val policy =
                        when (installerType) {
                            InstallerType.SHIZUKU -> InstallPolicy.AlwaysInstall
                            InstallerType.DHIZUKU -> InstallPolicy.AlwaysInstall
                            InstallerType.ROOT -> InstallPolicy.AlwaysInstall
                            InstallerType.DEFAULT -> InstallPolicy.InstallWhileForeground
                        }

                    downloadOrchestrator.enqueue(
                        DownloadSpec(
                            packageName = app.packageName,
                            repoOwner = app.repoOwner,
                            repoName = app.repoName,
                            asset = primaryAsset,
                            displayAppName = app.appName,
                            installPolicy = policy,
                            releaseTag = latestRelease.tagName,
                        ),
                    )

                    val orchestratorResult =
                        waitForOrchestratorReady(app.packageName) { progress ->
                            updateAppProgress(app.packageName, progress)
                        }
                    val filePath = when (orchestratorResult) {
                        is OrchestratorResult.Ready -> orchestratorResult.filePath
                        is OrchestratorResult.AlreadyInstalled -> {
                            updateAppState(app.packageName, UpdateState.Idle)
                            return@launch
                        }
                        is OrchestratorResult.Cancelled -> {
                            updateAppState(app.packageName, UpdateState.Idle)
                            return@launch
                        }
                        is OrchestratorResult.Failed -> {
                            updateAppState(
                                app.packageName,
                                UpdateState.Error(orchestratorResult.message ?: "Download failed"),
                            )
                            return@launch
                        }
                    }

                    val apkInfo =
                        installer.getApkInfoExtractor().extractPackageInfo(filePath)

                    val currentApp = installedAppsRepository.getAppByPackage(app.packageName)
                    if (currentApp != null) {
                        installedAppsRepository.updateApp(
                            currentApp.copy(
                                isPendingInstall = true,
                                latestVersion = latestVersion,
                                latestAssetName = latestAssetName,
                                latestAssetUrl = latestAssetUrl,
                                latestVersionName = apkInfo?.versionName ?: latestVersion,
                                latestVersionCode = apkInfo?.versionCode ?: 0L,
                            ),
                        )
                    } else {
                        markPendingUpdate(app.toDomain())
                    }

                    updateAppState(app.packageName, UpdateState.Installing)

                    try {
                        systemInstallSerializer.awaitFreeAndMarkPending(app.packageName)
                        installer.install(filePath, ext)
                    } catch (e: Exception) {
                        systemInstallSerializer.markCompleted(app.packageName)
                        installedAppsRepository.updatePendingStatus(app.packageName, false)
                        throw e
                    }

                    // Successful install — release the orchestrator
                    // entry so the apps row stops showing the
                    // download/install state. The DB sync continues
                    // via PackageEventReceiver.
                    downloadOrchestrator.dismiss(app.packageName)
                    try {
                        installedAppsRepository.setPendingInstallFilePath(app.packageName, null)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (clearEx: Exception) {
                        logger.error("Failed to clear pending install file path: ${clearEx.message}")
                    }

                    updateAppState(app.packageName, UpdateState.Idle)

                    logger.debug("Launched installer for ${app.appName} $latestVersion, waiting for system confirmation")
                } catch (e: CancellationException) {
                    logger.debug("Update cancelled for ${app.packageName}")
                    cleanupUpdate(app.packageName, app.latestAssetName)
                    try {
                        installedAppsRepository.updatePendingStatus(app.packageName, false)
                    } catch (clearEx: Exception) {
                        logger.error("Failed to clear pending status on cancellation: ${clearEx.message}")
                    }
                    updateAppState(app.packageName, UpdateState.Idle)
                    throw e
                } catch (_: RateLimitException) {
                    logger.debug("Rate limited during update for ${app.packageName}")
                    try {
                        installedAppsRepository.updatePendingStatus(app.packageName, false)
                    } catch (clearEx: Exception) {
                        logger.error("Failed to clear pending status on rate limit: ${clearEx.message}")
                    }
                    updateAppState(app.packageName, UpdateState.Idle)
                    _events.send(
                        AppsEvent.ShowError(getString(Res.string.rate_limit_exceeded)),
                    )
                } catch (e: Exception) {
                    logger.error("Update failed for ${app.packageName}: ${e.message}")
                    cleanupUpdate(app.packageName, app.latestAssetName)
                    try {
                        installedAppsRepository.updatePendingStatus(app.packageName, false)
                    } catch (clearEx: Exception) {
                        logger.error("Failed to clear pending status on error: ${clearEx.message}")
                    }
                    updateAppState(
                        app.packageName,
                        UpdateState.Error(e.message ?: "Update failed"),
                    )
                    _events.send(
                        AppsEvent.ShowError(
                            getString(
                                Res.string.failed_to_update,
                                app.appName,
                                e.message ?: "",
                            ),
                        ),
                    )
                } finally {
                    activeUpdates.remove(app.packageName)
                }
            }

        activeUpdates[app.packageName] = job
    }

    private fun updateAllApps() {
        if (_state.value.isUpdatingAll) {
            logger.error("Update all already in progress")
            return
        }

        updateAllJob =
            viewModelScope.launch {
                try {
                    _state.update { it.copy(isUpdatingAll = true) }

                    val appsToUpdate =
                        _state.value.apps.filter {
                            it.installedApp.isUpdateAvailable &&
                                it.updateState !is UpdateState.Success
                        }

                    if (appsToUpdate.isEmpty()) {
                        _events.send(AppsEvent.ShowError(getString(Res.string.no_updates_available)))
                        return@launch
                    }

                    logger.debug("Starting update all for ${appsToUpdate.size} apps")

                    appsToUpdate.forEachIndexed { index, appItem ->
                        if (!isActive) {
                            logger.debug("Update all cancelled")
                            return@launch
                        }

                        _state.update {
                            it.copy(
                                updateAllProgress =
                                    UpdateAllProgress(
                                        current = index + 1,
                                        total = appsToUpdate.size,
                                        currentAppName = appItem.installedApp.appName,
                                    ),
                            )
                        }

                        logger.debug("Updating ${index + 1}/${appsToUpdate.size}: ${appItem.installedApp.appName}")

                        updateSingleApp(appItem.installedApp)
                        activeUpdates[appItem.installedApp.packageName]?.join()

                        delay(1000)
                    }

                    logger.debug("Update all completed successfully")
                    _events.send(AppsEvent.ShowSuccess(getString(Res.string.all_apps_updated_successfully)))
                } catch (_: CancellationException) {
                    logger.debug("Update all cancelled")
                } catch (e: Exception) {
                    logger.error("Update all failed: ${e.message}")
                    _events.send(
                        AppsEvent.ShowError(
                            getString(
                                Res.string.update_all_failed,
                                arrayOf(e.message),
                            ),
                        ),
                    )
                } finally {
                    _state.update {
                        it.copy(
                            isUpdatingAll = false,
                            updateAllProgress = null,
                        )
                    }
                    updateAllJob = null
                }
            }
    }

    private fun cancelUpdate(packageName: String) {
        activeUpdates[packageName]?.cancel()
        activeUpdates.remove(packageName)

        val app = _state.value.apps.find { it.installedApp.packageName == packageName }
        app?.installedApp?.latestAssetName?.let { assetName ->
            viewModelScope.launch {
                cleanupUpdate(packageName, assetName)
            }
        }

        updateAppState(packageName, UpdateState.Idle)
    }

    private fun cancelAllUpdates() {
        updateAllJob?.cancel()
        updateAllJob = null

        activeUpdates.values.forEach { it.cancel() }
        activeUpdates.clear()

        viewModelScope.launch {
            _state.value.apps.forEach { appItem ->
                if (appItem.updateState != UpdateState.Idle &&
                    appItem.updateState != UpdateState.Success
                ) {
                    appItem.installedApp.latestAssetName?.let { assetName ->
                        cleanupUpdate(appItem.installedApp.packageName, assetName)
                    }
                    updateAppState(appItem.installedApp.packageName, UpdateState.Idle)
                }
            }
        }

        _state.update {
            it.copy(
                isUpdatingAll = false,
                updateAllProgress = null,
            )
        }
    }

    private fun updateAppState(
        packageName: String,
        state: UpdateState,
    ) {
        _state.update { currentState ->
            currentState.copy(
                apps =
                    currentState.apps
                        .map { appItem ->
                            if (appItem.installedApp.packageName == packageName) {
                                appItem.copy(
                                    updateState = state,
                                    downloadProgress =
                                        if (state is UpdateState.Downloading) {
                                            appItem.downloadProgress
                                        } else {
                                            null
                                        },
                                    error = if (state is UpdateState.Error) state.message else null,
                                )
                            } else {
                                appItem
                            }
                        }.toImmutableList(),
            )
        }

        filterApps()
    }

    private fun updateAppProgress(
        packageName: String,
        progress: Int?,
    ) {
        // Download progress is purely a per-row visual update; it doesn't
        // affect sort order or search match. Avoid re-running the full
        // filter+sort pass on every progress tick (which at 50+ apps with
        // a download in flight ran ~100×/sec). Map both lists in place.
        _state.update { currentState ->
            currentState.copy(
                apps =
                    currentState.apps
                        .map { appItem ->
                            if (appItem.installedApp.packageName == packageName) {
                                appItem.copy(downloadProgress = progress)
                            } else {
                                appItem
                            }
                        }.toImmutableList(),
                filteredApps =
                    currentState.filteredApps
                        .map { appItem ->
                            if (appItem.installedApp.packageName == packageName) {
                                appItem.copy(downloadProgress = progress)
                            } else {
                                appItem
                            }
                        }.toImmutableList(),
            )
        }
    }

    private suspend fun markPendingUpdate(app: InstalledApp) {
        installedAppsRepository.updatePendingStatus(app.packageName, true)
        logger.debug("Marked ${app.packageName} as pending install")
    }

    /**
     * Subscribes to the orchestrator's entry for [packageName] and
     * suspends until it reaches a terminal stage. Mirrors progress
     * via [onProgress] while downloading.
     *
     * Returns:
     *  - The file path when the orchestrator parks the file at
     *    [OrchestratorStage.AwaitingInstall] (regular installer path)
     *  - `null` when the orchestrator finishes its own install
     *    ([OrchestratorStage.Completed], the Shizuku/AlwaysInstall
     *    path) — the caller has nothing more to do
     *  - `null` when the entry is cancelled or fails — the caller
     *    treats this as "abort the local install logic"
     *
     * Implementation: forwards progress side-effects via a
     * `transform` step, then `first { predicate }` finds the first
     * emission whose stage is terminal. Avoids needing to throw out
     * of `collect`.
     */
    /**
     * Result type for [waitForOrchestratorReady] so callers can
     * distinguish "file is ready" from "orchestrator already installed"
     * from "download failed".
     */
    private sealed interface OrchestratorResult {
        data class Ready(val filePath: String) : OrchestratorResult
        data object AlreadyInstalled : OrchestratorResult
        data object Cancelled : OrchestratorResult
        data class Failed(val message: String?) : OrchestratorResult
    }

    private suspend fun waitForOrchestratorReady(
        packageName: String,
        onProgress: (Int) -> Unit,
    ): OrchestratorResult {
        val terminal =
            downloadOrchestrator
                .observe(packageName)
                .onEach { entry ->
                    if (entry != null) {
                        entry.progressPercent?.let(onProgress)
                    }
                }
                .first { entry ->
                    entry == null ||
                        entry.stage == OrchestratorStage.AwaitingInstall ||
                        entry.stage == OrchestratorStage.Completed ||
                        entry.stage == OrchestratorStage.Cancelled ||
                        entry.stage == OrchestratorStage.Failed
                }
        return when {
            terminal == null -> OrchestratorResult.Cancelled
            terminal.stage == OrchestratorStage.AwaitingInstall ->
                terminal.filePath?.let { OrchestratorResult.Ready(it) }
                    ?: OrchestratorResult.Failed("Downloaded file path missing")
            terminal.stage == OrchestratorStage.Completed -> OrchestratorResult.AlreadyInstalled
            terminal.stage == OrchestratorStage.Failed -> OrchestratorResult.Failed(terminal.errorMessage)
            else -> OrchestratorResult.Cancelled
        }
    }

    /**
     * Triggers an install for an app whose download was previously
     * deferred (the orchestrator parked the file in `AwaitingInstall`
     * mode after the user navigated away mid-download). Used by the
     * apps row "Install" button when [InstalledAppUi.pendingInstallFilePath]
     * is non-null.
     *
     * Delegates to [DownloadOrchestrator.installPending] which
     * handles validation-free install + DB cleanup.
     */
    private fun installPendingApp(app: InstalledAppUi) {
        if (activeUpdates.containsKey(app.packageName)) {
            logger.debug("Install already in progress for ${app.packageName}")
            return
        }
        viewModelScope.launch {
            try {
                updateAppState(app.packageName, UpdateState.Installing)
                downloadOrchestrator.installPending(app.packageName)
                updateAppState(app.packageName, UpdateState.Idle)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.error("Failed to install pending app ${app.packageName}: ${t.message}")
                updateAppState(
                    app.packageName,
                    UpdateState.Error(t.message ?: "Install failed"),
                )
            }
        }
    }

    /**
     * User decided to drop a pending install — they cancelled the
     * system installer prompt and don't want this app anymore. Cancels
     * the orchestrator entry (this also nukes the parked APK file via
     * the downloader's cancel path), removes the DB row, and clears
     * any leftover pending notification.
     */
    private fun discardPendingInstall(app: InstalledAppUi) {
        viewModelScope.launch {
            try {
                downloadOrchestrator.cancel(app.packageName)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                logger.warn("discardPendingInstall: orchestrator cancel failed: ${t.message}")
            }
            // Belt-and-braces: cancel doesn't nuke the file when the
            // entry was already in AwaitingInstall — the orchestrator
            // already cleared its in-memory metadata before the user
            // tapped Discard. Delete the parked file directly so the
            // bytes don't leak.
            app.pendingInstallFilePath?.let { path ->
                runCatching { File(path).takeIf { it.exists() }?.delete() }
                    .onFailure {
                        logger.warn(
                            "discardPendingInstall: failed to delete parked file: ${it.message}",
                        )
                    }
            }
            try {
                installedAppsRepository.deleteInstalledApp(app.packageName)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                // Mirror uninstallApp's UX: a row-delete failure leaves
                // the apps list pointing at metadata for an APK that's
                // already been removed from disk, so the user has to
                // know it didn't take. Log the cause and surface a
                // localized error event for the snackbar host.
                logger.error(
                    "discardPendingInstall: row delete failed for ${app.packageName}: ${t.message}",
                )
                _events.send(
                    AppsEvent.ShowError(
                        getString(
                            Res.string.failed_to_discard_pending,
                            app.appName,
                        ),
                    ),
                )
            }
        }
    }

    private suspend fun cleanupUpdate(
        packageName: String,
        assetName: String?,
    ) {
        try {
            if (assetName != null) {
                val deleted = downloader.cancelDownload(assetName)
                logger.debug("Cleanup for $packageName - file deleted: $deleted")
            }
        } catch (e: Exception) {
            logger.error("Cleanup failed for $packageName: ${e.message}")
        }
    }

    private var smartMatchJob: Job? = null

    private fun startSmartMatch(app: zed.rainxch.apps.presentation.model.DeviceAppUi) {
        smartMatchJob?.cancel()
        _state.update {
            it.copy(
                selectedDeviceApp = app,
                linkStep = LinkStep.SmartMatch,
                linkSearchLoading = true,
                linkSuggestions = persistentListOf(),
                linkSearchError = null,
                repoUrl = "",
                repoValidationError = null,
                fetchedRepoInfo = null,
            )
        }
        smartMatchJob = viewModelScope.launch {
            try {
                val candidate =
                    zed.rainxch.core.domain.system.ExternalAppCandidate(
                        packageName = app.packageName,
                        appLabel = app.appName,
                        versionName = app.versionName,
                        versionCode = app.versionCode,
                        signingFingerprint = app.signingFingerprint,
                        installerKind = zed.rainxch.core.domain.system.InstallerKind.UNKNOWN,
                        manifestHint = null,
                        firstSeenAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
                    )
                val results = externalImportRepository.resolveMatches(listOf(candidate))
                val suggestions =
                    results
                        .firstOrNull()
                        ?.suggestions
                        ?.sortedByDescending { it.confidence }
                        ?.take(8)
                        .orEmpty()
                        .toImmutableList()
                _state.update {
                    it.copy(
                        linkSearchLoading = false,
                        linkSuggestions = suggestions,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn("Smart match failed for ${app.packageName}: ${e.message}")
                _state.update {
                    it.copy(
                        linkSearchLoading = false,
                        linkSearchError = e.message ?: "search failed",
                    )
                }
            }
        }
    }

    private fun openLinkSheet() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    showLinkSheet = true,
                    linkStep = LinkStep.PickApp,
                    deviceApps = persistentListOf(),
                    deviceAppSearchQuery = "",
                    selectedDeviceApp = null,
                    repoUrl = "",
                    repoValidationError = null,
                    fetchedRepoInfo = null,
                    linkSuggestions = persistentListOf(),
                    linkSearchError = null,
                    linkSearchLoading = false,
                )
            }

            try {
                val trackedPackages = appsRepository.getTrackedPackageNames()
                val deviceApps =
                    appsRepository
                        .getDeviceApps()
                        .filter { it.packageName !in trackedPackages }
                        .map { it.toUi() }
                        .toImmutableList()

                _state.update { it.copy(deviceApps = deviceApps) }
            } catch (e: Exception) {
                logger.error("Failed to load device apps: ${e.message}")
                _events.send(AppsEvent.ShowError(getString(Res.string.failed_to_load_apps)))
            }
        }
    }

    private fun dismissLinkSheet() {
        smartMatchJob?.cancel()
        smartMatchJob = null
        _state.update {
            it.copy(
                showLinkSheet = false,
                linkStep = LinkStep.PickApp,
                deviceApps = persistentListOf(),
                deviceAppSearchQuery = "",
                selectedDeviceApp = null,
                repoUrl = "",
                repoValidationError = null,
                linkValidationStatus = null,
                linkInstallableAssets = persistentListOf(),
                linkSelectedAsset = null,
                linkDownloadProgress = null,
                fetchedRepoInfo = null,
                isValidatingRepo = false,
                linkAssetFilter = "",
                linkAssetFilterError = null,
                linkFallbackToOlder = false,
                linkSuggestions = persistentListOf(),
                linkSearchError = null,
                linkSearchLoading = false,
            )
        }
    }

    private fun onLinkAssetFilterChanged(value: String) {
        // Validate the regex on every keystroke so the user gets immediate
        // feedback. The state's filteredLinkAssets getter falls back to the
        // unfiltered list when the regex is invalid, so the picker stays
        // usable even mid-typing.
        val parseResult = AssetFilter.parse(value)
        val error =
            parseResult?.exceptionOrNull()?.let { _ ->
                // Localized message comes from the UI layer; here we just
                // signal that something is wrong.
                "invalid"
            }
        _state.update {
            it.copy(
                linkAssetFilter = value,
                linkAssetFilterError = error,
            )
        }
    }

    /**
     * Picks a sensible default for the link-flow filter. Tries, in order:
     *   1. The trailing segment of the package name (e.g. `io.ente.auth` → `auth`)
     *   2. A token derived from the device app's display name (e.g.
     *      `Ente Auth` → `auth`)
     *   3. [AssetFilter.suggestFromAssetName] on the first asset
     *
     * Every candidate is routed through [Regex.escape] before validation
     * so metacharacters in package names or display words (think
     * `My App (Beta)` → `(beta)`) are treated literally and never break
     * regex compilation.
     *
     * Returns the first non-blank candidate that actually matches at least
     * one of the available assets — otherwise null, which leaves the field
     * empty so we don't pre-fill something useless.
     */
    private fun suggestFilterForLink(
        deviceAppName: String,
        packageName: String,
        firstAssetName: String?,
    ): String? {
        val state = _state.value
        val assets = state.linkInstallableAssets

        fun tryCandidate(rawToken: String): String? {
            if (rawToken.length < 3) return null
            val escaped = Regex.escape(rawToken)
            val regex =
                runCatching { Regex(escaped, RegexOption.IGNORE_CASE) }.getOrNull()
                    ?: return null
            return if (assets.any { regex.containsMatchIn(it.name) }) escaped else null
        }

        // 1. Last package segment (commonly the most distinctive token).
        val packageTail = packageName.substringAfterLast('.').lowercase()
        tryCandidate(packageTail)?.let { return it }

        // 2. Significant words from the display name.
        deviceAppName
            .split(' ', '-', '_')
            .map { it.lowercase().trim() }
            .forEach { token ->
                tryCandidate(token)?.let { return it }
            }

        // 3. Heuristic on the first asset name (already escaped + anchored
        //    by AssetFilter.suggestFromAssetName).
        return firstAssetName?.let { AssetFilter.suggestFromAssetName(it) }
    }

    private fun validateAndLinkRepo() {
        val selectedApp = _state.value.selectedDeviceApp ?: return
        val url = _state.value.repoUrl.trim()

        viewModelScope.launch {
            val customHosts = runCatching {
                tweaksRepository.getCustomForgeHosts().first()
            }.getOrElse { emptySet() }
            val parsedRef = zed.rainxch.core.domain.util.RepositoryUrlParser.parse(url, customHosts)
            if (parsedRef == null) {
                _state.update { it.copy(repoValidationError = getString(Res.string.invalid_github_url)) }
                return@launch
            }

            val owner = parsedRef.owner
            val repo = parsedRef.repo
            val sourceHost: String? = when (val src = parsedRef.source) {
                zed.rainxch.core.domain.model.RepositorySource.GitHub -> null
                is zed.rainxch.core.domain.model.RepositorySource.Forgejo -> src.host
            }
            _state.update {
                it.copy(
                    isValidatingRepo = true,
                    repoValidationError = null,
                    linkValidationStatus = null,
                )
            }

            try {
                _state.update { it.copy(linkValidationStatus = getString(Res.string.validating_repo)) }

                val repoInfo = appsRepository.fetchRepoInfo(owner, repo, sourceHost)
                if (repoInfo == null) {
                    _state.update {
                        it.copy(
                            isValidatingRepo = false,
                            linkValidationStatus = null,
                            repoValidationError = getString(Res.string.repo_not_found, owner, repo),
                        )
                    }
                    return@launch
                }

                _state.update {
                    it.copy(
                        fetchedRepoInfo = repoInfo.toUi(),
                        linkSourceHost = sourceHost,
                        linkValidationStatus = getString(Res.string.checking_release),
                    )
                }

                val latestRelease =
                    try {
                        appsRepository.getLatestRelease(owner, repo, sourceHost = sourceHost)
                    } catch (e: RateLimitException) {
                        throw e
                    } catch (e: Exception) {
                        logger.debug("Could not fetch release for validation: ${e.message}")
                        return@launch
                    }

                if (latestRelease == null) {
                    appsRepository.linkAppToRepo(selectedApp.toDomain(), repoInfo, sourceHost = sourceHost)
                    _state.update {
                        it.copy(
                            isValidatingRepo = false,
                            linkValidationStatus = null,
                            showLinkSheet = false,
                        )
                    }
                    _events.send(AppsEvent.AppLinkedSuccessfully(selectedApp.appName))
                    _events.send(
                        AppsEvent.ShowSuccess(
                            getString(
                                Res.string.app_linked_success,
                                selectedApp.appName,
                                repoInfo.owner,
                                repoInfo.name,
                            ),
                        ),
                    )
                    return@launch
                }

                val installableAssets =
                    latestRelease
                        .assets
                        .filter { installer.isAssetInstallable(it.name) }
                        .map { it.toUi() }
                        .toImmutableList()
                if (installableAssets.isEmpty()) {
                    appsRepository.linkAppToRepo(selectedApp.toDomain(), repoInfo, sourceHost = sourceHost)
                    _state.update {
                        it.copy(
                            isValidatingRepo = false,
                            linkValidationStatus = null,
                            showLinkSheet = false,
                        )
                    }
                    _events.send(AppsEvent.AppLinkedSuccessfully(selectedApp.appName))
                    _events.send(
                        AppsEvent.ShowSuccess(
                            getString(
                                Res.string.app_linked_success,
                                selectedApp.appName,
                                repoInfo.owner,
                                repoInfo.name,
                            ),
                        ),
                    )
                    return@launch
                }

                // Seed an auto-suggestion based on the device app's package
                // name first, then fall back to the first installable asset.
                // This makes monorepo linking nearly zero-effort: pick "Ente
                // Auth" → the filter pre-fills with "auth" so the picker
                // already shows just the relevant APKs.
                val suggestedFilter =
                    suggestFilterForLink(
                        deviceAppName = selectedApp.appName,
                        packageName = selectedApp.packageName,
                        firstAssetName = installableAssets.firstOrNull()?.name,
                    )

                _state.update {
                    it.copy(
                        isValidatingRepo = false,
                        linkValidationStatus = null,
                        linkStep = LinkStep.PickAsset,
                        linkInstallableAssets = installableAssets,
                        linkAssetFilter = suggestedFilter.orEmpty(),
                        linkAssetFilterError = null,
                        linkFallbackToOlder = false,
                    )
                }
            } catch (_: RateLimitException) {
                _state.update {
                    it.copy(
                        isValidatingRepo = false,
                        linkValidationStatus = null,
                        repoValidationError = getString(Res.string.rate_limit_try_again),
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to link app: ${e.message}")
                _state.update {
                    it.copy(
                        isValidatingRepo = false,
                        linkValidationStatus = null,
                        repoValidationError = getString(Res.string.failed_to_link, e.message ?: ""),
                    )
                }
            }
        }
    }

    private fun validateWithAsset(asset: GithubAssetUi) {
        val selectedApp = _state.value.selectedDeviceApp ?: return
        val repoInfo = _state.value.fetchedRepoInfo ?: return
        val siblingCount = _state.value.linkInstallableAssets.size
        val pickedIndex =
            _state.value.linkInstallableAssets
                .indexOfFirst { it.id == asset.id }
                .takeIf { it >= 0 }
        val assetFilterRegex = _state.value.linkAssetFilter.takeIf { it.isNotBlank() }
        val fallbackToOlder = _state.value.linkFallbackToOlder

        // The user explicitly picked this asset → trust the choice.
        // Skip the re-download + signing-key verification dance (which
        // burned bandwidth and time for nothing the user couldn't already
        // confirm). The `installSource = MANUAL` field linkAppToRepo
        // writes is what the Details banner reads to flag manual links.
        viewModelScope.launch {
            _state.update {
                it.copy(
                    linkSelectedAsset = asset,
                    linkDownloadProgress = null,
                    linkValidationStatus = null,
                    repoValidationError = null,
                )
            }
            try {
                appsRepository.linkAppToRepo(
                    deviceApp = selectedApp.toDomain(),
                    repoInfo = repoInfo.toDomain(),
                    assetFilterRegex = assetFilterRegex,
                    fallbackToOlderReleases = fallbackToOlder,
                    pickedAssetName = asset.name,
                    pickedAssetSiblingCount = siblingCount,
                    pickedAssetIndex = pickedIndex,
                    sourceHost = _state.value.linkSourceHost,
                )
                _state.update {
                    it.copy(
                        linkDownloadProgress = null,
                        linkValidationStatus = null,
                        showLinkSheet = false,
                    )
                }
                _events.send(AppsEvent.AppLinkedSuccessfully(selectedApp.appName))
                _events.send(
                    AppsEvent.ShowSuccess(
                        getString(
                            Res.string.app_linked_success,
                            selectedApp.appName,
                            repoInfo.owner,
                            repoInfo.name,
                        ),
                    ),
                )
            } catch (e: Exception) {
                logger.error("Failed to link app: ${e.message}")
                _state.update {
                    it.copy(
                        linkDownloadProgress = null,
                        linkValidationStatus = null,
                        repoValidationError = getString(Res.string.failed_to_link, e.message ?: ""),
                    )
                }
            }
        }
    }

    private fun parseGithubUrl(input: String): Pair<String, String>? {
        val normalized =
            input
                .trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .substringBefore("?")
                .substringBefore("#")
                .removeSuffix("/")

        val parts = normalized.split("/")
        if (parts.size < 3) return null
        if (!parts[0].equals("github.com", ignoreCase = true)) return null

        val owner = parts[1]
        val repo = parts[2]

        if (owner.isBlank() || repo.isBlank()) return null
        if (owner.length > 39 || repo.length > 100) return null

        return owner to repo
    }

    private fun exportApps() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            try {
                val json = appsRepository.exportApps()
                val fileName = "github-store-apps-${System.currentTimeMillis()}.json"
                shareManager.shareFile(fileName, json)
            } catch (e: Exception) {
                logger.error("Export failed: ${e.message}")
                _events.send(
                    AppsEvent.ShowError(
                        getString(
                            Res.string.export_failed,
                            e.message ?: "",
                        ),
                    ),
                )
            } finally {
                _state.update { it.copy(isExporting = false) }
            }
        }
    }

    private fun importAppsFromFile() {
        shareManager.pickFile("application/json") { content ->
            if (content != null) {
                viewModelScope.launch {
                    importApps(content)
                }
            }
        }
    }

    private suspend fun importApps(json: String) {
        _state.update { it.copy(isImporting = true) }
        try {
            val result = appsRepository.importApps(json)
            _state.update { it.copy(importSummary = result) }
            _events.send(AppsEvent.ImportComplete(result))
        } catch (e: Exception) {
            logger.error("Import failed: ${e.message}")
            _events.send(AppsEvent.ShowError(getString(Res.string.import_failed, e.message ?: "")))
        } finally {
            _state.update { it.copy(isImporting = false) }
        }
    }

    private fun exportObtainium() {
        viewModelScope.launch {
            _state.update { it.copy(isExporting = true) }
            try {
                val json = appsRepository.exportObtainium()
                val fileName = "github-store-obtainium-${System.currentTimeMillis()}.json"
                shareManager.shareFile(fileName, json)
            } catch (e: Exception) {
                logger.error("Obtainium export failed: ${e.message}")
                _events.send(
                    AppsEvent.ShowError(
                        getString(Res.string.export_failed, e.message ?: ""),
                    ),
                )
            } finally {
                _state.update { it.copy(isExporting = false) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        // Cancel local OBSERVERS only — the orchestrator entries
        // keep running in the application scope. Each in-flight
        // download is downgraded to DeferUntilUserAction so the
        // user gets a notification when it's ready, instead of
        // losing the work.
        updateAllJob?.cancel()
        val packageNames = activeUpdates.keys.toList()
        activeUpdates.values.forEach { it.cancel() }

        viewModelScope.launch(kotlinx.coroutines.NonCancellable) {
            for (packageName in packageNames) {
                try {
                    downloadOrchestrator.downgradeToDeferred(packageName)
                } catch (t: Throwable) {
                    logger.error("Failed to downgrade orchestrator for $packageName: ${t.message}")
                }
            }
        }
    }
}
