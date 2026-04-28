package zed.rainxch.apps.presentation.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import zed.rainxch.apps.domain.repository.AppsRepository
import zed.rainxch.apps.presentation.import.model.CandidateUi
import zed.rainxch.apps.presentation.import.model.ImportPhase
import zed.rainxch.apps.presentation.import.model.RepoSuggestionUi
import zed.rainxch.apps.presentation.import.model.SuggestionSource
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.DeviceApp
import zed.rainxch.core.domain.repository.ExternalImportRepository
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.TelemetryRepository
import zed.rainxch.core.domain.system.ExternalAppCandidate
import zed.rainxch.core.domain.system.ExternalDecisionSnapshot
import zed.rainxch.core.domain.system.InstallerKind
import zed.rainxch.core.domain.system.RepoMatchResult
import zed.rainxch.core.domain.system.RepoMatchSource
import zed.rainxch.core.domain.system.RepoMatchSuggestion
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.external_import_error_link_failed
import zed.rainxch.githubstore.core.presentation.res.external_import_error_link_network
import zed.rainxch.githubstore.core.presentation.res.external_import_error_scan_failed_default
import zed.rainxch.githubstore.core.presentation.res.external_import_installer_browser
import zed.rainxch.githubstore.core.presentation.res.external_import_installer_fdroid
import zed.rainxch.githubstore.core.presentation.res.external_import_installer_obtainium
import zed.rainxch.githubstore.core.presentation.res.external_import_installer_self
import zed.rainxch.githubstore.core.presentation.res.external_import_installer_sideload
import zed.rainxch.githubstore.core.presentation.res.external_import_installer_unknown
import zed.rainxch.githubstore.core.presentation.res.external_import_search_error_default
import zed.rainxch.githubstore.core.presentation.res.external_import_undo_failed
import zed.rainxch.githubstore.core.presentation.res.external_import_undo_linked
import zed.rainxch.githubstore.core.presentation.res.external_import_undo_skipped

class ExternalImportViewModel(
    private val externalImportRepository: ExternalImportRepository,
    private val appsRepository: AppsRepository,
    private val installedAppsRepository: InstalledAppsRepository,
    private val telemetry: TelemetryRepository,
    private val logger: GitHubStoreLogger,
) : ViewModel() {
    private var candidatesByPackage: Map<String, ExternalAppCandidate> = emptyMap()
    // Cached so OnAutoSummaryUndoAll can re-build cards for previously
    // auto-linked packages without round-tripping resolveMatches() (which
    // would issue another network call and could return different matches).
    private var lastResolvedMatches: List<RepoMatchResult> = emptyList()
    // Mirror of autoLinkedPackages: per-package pre-link snapshot of whether an
    // installed_apps row already existed. Bulk undo consults this to avoid
    // wiping rows that pre-existed (e.g., the user had previously linked the
    // app through some other path before auto-link added an entry to it).
    private var autoLinkedHadInstalledRow: Map<String, Boolean> = emptyMap()
    // Per-package external_links snapshot captured BEFORE auto-link writes the
    // MATCHED row. Bulk undo restores from these so the DAO actually rolls back
    // to the pre-link state (typically PENDING_REVIEW). Snapshotting AFTER the
    // link would just re-apply the linked state — a silent no-op.
    private var autoLinkedPreSnapshots: Map<String, ExternalDecisionSnapshot?> = emptyMap()
    private var hasStarted = false
    private var scanJob: Job? = null
    private var searchJob: Job? = null
    private var pendingUndo: PendingUndo? = null

    private val _state = MutableStateFlow(ExternalImportState())
    val state =
        _state
            .onStart {
                if (!hasStarted) {
                    hasStarted = true
                    startScanIfIdle()
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = ExternalImportState(),
            )

    private val _events = Channel<ExternalImportEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: ExternalImportAction) {
        when (action) {
            ExternalImportAction.OnStart -> {
                if (_state.value.phase == ImportPhase.Idle) startScanIfIdle()
            }

            ExternalImportAction.OnRequestPermission -> {
                _state.update { it.copy(phase = ImportPhase.RequestingPermission) }
                viewModelScope.launch { runCatching { telemetry.importPermissionRequested() } }
            }

            is ExternalImportAction.OnPermissionGranted -> {
                _state.update { it.copy(isPermissionDenied = false) }
                emitPermissionOutcome(granted = true, sdkInt = action.sdkInt)
                startScanIfIdle(force = true)
            }

            is ExternalImportAction.OnPermissionDenied -> {
                _state.update { it.copy(isPermissionDenied = true) }
                emitPermissionOutcome(granted = false, sdkInt = action.sdkInt)
                startScanIfIdle(force = true)
            }

            is ExternalImportAction.OnSkipCard -> skipPackage(action.packageName, neverAsk = false)

            is ExternalImportAction.OnSkipForever -> skipPackage(action.packageName, neverAsk = true)

            ExternalImportAction.OnSkipRemaining -> skipRemaining()

            is ExternalImportAction.OnPickSuggestion ->
                pickSuggestion(action.packageName, action.suggestion)

            is ExternalImportAction.OnLinkCard -> linkCardWithPreselected(action.packageName)

            is ExternalImportAction.OnToggleCardExpanded -> toggleCardExpanded(action.packageName)

            is ExternalImportAction.OnSearchOverrideChanged -> {
                // Explicit submit only — we never auto-fire on keystrokes.
                _state.update {
                    if (it.activeSearchPackage != action.packageName) {
                        // Switched cards: drop stale results from the previous card.
                        it.copy(
                            activeSearchPackage = action.packageName,
                            searchQuery = action.query,
                            searchResults = persistentListOf(),
                            isSearching = false,
                            searchError = null,
                        )
                    } else {
                        it.copy(searchQuery = action.query)
                    }
                }
            }

            is ExternalImportAction.OnSearchOverrideSubmit -> submitSearchOverride(action.packageName)

            ExternalImportAction.OnUndoLast -> undoLast()

            ExternalImportAction.OnExit -> {
                viewModelScope.launch {
                    _events.send(ExternalImportEvent.NavigateBack)
                }
            }

            ExternalImportAction.OnDismissCompletionToast -> {
                _state.update { it.copy(showCompletionToast = false) }
            }

            ExternalImportAction.OnAutoSummaryContinue -> autoSummaryContinue()

            ExternalImportAction.OnAutoSummaryUndoAll -> autoSummaryUndoAll()

            ExternalImportAction.OnAddManually -> {
                viewModelScope.launch {
                    _events.send(ExternalImportEvent.NavigateBackAndOpenManualLink)
                }
            }
        }
    }

    private fun toggleCardExpanded(packageName: String) {
        _state.update { current ->
            val nextSet =
                if (packageName in current.expandedPackages) {
                    current.expandedPackages.toPersistentSet().remove(packageName)
                } else {
                    current.expandedPackages.toPersistentSet().add(packageName)
                }
            // Clear cross-card search results when collapsing the active card so
            // they don't bleed into the next card the user expands.
            val keepSearch = current.activeSearchPackage == packageName && packageName in nextSet
            current.copy(
                expandedPackages = nextSet,
                activeSearchPackage = if (keepSearch) current.activeSearchPackage else null,
                searchQuery = if (keepSearch) current.searchQuery else "",
                searchResults = if (keepSearch) current.searchResults else persistentListOf(),
                isSearching = if (keepSearch) current.isSearching else false,
                searchError = if (keepSearch) current.searchError else null,
            )
        }
    }

    private fun startScanIfIdle(force: Boolean = false) {
        if (!force && _state.value.phase != ImportPhase.Idle) return
        if (scanJob?.isActive == true) return
        scanJob = viewModelScope.launch {
            try {
                _state.update { it.copy(phase = ImportPhase.Scanning, errorMessage = null) }

                externalImportRepository.runFullScan()

                val candidates = externalImportRepository.pendingCandidatesFlow().first()
                candidatesByPackage = candidates.associateBy { it.packageName }

                _state.update {
                    it.copy(
                        phase = ImportPhase.AutoImporting,
                        totalCandidates = candidates.size,
                    )
                }

                val matches = externalImportRepository.resolveMatches(candidates)
                lastResolvedMatches = matches
                val autoLinked = autoMaterialize(matches)
                val autoLinkedPackages = autoLinked.toSet()

                val reviewCandidates =
                    candidates.filter { it.packageName !in autoLinkedPackages }
                val reviewMatchesByPkg =
                    matches.associateBy { it.packageName }

                val cards =
                    reviewCandidates
                        .mapNotNull { candidate ->
                            val match = reviewMatchesByPkg[candidate.packageName]
                            buildCard(candidate, match)
                        }.toImmutableList()

                if (autoLinked.isNotEmpty()) {
                    // Stop on the summary screen so the user sees what auto-linked
                    // and can undo before we cascade into the review wizard.
                    val autoLinkedLabels = autoLinked.mapNotNull { pkg ->
                        candidatesByPackage[pkg]?.appLabel
                    }
                    _state.update {
                        it.copy(
                            phase = ImportPhase.AutoImportSummary,
                            cards = cards,
                            autoImported = autoLinked.size,
                            autoLinkedPackages = autoLinked.toPersistentList(),
                            autoLinkedLabels = autoLinkedLabels.toPersistentList(),
                        )
                    }
                } else if (cards.isEmpty()) {
                    _state.update {
                        it.copy(
                            phase = ImportPhase.Done,
                            cards = persistentListOf(),
                            autoImported = 0,
                            showCompletionToast = true,
                        )
                    }
                    _events.send(ExternalImportEvent.PlayConfetti)
                } else {
                    _state.update {
                        it.copy(
                            phase = ImportPhase.AwaitingReview,
                            cards = cards,
                            autoImported = 0,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("External import scan failed: ${e.message}")
                _state.update {
                    it.copy(
                        phase = ImportPhase.Idle,
                        errorMessage = e.message,
                    )
                }
                _events.send(
                    ExternalImportEvent.ShowError(
                        e.message ?: getString(Res.string.external_import_error_scan_failed_default),
                    ),
                )
            }
        }
    }

    private suspend fun buildCard(
        candidate: ExternalAppCandidate,
        match: RepoMatchResult?,
    ): CandidateUi? {
        val suggestionsDomain = match?.suggestions.orEmpty()
        val top = suggestionsDomain.maxByOrNull { it.confidence }
        val preselected =
            if (top != null && top.confidence in PRESELECT_MIN..PRESELECT_MAX) top.toUi() else null

        return CandidateUi(
            packageName = candidate.packageName,
            appLabel = candidate.appLabel,
            versionName = candidate.versionName,
            installerLabel = candidate.installerKind.toUiLabel(),
            suggestions = suggestionsDomain.take(3).map { it.toUi() }.toImmutableList(),
            preselectedSuggestion = preselected,
        )
    }

    private fun submitSearchOverride(packageName: String) {
        val current = _state.value
        // Search submit applies to whichever card was last typed in. If the
        // active package and the submitted package mismatch, we still honour
        // the submit using the active query — this only happens if the user
        // taps the icon in a different card before the keystrokes registered,
        // which the UI prevents via per-card query binding.
        if (current.activeSearchPackage != packageName) return

        val query = current.searchQuery.trim()
        if (query.isEmpty()) {
            searchJob?.cancel()
            _state.update {
                it.copy(
                    isSearching = false,
                    searchError = null,
                    searchResults = persistentListOf(),
                )
            }
            return
        }

        // Fast-path: a github.com/owner/repo URL bypasses the search API and
        // surfaces a single MANUAL suggestion that the user can tap to link.
        // This unblocks users with rate-limited search and matches Obtainium's
        // "paste a URL" mental model.
        parseGithubRepoUrl(query)?.let { (owner, repo) ->
            searchJob?.cancel()
            _state.update {
                it.copy(
                    isSearching = false,
                    searchError = null,
                    searchResults = persistentListOf(
                        RepoSuggestionUi(
                            owner = owner,
                            repo = repo,
                            confidence = 1.0,
                            source = SuggestionSource.MANUAL,
                            stars = null,
                            description = null,
                        ),
                    ),
                )
            }
            viewModelScope.launch { runCatching { telemetry.importSearchOverrideUsed() } }
            return
        }

        searchJob?.cancel()
        _state.update { it.copy(isSearching = true, searchError = null) }
        viewModelScope.launch { runCatching { telemetry.importSearchOverrideUsed() } }
        searchJob = viewModelScope.launch {
            val result = runCatching { externalImportRepository.searchRepos(query) }
                .getOrElse { e ->
                    if (e is CancellationException) throw e
                    Result.failure(e)
                }

            result.fold(
                onSuccess = { suggestions ->
                    if (suggestions.isEmpty()) {
                        runCatching { telemetry.importSearchOverrideNoResults() }
                    }
                    _state.update {
                        // Stale-completion guard: if the user collapsed or switched cards
                        // while the request was in flight, drop the response on the floor
                        // so old results never bleed into a newly-active card.
                        if (it.activeSearchPackage != packageName) it
                        else it.copy(
                            isSearching = false,
                            searchError = null,
                            searchResults =
                                suggestions.map { s -> s.toUi() }.toImmutableList(),
                        )
                    }
                },
                onFailure = { e ->
                    if (e is CancellationException) throw e
                    logger.error("Search override failed for '$query': ${e.message}")
                    val fallback = getString(Res.string.external_import_search_error_default)
                    _state.update {
                        if (it.activeSearchPackage != packageName) it
                        else it.copy(
                            isSearching = false,
                            searchError = e.message ?: fallback,
                            searchResults = persistentListOf(),
                        )
                    }
                },
            )
        }
    }

    private fun skipPackage(packageName: String, neverAsk: Boolean) {
        val card = _state.value.cards.firstOrNull { it.packageName == packageName } ?: return
        viewModelScope.launch {
            // Distinguish "no prior row" (success → null) from "couldn't read"
            // (failure). Treating the latter as null would let undoLast's
            // fallback unlink wipe a row that should have been preserved.
            val snapshotResult = runCatching {
                externalImportRepository.snapshotDecision(packageName)
            }
            if (snapshotResult.isFailure) {
                logger.error("Snapshot read failed for $packageName: ${snapshotResult.exceptionOrNull()?.message}")
                _events.send(
                    ExternalImportEvent.ShowError(
                        getString(Res.string.external_import_error_link_failed),
                    ),
                )
                return@launch
            }
            val snapshot = snapshotResult.getOrNull()
            val hadInstalledRow = installedAppsRepository.getAppByPackage(packageName) != null

            // Short-circuit on failure: don't remove the card, don't offer undo,
            // don't fire telemetry — the DAO state is unchanged. Surface an error
            // so the user knows the action didn't take effect.
            val ok = try {
                externalImportRepository.skipPackage(packageName, neverAsk = neverAsk)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Skip failed for $packageName: ${e.message}")
                false
            }
            if (!ok) {
                _events.send(
                    ExternalImportEvent.ShowError(
                        getString(Res.string.external_import_error_link_failed),
                    ),
                )
                return@launch
            }

            runCatching {
                telemetry.importSkipped(
                    countBucket = "1-2",
                    persisted = if (neverAsk) "forever" else "7day",
                )
            }
            removeCardFromState(packageName) { it.copy(skipped = it.skipped + 1) }

            pendingUndo = PendingUndo(
                card = card,
                snapshot = snapshot,
                hadInstalledAppRowBefore = hadInstalledRow,
                kind = PendingUndo.Kind.Skip,
            )
            _events.send(
                ExternalImportEvent.ShowUndoSnackbar(
                    getString(Res.string.external_import_undo_skipped, card.appLabel),
                ),
            )
        }
    }

    private fun pickSuggestion(packageName: String, suggestion: RepoSuggestionUi) {
        val card = _state.value.cards.firstOrNull { it.packageName == packageName } ?: return
        val preselected = card.preselectedSuggestion
        val source = if (suggestion == preselected) "preselected" else "alternative"
        val candidate = candidatesByPackage[packageName]

        viewModelScope.launch {
            if (candidate == null) {
                logger.error("Cannot materialize $packageName: candidate missing from snapshot")
                _events.send(
                    ExternalImportEvent.ShowError(
                        getString(Res.string.external_import_error_link_failed),
                    ),
                )
                return@launch
            }

            val snapshotResult = runCatching {
                externalImportRepository.snapshotDecision(packageName)
            }
            if (snapshotResult.isFailure) {
                logger.error("Snapshot read failed for $packageName: ${snapshotResult.exceptionOrNull()?.message}")
                _events.send(
                    ExternalImportEvent.ShowError(
                        getString(Res.string.external_import_error_link_failed),
                    ),
                )
                return@launch
            }
            val snapshot = snapshotResult.getOrNull()
            val hadInstalledRow = installedAppsRepository.getAppByPackage(packageName) != null

            val materialized = materializeAndMark(candidate, suggestion.owner, suggestion.repo, source)
            if (!materialized) {
                _events.send(
                    ExternalImportEvent.ShowError(
                        getString(Res.string.external_import_error_link_network),
                    ),
                )
                return@launch
            }
            runCatching {
                telemetry.importManuallyLinked(countBucket = "1-2", source = source)
            }
            removeCardFromState(packageName) { it.copy(manuallyLinked = it.manuallyLinked + 1) }

            pendingUndo = PendingUndo(
                card = card,
                snapshot = snapshot,
                hadInstalledAppRowBefore = hadInstalledRow,
                kind = PendingUndo.Kind.Link,
            )
            _events.send(
                ExternalImportEvent.ShowUndoSnackbar(
                    getString(Res.string.external_import_undo_linked, card.appLabel),
                ),
            )
        }
    }

    private fun linkCardWithPreselected(packageName: String) {
        val card = _state.value.cards.firstOrNull { it.packageName == packageName } ?: return
        val preselect = card.preselectedSuggestion
        if (preselect != null) {
            pickSuggestion(packageName, preselect)
        } else {
            // No preselection means there's nothing to confidently link to. The
            // list-mode UI hides the link CTA in this case, but defensively
            // surface the expand affordance instead of silently dropping.
            toggleCardExpanded(packageName)
        }
    }

    private fun undoLast() {
        val undo = pendingUndo ?: return

        viewModelScope.launch {
            try {
                // Run rollback DAO ops without swallowing — any failure must
                // abort and preserve `pendingUndo` so the user can retry from
                // the snackbar. UI state is mutated only after every op succeeds.
                if (undo.kind == PendingUndo.Kind.Link && !undo.hadInstalledAppRowBefore) {
                    installedAppsRepository.deleteInstalledApp(undo.packageName)
                }

                if (undo.snapshot != null) {
                    externalImportRepository.restoreDecision(undo.snapshot)
                } else {
                    externalImportRepository.unlink(undo.packageName)
                }

                // All DAO ops succeeded — now mutate UI and consume the token.
                pendingUndo = null
                _state.update { current ->
                    if (current.cards.any { it.packageName == undo.packageName }) {
                        current
                    } else {
                        val tally = when (undo.kind) {
                            PendingUndo.Kind.Link ->
                                current.copy(
                                    manuallyLinked = (current.manuallyLinked - 1).coerceAtLeast(0),
                                )
                            PendingUndo.Kind.Skip ->
                                current.copy(
                                    skipped = (current.skipped - 1).coerceAtLeast(0),
                                )
                        }
                        tally.copy(
                            cards = (listOf(undo.card) + current.cards).toImmutableList(),
                            phase = ImportPhase.AwaitingReview,
                            showCompletionToast = false,
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Undo failed for ${undo.packageName}: ${e.message}")
                // Preserve pendingUndo so the snackbar can offer Undo again.
                _events.send(
                    ExternalImportEvent.ShowError(
                        getString(Res.string.external_import_undo_failed),
                    ),
                )
            }
        }
    }

    private fun autoSummaryContinue() {
        val current = _state.value
        if (current.phase != ImportPhase.AutoImportSummary) return
        // User accepted the auto-imports; the pre-link metadata is no
        // longer needed and shouldn't leak into a subsequent wizard run.
        autoLinkedHadInstalledRow = emptyMap()
        autoLinkedPreSnapshots = emptyMap()
        if (current.cards.isNotEmpty()) {
            _state.update { it.copy(phase = ImportPhase.AwaitingReview) }
        } else {
            _state.update {
                it.copy(
                    phase = ImportPhase.Done,
                    showCompletionToast = true,
                )
            }
            viewModelScope.launch { _events.send(ExternalImportEvent.PlayConfetti) }
        }
    }

    private fun autoSummaryUndoAll() {
        val current = _state.value
        if (current.phase != ImportPhase.AutoImportSummary) return
        val packages = current.autoLinkedPackages.toList()
        if (packages.isEmpty()) {
            _state.update { it.copy(phase = ImportPhase.AwaitingReview) }
            return
        }

        // Snapshot the pre-link maps locally so a concurrent reset can't race us.
        val hadInstalledMap = autoLinkedHadInstalledRow
        val preSnapshots = autoLinkedPreSnapshots

        viewModelScope.launch {
            // Roll each auto-linked package back to its PRE-LINK external_links
            // state using the snapshot captured BEFORE materializeAndMark wrote
            // the MATCHED row. installed_apps is only deleted for packages whose
            // row did NOT pre-exist before auto-link — same policy as undoLast.
            //
            // Fail-fast: any DAO failure aborts the bulk undo before we touch
            // UI state or clear the pre-link maps. The user keeps seeing the
            // AutoImportSummary screen and can retry. Already-rolled-back
            // packages stay rolled back (idempotent on retry).
            try {
                packages.forEach { pkg ->
                    val preSnapshot = preSnapshots[pkg]
                    val hadRowBefore = hadInstalledMap[pkg] == true
                    if (!hadRowBefore) {
                        installedAppsRepository.deleteInstalledApp(pkg)
                    }
                    if (preSnapshot != null) {
                        externalImportRepository.restoreDecision(preSnapshot)
                    } else {
                        // No pre-link row existed — drop the auto-link row entirely.
                        externalImportRepository.unlink(pkg)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("Bulk undo failed: ${e.message}")
                _events.send(
                    ExternalImportEvent.ShowError(
                        getString(Res.string.external_import_undo_failed),
                    ),
                )
                return@launch
            }

            // All rollbacks succeeded — invalidate the single-row undo token,
            // clear the per-package metadata so a subsequent wizard run can't
            // see stale pre-link snapshots, and rebuild the wizard.
            pendingUndo = null
            autoLinkedHadInstalledRow = emptyMap()
            autoLinkedPreSnapshots = emptyMap()

            val matchesByPkg = lastResolvedMatches.associateBy { it.packageName }
            val restoredCards = packages.mapNotNull { pkg ->
                val candidate = candidatesByPackage[pkg] ?: return@mapNotNull null
                buildCard(candidate, matchesByPkg[pkg])
            }

            _state.update { state ->
                val merged = (restoredCards + state.cards)
                    .distinctBy { it.packageName }
                    .toImmutableList()
                state.copy(
                    phase = if (merged.isEmpty()) ImportPhase.Done else ImportPhase.AwaitingReview,
                    cards = merged,
                    autoImported = 0,
                    autoLinkedPackages = persistentListOf(),
                    autoLinkedLabels = persistentListOf(),
                    showCompletionToast = merged.isEmpty(),
                )
            }
            if (_state.value.cards.isEmpty()) {
                _events.send(ExternalImportEvent.PlayConfetti)
            }
        }
    }

    private fun emitPermissionOutcome(granted: Boolean, sdkInt: Int?) {
        viewModelScope.launch {
            runCatching {
                telemetry.importPermissionOutcome(
                    granted = granted,
                    sdkIntBucket = bucketSdkInt(sdkInt),
                )
            }
        }
    }

    private fun bucketSdkInt(sdkInt: Int?): String =
        when {
            sdkInt == null -> "unknown"
            sdkInt in 26..29 -> "26-29"
            sdkInt in 30..32 -> "30-32"
            sdkInt >= 33 -> "33+"
            else -> "unknown"
        }

    private fun bucketCount(count: Int): String =
        when {
            count <= 0 -> "0"
            count in 1..2 -> "1-2"
            count in 3..9 -> "3-9"
            count in 10..49 -> "10-49"
            else -> "50+"
        }

    private fun skipRemaining() {
        val current = _state.value
        if (current.phase != ImportPhase.AwaitingReview) return
        val remaining = current.cards
        if (remaining.isEmpty()) return

        viewModelScope.launch {
            // Track per-package outcome so a partial failure doesn't claim the
            // entire wizard cleared and doesn't fire confetti / Done telemetry
            // on packages whose DAO state is unchanged.
            val successes = mutableSetOf<String>()
            val failures = mutableListOf<String>()
            remaining.forEach { card ->
                try {
                    externalImportRepository.skipPackage(card.packageName, neverAsk = false)
                    successes += card.packageName
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Skip-remaining failed for ${card.packageName}: ${e.message}")
                    failures += card.packageName
                }
            }

            if (successes.isNotEmpty()) {
                runCatching {
                    telemetry.importSkipped(
                        countBucket = bucketCount(successes.size),
                        persisted = "7day",
                    )
                }
            }

            // Skip-remaining is intentionally not undoable — bulk skip clears
            // the wizard and triggers the completion screen, and a single
            // snackbar isn't a sensible affordance for "undo seven things".
            pendingUndo = null

            val allSucceeded = failures.isEmpty()
            _state.update { state ->
                val keptCards = state.cards.filter { it.packageName !in successes }.toImmutableList()
                state.copy(
                    cards = keptCards,
                    expandedPackages = persistentSetOf(),
                    activeSearchPackage = null,
                    searchQuery = "",
                    searchResults = persistentListOf(),
                    isSearching = false,
                    searchError = null,
                    phase = if (allSucceeded && keptCards.isEmpty()) ImportPhase.Done else state.phase,
                    skipped = state.skipped + successes.size,
                    showCompletionToast = allSucceeded && keptCards.isEmpty(),
                )
            }

            if (allSucceeded) {
                _events.send(ExternalImportEvent.PlayConfetti)
            } else {
                _events.send(
                    ExternalImportEvent.ShowError(
                        getString(Res.string.external_import_error_link_failed),
                    ),
                )
            }
        }
    }

    private suspend fun autoMaterialize(matches: List<RepoMatchResult>): List<String> {
        val linked = mutableListOf<String>()
        val hadInstalledRow = mutableMapOf<String, Boolean>()
        val preSnapshots = mutableMapOf<String, ExternalDecisionSnapshot?>()
        matches.forEach { result ->
            val top = result.topSuggestion ?: return@forEach
            if (top.confidence < AUTO_LINK_THRESHOLD) return@forEach
            val candidate = candidatesByPackage[result.packageName] ?: return@forEach

            // Capture pre-link state BEFORE materializeAndMark writes the
            // MATCHED row. Bulk undo uses both: the pre-link snapshot to
            // restore the DAO row to its original state, and the
            // installed_apps presence flag to decide whether to delete the
            // installed_apps row (only if auto-link created it).
            //
            // Snapshot failure must skip the auto-link entirely — without a
            // reliable pre-link snapshot, undo would fall back to unlink and
            // wipe a row that should have been preserved. Push the candidate
            // through manual review instead.
            val snapshotResult = runCatching {
                externalImportRepository.snapshotDecision(result.packageName)
            }
            if (snapshotResult.isFailure) {
                logger.warn(
                    "Skip auto-link for ${result.packageName}: " +
                        "pre-link snapshot read failed (${snapshotResult.exceptionOrNull()?.message})",
                )
                return@forEach
            }
            val preSnapshot = snapshotResult.getOrNull()
            val pre = runCatching {
                installedAppsRepository.getAppByPackage(result.packageName) != null
            }.getOrDefault(false)

            val ok =
                materializeAndMark(
                    candidate = candidate,
                    owner = top.owner,
                    repo = top.repo,
                    source = "auto-${top.source.name.lowercase()}",
                )
            if (ok) {
                linked += result.packageName
                hadInstalledRow[result.packageName] = pre
                preSnapshots[result.packageName] = preSnapshot
            }
        }
        autoLinkedHadInstalledRow = hadInstalledRow.toMap()
        autoLinkedPreSnapshots = preSnapshots.toMap()
        return linked
    }

    private suspend fun materializeAndMark(
        candidate: ExternalAppCandidate,
        owner: String,
        repo: String,
        source: String,
    ): Boolean {
        val repoInfo =
            try {
                appsRepository.fetchRepoInfo(owner, repo)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.error("fetchRepoInfo($owner/$repo) failed: ${e.message}")
                null
            }
        if (repoInfo == null) {
            logger.warn("Skipping link for ${candidate.packageName}: repo $owner/$repo not found")
            return false
        }

        val deviceApp = candidate.toDeviceApp()
        try {
            appsRepository.linkAppToRepo(deviceApp, repoInfo)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("linkAppToRepo failed for ${candidate.packageName}: ${e.message}")
            return false
        }

        val linkResult =
            try {
                externalImportRepository.linkManually(
                    packageName = candidate.packageName,
                    owner = owner,
                    repo = repo,
                    source = source,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Result.failure(e)
            }
        if (linkResult.isFailure) {
            logger.error(
                "external_links upsert failed for ${candidate.packageName}: " +
                    "${linkResult.exceptionOrNull()?.message}",
            )
            // installed_apps row is already written; the audit trail is
            // ahead but recoverable on the next scan via mergeCandidate.
        }
        return true
    }

    private fun ExternalAppCandidate.toDeviceApp(): DeviceApp =
        DeviceApp(
            packageName = packageName,
            appName = appLabel,
            versionName = versionName,
            versionCode = versionCode,
            signingFingerprint = signingFingerprint,
        )

    private suspend fun removeCardFromState(
        packageName: String,
        tally: (ExternalImportState) -> ExternalImportState,
    ) {
        // _state.update may invoke the lambda multiple times under contention;
        // never assign captured vars from inside it. Read the post-update
        // state to decide whether to fire the completion event.
        _state.update { current ->
            val newCards = current.cards.filterNot { it.packageName == packageName }.toImmutableList()
            val tallied = tally(current).copy(
                cards = newCards,
                expandedPackages = current.expandedPackages.toPersistentSet().remove(packageName),
                activeSearchPackage = if (current.activeSearchPackage == packageName) null else current.activeSearchPackage,
                searchQuery = if (current.activeSearchPackage == packageName) "" else current.searchQuery,
                searchResults = if (current.activeSearchPackage == packageName) persistentListOf() else current.searchResults,
                isSearching = if (current.activeSearchPackage == packageName) false else current.isSearching,
                searchError = if (current.activeSearchPackage == packageName) null else current.searchError,
            )
            if (newCards.isEmpty()) {
                tallied.copy(
                    phase = ImportPhase.Done,
                    showCompletionToast = true,
                )
            } else {
                tallied
            }
        }
        if (_state.value.cards.isEmpty()) {
            _events.send(ExternalImportEvent.PlayConfetti)
        }
    }

    private fun RepoMatchSuggestion.toUi(): RepoSuggestionUi =
        RepoSuggestionUi(
            owner = owner,
            repo = repo,
            confidence = confidence,
            source =
                when (source) {
                    RepoMatchSource.MANIFEST -> SuggestionSource.MANIFEST
                    RepoMatchSource.SEARCH -> SuggestionSource.SEARCH
                    RepoMatchSource.FINGERPRINT -> SuggestionSource.FINGERPRINT
                    RepoMatchSource.MANUAL -> SuggestionSource.MANUAL
                },
            stars = stars,
            description = description,
        )

    private suspend fun InstallerKind.toUiLabel(): String =
        // Exhaustive: STORE_PLAY / STORE_AURORA / STORE_GALAXY / STORE_OEM_OTHER /
        // SYSTEM are filtered out at the scanner today, but the wizard's enum
        // contract is shared with the scanner — handle every value explicitly so
        // a future scanner change can't silently mislabel a candidate.
        when (this) {
            InstallerKind.STORE_OBTAINIUM -> getString(Res.string.external_import_installer_obtainium)
            InstallerKind.STORE_FDROID -> getString(Res.string.external_import_installer_fdroid)
            InstallerKind.BROWSER -> getString(Res.string.external_import_installer_browser)
            InstallerKind.SIDELOAD -> getString(Res.string.external_import_installer_sideload)
            InstallerKind.GITHUB_STORE_SELF -> getString(Res.string.external_import_installer_self)
            InstallerKind.UNKNOWN,
            InstallerKind.STORE_PLAY,
            InstallerKind.STORE_AURORA,
            InstallerKind.STORE_GALAXY,
            InstallerKind.STORE_OEM_OTHER,
            InstallerKind.SYSTEM,
            -> getString(Res.string.external_import_installer_unknown)
        }

    private data class PendingUndo(
        val card: CandidateUi,
        val snapshot: ExternalDecisionSnapshot?,
        val hadInstalledAppRowBefore: Boolean,
        val kind: Kind,
    ) {
        val packageName: String get() = card.packageName
        val appLabel: String get() = card.appLabel

        enum class Kind { Skip, Link }
    }

    companion object {
        private const val AUTO_LINK_THRESHOLD = 0.85
        private const val PRESELECT_MIN = 0.5
        private const val PRESELECT_MAX = 0.85
    }
}

private fun parseGithubRepoUrl(input: String): Pair<String, String>? {
    val trimmed = input.trim().removeSuffix("/")
    if (trimmed.isEmpty()) return null
    // Accept both bare host references and full https URLs. Anything else
    // (search keywords, partial slugs without owner) falls through to the
    // backend search path.
    val withoutScheme = trimmed
        .removePrefix("https://")
        .removePrefix("http://")
        .removePrefix("www.")
    if (!withoutScheme.startsWith("github.com/", ignoreCase = true)) return null
    val path = withoutScheme.substring("github.com/".length)
    val parts = path.split('/').filter { it.isNotEmpty() }
    if (parts.size < 2) return null
    val owner = parts[0]
    val repo = parts[1].substringBefore('?').substringBefore('#')
    if (!isValidGithubSegment(owner) || !isValidGithubSegment(repo)) return null
    return owner to repo
}

private fun isValidGithubSegment(s: String): Boolean =
    s.isNotEmpty() &&
        s.length <= 100 &&
        s.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
