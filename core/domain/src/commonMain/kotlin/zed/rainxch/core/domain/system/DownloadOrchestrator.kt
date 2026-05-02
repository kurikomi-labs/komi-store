package zed.rainxch.core.domain.system

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import zed.rainxch.core.domain.model.GithubAsset

/**
 * Application-scoped manager for in-flight downloads and installs.
 *
 * # Why this exists
 *
 * Before this interface, every download/install ran inside the calling
 * ViewModel's `viewModelScope`. Two problems followed:
 *
 *   1. **Screen-leave kills installs** — navigating away from the
 *      details screen mid-download cancels the OkHttp call and deletes
 *      the partial file. Users had to camp on the screen to update
 *      anything.
 *
 *   2. **No cross-screen coordination** — the apps list and the details
 *      screen each had their own download bookkeeping. Hopping between
 *      screens lost progress, status, and cancel handles.
 *
 * The orchestrator solves both by owning a long-lived [CoroutineScope]
 * tied to the application's lifetime. ViewModels become **observers**
 * of [downloads] rather than **owners** of any work — when a screen is
 * destroyed, the work keeps running and the next screen instance picks
 * the state right back up.
 *
 * # Install policies
 *
 * Each enqueued download declares an [InstallPolicy] that controls what
 * happens when the bytes are on disk:
 *
 *   - [InstallPolicy.AlwaysInstall] — fire the installer no matter what.
 *     Used for **Shizuku** silent installs, where there's no UI dialog
 *     and the user has explicitly opted into the unattended path.
 *
 *   - [InstallPolicy.InstallWhileForeground] — fire the installer **only
 *     if the screen that requested the download is still alive**. The
 *     ViewModel must call [downgradeToDeferred] in `onCleared()`. If the
 *     downgrade lands before the download completes, the installer is
 *     not invoked; instead the file is parked, the row is marked
 *     "pending install", and the notifier is poked.
 *
 *   - [InstallPolicy.DeferUntilUserAction] — never auto-install. Used by
 *     the apps list when the user has tapped "Update" but the screen
 *     might leave at any moment, and by recovery flows. The file lands
 *     on disk and the user installs it explicitly.
 *
 * # Concurrency model
 *
 * Up to [maxConcurrent] downloads run in parallel; further `enqueue`
 * calls join an internal queue. Cancellation is per-package: cancelling
 * one download doesn't touch the others. Implementations should
 * surface a `SupervisorJob` so a single failed download can't poison
 * the scope.
 *
 * # Filename namespacing
 *
 * The orchestrator scopes filenames as `owner_repo_originalName.ext`
 * before passing them to the [zed.rainxch.core.domain.network.Downloader]
 * — this prevents collisions between two repos that ship installers
 * with the same name (e.g. `app.apk`).
 */
interface DownloadOrchestrator {
    /**
     * Live snapshot of every download the orchestrator currently knows
     * about, keyed by package name. Includes downloads in every stage
     * (queued, downloading, awaiting install, failed, cancelled). The
     * map is updated atomically so consumers can use plain `collect`
     * without worrying about torn reads.
     *
     * Entries are removed when:
     *  - The install completes (foreground or Shizuku) and
     *    [dismiss] is called by the consuming ViewModel
     *  - The user explicitly dismisses a failed/cancelled download
     *  - The orchestrator is cleared (process death)
     *
     * Pending-install entries (`stage = AwaitingInstall`) survive
     * across screen instances precisely so the apps list can show
     * "ready to install" rows.
     */
    val downloads: StateFlow<Map<String, OrchestratedDownload>>

    /**
     * Convenience flow that emits the entry for [packageName] whenever
     * it changes — equivalent to `downloads.map { it[packageName] }`
     * with `distinctUntilChanged`. Use this from a ViewModel observing
     * a single app.
     */
    fun observe(packageName: String): Flow<OrchestratedDownload?>

    /**
     * Enqueues a download for [spec]. If a download for the same
     * package is already in progress, this returns the existing
     * download's id without starting a new one — duplicates are a
     * silent no-op so consumers can be naïve about idempotency.
     *
     * @return the orchestrator's internal id for the download. Use it
     *   with [cancel] / [dismiss] for safe targeting (the package name
     *   also works but is less precise if a row gets re-enqueued).
     */
    suspend fun enqueue(spec: DownloadSpec): String

    /**
     * Atomically swaps the install policy for [packageName] to
     * [InstallPolicy.DeferUntilUserAction]. Called by foreground
     * ViewModels in `onCleared()` so a download that was started with
     * [InstallPolicy.InstallWhileForeground] doesn't auto-install
     * after the screen goes away.
     *
     * Race-safe: if the download has already finished and the install
     * is in progress, this is a no-op (the install completes). If the
     * download is still running, the policy flips immediately and the
     * orchestrator parks the file when bytes are done.
     *
     * No-op if no download exists for [packageName].
     */
    suspend fun downgradeToDeferred(packageName: String)

    /**
     * Cancels the download for [packageName] (if any), deletes the
     * partial file, and removes the entry from [downloads]. Safe to
     * call from any thread; idempotent.
     */
    suspend fun cancel(packageName: String)

    /**
     * Triggers the install of a previously-deferred download. Looks up
     * the file via [OrchestratedDownload.filePath] for [packageName],
     * runs the platform installer, and removes the entry on success.
     *
     * Used by:
     *  - The apps list "Install" button when a row is in
     *    [DownloadStage.AwaitingInstall]
     *  - The notification action that opens the apps list and resumes
     *
     * Validation (signing fingerprint, package mismatch) is the
     * caller's responsibility — the orchestrator runs the bare
     * `installer.install` call. The dialog-driven path in
     * `DetailsViewModel` continues to handle the screen-attached
     * case where the validation surface lives.
     */
    suspend fun installPending(packageName: String): InstallOutcome?

    /**
     * Removes the entry for [packageName] from [downloads]. Used when
     * the consuming ViewModel has handled a terminal state (success,
     * error, cancellation) and doesn't want it sticking around in the
     * map.
     */
    fun dismiss(packageName: String)
}

/**
 * What the caller wants the orchestrator to do with a single asset.
 */
data class DownloadSpec(
    /** Package name being installed/updated. Used as the map key. */
    val packageName: String,
    /** Repository owner — half of the filename namespace prefix. */
    val repoOwner: String,
    /** Repository name — the other half. */
    val repoName: String,
    /** Asset to download. The orchestrator pulls URL/name/size from this. */
    val asset: GithubAsset,
    /** Display name shown in notifications and progress UI. */
    val displayAppName: String,
    /** What to do with the file once it's on disk. See [InstallPolicy]. */
    val installPolicy: InstallPolicy,
    /**
     * Release tag of the version being downloaded. Stored on the
     * orchestrator entry for log / display purposes; the orchestrator
     * itself doesn't interpret it.
     */
    val releaseTag: String,
)

/**
 * Snapshot of one download's state in [DownloadOrchestrator.downloads].
 * Immutable — every state transition produces a new instance.
 */
data class OrchestratedDownload(
    val id: String,
    val packageName: String,
    val repoOwner: String,
    val repoName: String,
    val displayAppName: String,
    val assetName: String,
    val assetSize: Long,
    val downloadUrl: String,
    val releaseTag: String,
    /** Path on disk once the download has completed. Null until then. */
    val filePath: String?,
    /** Current install policy — can change mid-flight via downgrade. */
    val installPolicy: InstallPolicy,
    val stage: DownloadStage,
    /** 0..100, null when content-length is unknown. */
    val progressPercent: Int?,
    /**
     * Bytes received so far. Updated on every emission of the
     * underlying download flow — gives the UI a smooth byte counter
     * even when the percent integer hasn't ticked over.
     */
    val bytesDownloaded: Long = 0L,
    /**
     * Total bytes expected. Falls back to [assetSize] when the
     * server doesn't send `Content-Length`. Null only in rare cases
     * where neither is known up front.
     */
    val totalBytes: Long? = null,
    /** Error message if [stage] is [DownloadStage.Failed]. */
    val errorMessage: String? = null,
    /**
     * Outcome reported by the platform installer when the orchestrator
     * itself ran the install (the [InstallPolicy.AlwaysInstall] path).
     *
     * `null` for everything before the orchestrator finishes calling
     * `installer.install()` and for entries that never went through the
     * orchestrator's own install (i.e. [InstallPolicy.InstallWhileForeground]
     * and [InstallPolicy.DeferUntilUserAction] — those hand the file off
     * to the foreground ViewModel which captures the outcome itself).
     *
     * Consumers observing [DownloadStage.Completed] must use this to
     * decide whether to persist the row as actually-installed
     * (`COMPLETED`) or as pending-confirmation (`DELEGATED_TO_SYSTEM` —
     * e.g. Shizuku falling back to the system installer because the
     * binder went away). Treating every `Completed` as a real install
     * silently marks rows installed on cancelled prompts.
     */
    val installOutcome: InstallOutcome? = null,
)

/**
 * Lifecycle stages a download moves through. Forward-only except via
 * [DownloadOrchestrator.cancel] which can interrupt anything before
 * `Completed`.
 */
enum class DownloadStage {
    /** Sitting in the queue, waiting for a worker slot. */
    Queued,

    /** OkHttp is actively pulling bytes. */
    Downloading,

    /** Bytes are on disk; the orchestrator is about to invoke the installer. */
    Installing,

    /**
     * Bytes are on disk and the install policy said "wait for the user".
     * The apps list shows a one-tap install button for entries in this
     * stage. The notifier was poked when the entry first landed here.
     */
    AwaitingInstall,

    /** Installer reported success. The entry is eligible for [DownloadOrchestrator.dismiss]. */
    Completed,

    /** Cancelled by [DownloadOrchestrator.cancel]. */
    Cancelled,

    /** Download or install failed. See [OrchestratedDownload.errorMessage]. */
    Failed,
}

/**
 * What the orchestrator should do with the bytes once they're on disk.
 * See the interface kdoc for the rationale behind each policy.
 */
enum class InstallPolicy {
    /**
     * Always invoke the installer when the download finishes. Used
     * for Shizuku silent installs and other unattended paths.
     */
    AlwaysInstall,

    /**
     * Invoke the installer only if the screen that requested the
     * download is still attached. Foreground ViewModels declare this
     * and call [DownloadOrchestrator.downgradeToDeferred] in
     * `onCleared()`.
     */
    InstallWhileForeground,

    /**
     * Never auto-install. The file is parked in
     * [DownloadStage.AwaitingInstall] and the user installs from the
     * apps list explicitly.
     */
    DeferUntilUserAction,
}
