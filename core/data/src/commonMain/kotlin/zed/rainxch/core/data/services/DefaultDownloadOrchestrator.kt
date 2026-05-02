package zed.rainxch.core.data.services

import co.touchlab.kermit.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import zed.rainxch.core.domain.network.DigestVerifier
import zed.rainxch.core.domain.network.Downloader
import zed.rainxch.core.domain.network.SlowDownloadDetector
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.system.DownloadOrchestrator
import zed.rainxch.core.domain.system.DownloadSpec
import zed.rainxch.core.domain.system.DownloadStage
import zed.rainxch.core.domain.system.InstallOutcome
import zed.rainxch.core.domain.system.InstallPolicy
import zed.rainxch.core.domain.system.Installer
import zed.rainxch.core.domain.system.MultiSourceDownloader
import zed.rainxch.core.domain.system.OrchestratedDownload
import zed.rainxch.core.domain.system.PendingInstallNotifier
import zed.rainxch.core.domain.util.AssetFileName
import kotlin.random.Random

/**
 * Default implementation of [DownloadOrchestrator].
 *
 * # Lifetime
 *
 * Owned by Koin as a singleton with the application-scoped
 * [CoroutineScope] (see `coreModule`'s `CoroutineScope` factory). All
 * downloads run in [appScope] so they outlive any ViewModel that
 * triggered them. The orchestrator itself never cancels its scope —
 * the process going away does that for free.
 *
 * # Concurrency
 *
 * Uses a per-package [Mutex] surfaced via [stateMutex] to serialize
 * map mutations. Actual download/install work runs in [appScope] so
 * the mutex is only held for short metadata writes — never across
 * the long-running download.
 *
 * Up to [DEFAULT_MAX_CONCURRENT] downloads run at once. Further
 * enqueue calls don't block — they just spawn a coroutine that
 * `delay`s while the active count is at capacity. The simpler "always
 * spawn, let the platform Downloader queue internally" approach is
 * tempting, but OkHttp doesn't queue — it'd open every connection at
 * once and likely hit GitHub's rate limit.
 *
 * # Filename namespacing
 *
 * Every download is written to disk with the
 * `<owner>_<repo>_<original>.ext` form via [AssetFileName.scoped].
 * This is the only place that name transformation lives — callers
 * pass the original asset name and the orchestrator handles the rest.
 * The scoped name is also what gets passed to [Downloader.download],
 * so the Downloader's per-name dedup map is automatically scoped.
 *
 * # Install policies
 *
 * See [InstallPolicy] for the rules. Implementation detail: the
 * "screen still attached" check at install time is done by re-reading
 * `installPolicy` from the live state map *after* the download
 * completes — never from a captured local — so a [downgradeToDeferred]
 * call lands atomically against the in-flight download.
 */
class DefaultDownloadOrchestrator(
    private val downloader: Downloader,
    private val multiSourceDownloader: MultiSourceDownloader,
    private val digestVerifier: DigestVerifier,
    private val installer: Installer,
    private val installedAppsRepository: InstalledAppsRepository,
    private val pendingInstallNotifier: PendingInstallNotifier,
    private val slowDownloadDetector: SlowDownloadDetector,
    private val appScope: CoroutineScope,
) : DownloadOrchestrator {
    private companion object {
        private const val DEFAULT_MAX_CONCURRENT = 3
        private const val QUEUE_POLL_DELAY_MS = 200L
    }

    private val _downloads = MutableStateFlow<Map<String, OrchestratedDownload>>(emptyMap())
    override val downloads: StateFlow<Map<String, OrchestratedDownload>> = _downloads.asStateFlow()

    /**
     * Mutex guarding map mutations *and* the active-count check used
     * by the queue. Held only for short critical sections — never
     * across `Downloader.download(...).collect`.
     */
    private val stateMutex = Mutex()

    /**
     * Job handles per package, used by [cancel] to interrupt the
     * download. Separate from `_downloads` because Job is mutable
     * state that doesn't belong in an immutable StateFlow value.
     */
    private val activeJobs = mutableMapOf<String, Job>()

    override fun observe(packageName: String): Flow<OrchestratedDownload?> =
        _downloads
            .map { it[packageName] }
            .distinctUntilChanged()

    override suspend fun enqueue(spec: DownloadSpec): String {
        // Idempotent: if a download for this package is already
        // running (or queued, or awaiting install), return its id and
        // upgrade the install policy if the new caller wants a
        // stronger guarantee. Common case: user taps Update twice in
        // a row and we don't want to spawn two downloads.
        stateMutex.withLock {
            val existing = _downloads.value[spec.packageName]
            if (existing != null && existing.stage != DownloadStage.Failed &&
                existing.stage != DownloadStage.Cancelled
            ) {
                // Allow caller to upgrade policy: e.g. apps list
                // started a Deferred download, then user opened
                // Details and we want to flip to InstallWhileForeground.
                if (existing.installPolicy != spec.installPolicy &&
                    existing.installPolicy.priority < spec.installPolicy.priority
                ) {
                    _downloads.update { state ->
                        state + (spec.packageName to existing.copy(installPolicy = spec.installPolicy))
                    }
                }
                return existing.id
            }
        }

        val id = generateId()
        val initial =
            OrchestratedDownload(
                id = id,
                packageName = spec.packageName,
                repoOwner = spec.repoOwner,
                repoName = spec.repoName,
                displayAppName = spec.displayAppName,
                assetName = spec.asset.name,
                assetSize = spec.asset.size,
                downloadUrl = spec.asset.downloadUrl,
                releaseTag = spec.releaseTag,
                filePath = null,
                installPolicy = spec.installPolicy,
                stage = DownloadStage.Queued,
                progressPercent = null,
            )
        stateMutex.withLock {
            _downloads.update { it + (spec.packageName to initial) }
        }

        val job =
            appScope.launch {
                try {
                    runDownload(spec)
                } catch (e: CancellationException) {
                    // Honour structured concurrency. The cancel path
                    // is responsible for cleaning state.
                    Logger.d { "Orchestrator: download cancelled for ${spec.packageName}" }
                    throw e
                } catch (t: Throwable) {
                    Logger.e(t) { "Orchestrator: download/install failed for ${spec.packageName}" }
                    markFailed(spec.packageName, t.message)
                } finally {
                    stateMutex.withLock {
                        if (activeJobs[spec.packageName]?.isCompleted == true ||
                            activeJobs[spec.packageName] == null
                        ) {
                            activeJobs.remove(spec.packageName)
                        }
                    }
                }
            }
        stateMutex.withLock {
            activeJobs[spec.packageName] = job
        }
        return id
    }

    /**
     * The actual download/install pipeline. Runs entirely inside
     * [appScope].
     *
     *  1. Wait for a slot if [DEFAULT_MAX_CONCURRENT] is at capacity
     *  2. Stream bytes via [Downloader] under the scoped filename,
     *     emitting progress updates
     *  3. Read the latest [InstallPolicy] from state — this is the
     *     race-safe read that lets [downgradeToDeferred] land
     *  4. Branch on policy: install in-process, defer to user, or
     *     hand off to the dialog-driven Details flow
     */
    private suspend fun runDownload(spec: DownloadSpec) {
        // Wait for a worker slot. Polling is acceptable here because
        // the wait is not on the user's critical path (a queued
        // download is by definition something the user is OK waiting
        // on).
        while (true) {
            val activeNow =
                stateMutex.withLock {
                    _downloads.value.values.count { it.stage == DownloadStage.Downloading }
                }
            if (activeNow < DEFAULT_MAX_CONCURRENT) break
            kotlinx.coroutines.delay(QUEUE_POLL_DELAY_MS)
        }

        val scopedName =
            AssetFileName.scoped(
                owner = spec.repoOwner,
                repo = spec.repoName,
                originalName = spec.asset.name,
            )

        // Move to Downloading. Initialize totalBytes from the asset's
        // declared size — the server's Content-Length might disagree
        // (or be missing), in which case the download flow will
        // override it with the live value below.
        updateEntry(spec.packageName) {
            it.copy(
                stage = DownloadStage.Downloading,
                bytesDownloaded = 0L,
                totalBytes = spec.asset.size.takeIf { size -> size > 0 },
            )
        }

        multiSourceDownloader.download(spec.asset.downloadUrl, scopedName).collect { progress ->
            slowDownloadDetector.onProgress(progress)
            updateEntry(spec.packageName) {
                it.copy(
                    progressPercent = progress.percent,
                    bytesDownloaded = progress.bytesDownloaded,
                    // Prefer the live Content-Length when present;
                    // fall back to whatever we already had (asset.size).
                    totalBytes = progress.totalBytes ?: it.totalBytes,
                )
            }
        }

        val filePath =
            downloader.getDownloadedFilePath(scopedName)
                ?: throw IllegalStateException("Downloaded file missing: $scopedName")

        updateEntry(spec.packageName) {
            it.copy(filePath = filePath, progressPercent = 100)
        }

        // SHA-256 gate sits between download-complete and the install /
        // park transition: a tampered mirror must not be able to feed
        // an APK into the installer. Null digest = GitHub didn't expose
        // one (older assets, non-asset endpoints); log and proceed.
        val expectedDigest = spec.asset.digest
        if (expectedDigest != null) {
            val mismatch = digestVerifier.verify(filePath, expectedDigest)
            if (mismatch != null) {
                Logger.w { "Orchestrator: digest mismatch for ${spec.asset.name}: $mismatch" }
                runCatching { java.io.File(filePath).delete() }
                markFailed(
                    spec.packageName,
                    "Checksum mismatch — file may have been tampered with",
                )
                return
            }
        } else {
            Logger.i { "No digest for ${spec.asset.name}, skipping SHA-256 verification" }
        }

        // Race-safe read of the *current* install policy. The
        // ViewModel may have called downgradeToDeferred while the
        // download was in flight; that mutation lands here.
        val effectivePolicy =
            stateMutex.withLock {
                _downloads.value[spec.packageName]?.installPolicy ?: spec.installPolicy
            }

        when (effectivePolicy) {
            InstallPolicy.AlwaysInstall -> runInstall(spec, filePath)

            // InstallWhileForeground: park *without* notifying. The
            // foreground VM is expected to be observing and will run
            // its own dialog-driven install on the file. The notifier
            // only fires when the policy gets downgraded to Deferred
            // (either explicitly via downgradeToDeferred, or
            // originally), so the user only sees a notification when
            // there's nobody watching the download interactively.
            InstallPolicy.InstallWhileForeground -> parkForUser(spec, filePath, notify = false)

            InstallPolicy.DeferUntilUserAction -> parkForUser(spec, filePath, notify = true)
        }
    }

    /**
     * Invokes the platform installer for [filePath]. On success the
     * orchestrator entry is moved to [DownloadStage.Completed] (the
     * consuming ViewModel will dismiss it from the map).
     *
     * Validation (signing fingerprint, package mismatch, etc.) is
     * deliberately NOT done here. The dialog-driven path in
     * `DetailsViewModel` handles those because they need user
     * interaction. The orchestrator's "always install" path is used
     * for Shizuku silent installs where the user has explicitly opted
     * out of confirmations.
     */
    private suspend fun runInstall(spec: DownloadSpec, filePath: String) {
        updateEntry(spec.packageName) { it.copy(stage = DownloadStage.Installing) }
        val ext = spec.asset.name.substringAfterLast('.', "").lowercase()
        try {
            installer.ensurePermissionsOrThrow(ext)
            val outcome = installer.install(filePath, ext)
            when (outcome) {
                InstallOutcome.COMPLETED -> {
                    // Genuine silent install (Shizuku/Sui). Clear any
                    // pending file path on the row (if set by a prior
                    // aborted attempt) and move the entry to Completed.
                    try {
                        installedAppsRepository.setPendingInstallFilePath(spec.packageName, null)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.w(e) { "Orchestrator: failed to clear pending install path" }
                    }
                    pendingInstallNotifier.clearPending(spec.packageName)
                    updateEntry(spec.packageName) {
                        it.copy(stage = DownloadStage.Completed, installOutcome = outcome)
                    }
                }

                InstallOutcome.DELEGATED_TO_SYSTEM -> {
                    // The install was *handed off* to the system
                    // installer (e.g. Shizuku binder went away and the
                    // wrapper fell back to the standard installer). The
                    // user has not confirmed yet — they may cancel the
                    // system prompt. Do NOT mark this as a real install.
                    // Keep the file parked so the foreground VM can re-
                    // run validation if needed and the apps row can show
                    // a "ready to install" state. The eventual
                    // PACKAGE_ADDED broadcast (or its absence on cancel)
                    // is what flips `isPendingInstall` correctly.
                    Logger.i {
                        "Orchestrator: AlwaysInstall path returned DELEGATED_TO_SYSTEM " +
                            "for ${spec.packageName}; Completed-with-pending so DB row stays pending"
                    }
                    updateEntry(spec.packageName) {
                        it.copy(stage = DownloadStage.Completed, installOutcome = outcome)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Logger.e(t) { "Orchestrator: install failed for ${spec.packageName}" }
            markFailed(spec.packageName, t.message)
        }
    }

    /**
     * "Park" path: download finished but the install policy says
     * "don't auto-install". Persists the file path on the InstalledApp
     * row so the apps list can show a "Ready to install" row, optionally
     * fires the notification, and moves the orchestrator entry to
     * [DownloadStage.AwaitingInstall].
     *
     * @param notify Whether to poke the notifier. False for the
     *   InstallWhileForeground case (the foreground VM is watching);
     *   true for the DeferUntilUserAction case (no foreground watcher).
     */
    private suspend fun parkForUser(
        spec: DownloadSpec,
        filePath: String,
        notify: Boolean,
    ) {
        try {
            installedAppsRepository.setPendingInstallFilePath(
                packageName = spec.packageName,
                path = filePath,
                version = spec.releaseTag,
                assetName = spec.asset.name,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.e(e) { "Orchestrator: failed to persist pending install path" }
            // Persistence is best-effort — the orchestrator state
            // still has the file path, so the row can render via
            // observe() during the same process lifetime. Survives
            // process death is what we lose.
        }
        if (notify) {
            pendingInstallNotifier.notifyPending(
                packageName = spec.packageName,
                repoOwner = spec.repoOwner,
                repoName = spec.repoName,
                appName = spec.displayAppName,
                versionTag = spec.releaseTag,
            )
        }
        updateEntry(spec.packageName) { it.copy(stage = DownloadStage.AwaitingInstall) }
    }

    override suspend fun downgradeToDeferred(packageName: String) {
        // Snapshot what we need under the lock and decide whether to
        // notify outside it (notifier is third-party code, don't hold
        // the mutex across it).
        val shouldNotify: Boolean
        val notifySpec: NotifyData?
        stateMutex.withLock {
            val existing = _downloads.value[packageName] ?: return
            when (existing.stage) {
                DownloadStage.Queued, DownloadStage.Downloading -> {
                    // In-flight: just flip the policy. parkForUser
                    // will see it later and notify.
                    _downloads.update { state ->
                        state + (
                            packageName to existing.copy(
                                installPolicy = InstallPolicy.DeferUntilUserAction,
                            )
                        )
                    }
                    shouldNotify = false
                    notifySpec = null
                }

                DownloadStage.AwaitingInstall -> {
                    // Race: the orchestrator already parked the file
                    // (silently, because the policy was
                    // InstallWhileForeground) but the foreground VM
                    // is now going away. We need to retroactively
                    // notify the user so they don't lose track of
                    // the ready-to-install file.
                    val needsNotify =
                        existing.installPolicy == InstallPolicy.InstallWhileForeground
                    _downloads.update { state ->
                        state + (
                            packageName to existing.copy(
                                installPolicy = InstallPolicy.DeferUntilUserAction,
                            )
                        )
                    }
                    shouldNotify = needsNotify
                    notifySpec = if (needsNotify) {
                        NotifyData(
                            packageName = existing.packageName,
                            repoOwner = existing.repoOwner,
                            repoName = existing.repoName,
                            appName = existing.displayAppName,
                            versionTag = existing.releaseTag,
                        )
                    } else {
                        null
                    }
                }

                DownloadStage.Installing,
                DownloadStage.Completed,
                DownloadStage.Cancelled,
                DownloadStage.Failed,
                -> {
                    // Too late or terminal — nothing meaningful to do.
                    shouldNotify = false
                    notifySpec = null
                }
            }
        }
        if (shouldNotify && notifySpec != null) {
            pendingInstallNotifier.notifyPending(
                packageName = notifySpec.packageName,
                repoOwner = notifySpec.repoOwner,
                repoName = notifySpec.repoName,
                appName = notifySpec.appName,
                versionTag = notifySpec.versionTag,
            )
        }
    }

    private data class NotifyData(
        val packageName: String,
        val repoOwner: String,
        val repoName: String,
        val appName: String,
        val versionTag: String,
    )

    override suspend fun cancel(packageName: String) {
        val job = stateMutex.withLock { activeJobs.remove(packageName) }
        job?.cancel()

        // Best-effort: delete the partial file via the downloader's
        // cancellation hook. The downloader's cancel path is keyed
        // on the scoped filename — we can recompute it from the
        // entry's owner/repo/asset name.
        val entry = _downloads.value[packageName]
        if (entry != null) {
            val scopedName =
                AssetFileName.scoped(entry.repoOwner, entry.repoName, entry.assetName)
            try {
                downloader.cancelDownload(scopedName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Logger.w(e) { "Orchestrator: cancelDownload failed for $scopedName" }
            }

            // If the entry was parked (AwaitingInstall), clean up the
            // persistent pending-install metadata and notification so
            // the apps row doesn't keep showing "ready to install".
            if (entry.stage == DownloadStage.AwaitingInstall) {
                try {
                    installedAppsRepository.setPendingInstallFilePath(packageName, null)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.w(e) { "Orchestrator: failed to clear pending path on cancel" }
                }
                try {
                    pendingInstallNotifier.clearPending(packageName)
                } catch (e: Exception) {
                    Logger.w(e) { "Orchestrator: failed to clear notification on cancel" }
                }
            }
        }

        stateMutex.withLock {
            _downloads.update { it - packageName }
        }
    }

    override suspend fun installPending(packageName: String): InstallOutcome? {
        val entry = _downloads.value[packageName]
            ?: run {
                // The orchestrator's in-memory entry may be gone (e.g.
                // process restart). Fall back to the persisted
                // pendingInstallFilePath on the InstalledApp row.
                val app = try {
                    installedAppsRepository.getAppByPackage(packageName)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.e(e) { "Orchestrator: getAppByPackage failed" }
                    null
                }
                val filePath = app?.pendingInstallFilePath ?: return null
                val ext = filePath.substringAfterLast('.', "").lowercase()
                return runStandaloneInstall(packageName, filePath, ext)
            }

        val filePath = entry.filePath ?: return null
        val ext = entry.assetName.substringAfterLast('.', "").lowercase()
        return runStandaloneInstall(packageName, filePath, ext)
    }

    private suspend fun runStandaloneInstall(
        packageName: String,
        filePath: String,
        ext: String,
    ): InstallOutcome? {
        updateEntry(packageName) { it.copy(stage = DownloadStage.Installing) }
        return try {
            installer.ensurePermissionsOrThrow(ext)
            val outcome = installer.install(filePath, ext)
            if (outcome == InstallOutcome.COMPLETED) {
                try {
                    installedAppsRepository.setPendingInstallFilePath(packageName, null)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.w(e) { "Orchestrator: failed to clear pending install path post-install" }
                }
                pendingInstallNotifier.clearPending(packageName)
                updateEntry(packageName) { it.copy(stage = DownloadStage.Completed) }
            }
            // DELEGATED_TO_SYSTEM: the system installer dialog is
            // showing. Don't clear pending metadata or mark Completed —
            // PackageEventReceiver handles the final state transition.
            outcome
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Logger.e(t) { "Orchestrator: standalone install failed for $packageName" }
            markFailed(packageName, t.message)
            null
        }
    }

    override fun dismiss(packageName: String) {
        // Synchronous dismiss — no Mutex needed for the read-modify-write
        // because StateFlow.update is itself atomic. We just don't
        // touch activeJobs (cancel does that).
        _downloads.update { it - packageName }
    }

    private suspend fun updateEntry(
        packageName: String,
        transform: (OrchestratedDownload) -> OrchestratedDownload,
    ) {
        stateMutex.withLock {
            _downloads.update { state ->
                val current = state[packageName] ?: return@update state
                state + (packageName to transform(current))
            }
        }
    }

    private suspend fun markFailed(packageName: String, message: String?) {
        stateMutex.withLock {
            _downloads.update { state ->
                val current = state[packageName] ?: return@update state
                state + (packageName to current.copy(stage = DownloadStage.Failed, errorMessage = message))
            }
        }
    }

    private fun generateId(): String =
        // Cheap unique id without pulling in java.util.UUID — works
        // on all KMP targets. Collisions are statistically negligible
        // for the lifetime of the orchestrator.
        Random.nextLong().toULong().toString(36) + Random.nextLong().toULong().toString(36)
}

/**
 * Orderable strength of an install policy. Higher = stronger
 * guarantee. Used by [DefaultDownloadOrchestrator.enqueue] when an
 * existing download is re-enqueued with a different policy: we only
 * upgrade (never downgrade) so the original caller's intent is
 * preserved unless an even more committed caller arrives.
 */
private val InstallPolicy.priority: Int
    get() = when (this) {
        InstallPolicy.DeferUntilUserAction -> 0
        InstallPolicy.InstallWhileForeground -> 1
        InstallPolicy.AlwaysInstall -> 2
    }
