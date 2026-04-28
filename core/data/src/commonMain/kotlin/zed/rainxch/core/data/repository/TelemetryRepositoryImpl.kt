package zed.rainxch.core.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import zed.rainxch.core.data.BuildKonfig
import zed.rainxch.core.data.dto.EventRequest
import zed.rainxch.core.data.network.BackendApiClient
import zed.rainxch.core.data.utils.hashQuery
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.domain.repository.DeviceIdentityRepository
import zed.rainxch.core.domain.repository.TelemetryRepository
import zed.rainxch.core.domain.repository.TweaksRepository

class TelemetryRepositoryImpl(
    private val backendApiClient: BackendApiClient,
    private val deviceIdentity: DeviceIdentityRepository,
    private val tweaksRepository: TweaksRepository,
    private val platform: Platform,
    private val appScope: CoroutineScope,
    private val logger: GitHubStoreLogger,
) : TelemetryRepository {

    private val bufferMutex = Mutex()
    private val buffer = ArrayDeque<EventRequest>()

    init {
        appScope.launch {
            while (true) {
                delay(FLUSH_INTERVAL_MS)
                runCatching { flushPending() }
                    .onFailure { logger.debug("Telemetry flush error: ${it.message}") }
            }
        }
    }

    // ── recording (fire-and-forget, guarded by opt-in) ──────────────

    override fun recordSearchPerformed(query: String, resultCount: Int) {
        enqueue(
            eventType = "search_performed",
            queryHash = hashQuery(query),
            resultCount = resultCount,
        )
    }

    override fun recordSearchResultClicked(repoId: Long) {
        enqueue(eventType = "search_result_clicked", repoId = repoId)
    }

    override fun recordRepoViewed(repoId: Long) {
        enqueue(eventType = "repo_viewed", repoId = repoId)
    }

    override fun recordReleaseDownloaded(repoId: Long) {
        enqueue(eventType = "release_downloaded", repoId = repoId)
    }

    override fun recordInstallStarted(repoId: Long) {
        enqueue(eventType = "install_started", repoId = repoId)
    }

    override fun recordInstallSucceeded(repoId: Long) {
        enqueue(eventType = "install_succeeded", repoId = repoId, success = true)
    }

    override fun recordInstallFailed(repoId: Long, errorCode: String?) {
        enqueue(
            eventType = "install_failed",
            repoId = repoId,
            success = false,
            errorCode = errorCode,
        )
    }

    override fun recordAppOpenedAfterInstall(repoId: Long) {
        enqueue(eventType = "app_opened_after_install", repoId = repoId)
    }

    override fun recordUninstalled(repoId: Long) {
        enqueue(eventType = "uninstalled", repoId = repoId)
    }

    override fun recordFavorited(repoId: Long) {
        enqueue(eventType = "favorited", repoId = repoId)
    }

    override fun recordUnfavorited(repoId: Long) {
        enqueue(eventType = "unfavorited", repoId = repoId)
    }

    // ── E1 external-import events ───────────────────────────────────
    // Privacy invariant: never pass package names, repo names, app
    // labels, or signing fingerprints — only bucketed strings, enums,
    // and counts. Enforced in CI by `PrivacyAuditTest` (E6).

    override suspend fun importScanStarted(trigger: String) {
        enqueueExt(eventType = "import_scan_started", trigger = trigger)
    }

    override suspend fun importScanCompleted(
        candidateCountBucket: String,
        durationMsBucket: String,
    ) {
        enqueueExt(
            eventType = "import_scan_completed",
            candidateCountBucket = candidateCountBucket,
            durationMsBucket = durationMsBucket,
        )
    }

    override suspend fun importMatchAttempted(strategy: String, confidenceBucket: String) {
        enqueueExt(
            eventType = "import_match_attempted",
            strategy = strategy,
            confidenceBucket = confidenceBucket,
        )
    }

    override suspend fun importAutoLinked(countBucket: String) {
        enqueueExt(eventType = "import_auto_linked", countBucket = countBucket)
    }

    override suspend fun importManuallyLinked(countBucket: String, source: String) {
        enqueueExt(
            eventType = "import_manually_linked",
            countBucket = countBucket,
            source = source,
        )
    }

    override suspend fun importSkipped(countBucket: String, persisted: String) {
        enqueueExt(
            eventType = "import_skipped",
            countBucket = countBucket,
            persisted = persisted,
        )
    }

    override suspend fun importUnlinkedFromDetails() {
        enqueueExt(eventType = "import_unlinked_from_details")
    }

    override suspend fun importPermissionRequested() {
        enqueueExt(eventType = "import_permission_requested")
    }

    override suspend fun importPermissionOutcome(granted: Boolean, sdkIntBucket: String) {
        enqueueExt(
            eventType = "import_permission_outcome",
            granted = granted,
            sdkIntBucket = sdkIntBucket,
        )
    }

    override suspend fun importSearchOverrideUsed() {
        enqueueExt(eventType = "import_search_override_used")
    }

    override suspend fun importSearchOverrideNoResults() {
        enqueueExt(eventType = "import_search_override_no_results")
    }

    override suspend fun signingSeedSyncCompleted(
        rowsAddedBucket: String,
        durationMsBucket: String,
    ) {
        enqueueExt(
            eventType = "signing_seed_sync_completed",
            rowsAddedBucket = rowsAddedBucket,
            durationMsBucket = durationMsBucket,
        )
    }

    override suspend fun externalMatchApiFailure(statusCodeBucket: String, retried: Boolean) {
        enqueueExt(
            eventType = "external_match_api_failure",
            statusCodeBucket = statusCodeBucket,
            retried = retried,
        )
    }

    // ── batching ────────────────────────────────────────────────────

    override suspend fun flushPending() {
        // Re-check consent: the user may have disabled telemetry between
        // when these events were enqueued and now. Respect the current
        // setting — withdrawn consent means the buffered events must
        // never leave the device.
        if (!telemetryEnabled()) {
            bufferMutex.withLock { buffer.clear() }
            return
        }

        val pending = bufferMutex.withLock {
            if (buffer.isEmpty()) return
            val take = minOf(buffer.size, MAX_BATCH_SIZE)
            val batch = (0 until take).map { buffer.removeFirst() }
            batch
        }

        val result = withContext(Dispatchers.IO) {
            backendApiClient.postEvents(pending)
        }

        if (result.isFailure) {
            // Put events back at the front for retry next tick (bounded).
            // If consent was revoked during the round-trip, drop them
            // instead — the flight was already in-progress under the old
            // consent, but re-adding would leak past the withdrawal.
            if (telemetryEnabled()) {
                bufferMutex.withLock {
                    for (i in pending.indices.reversed()) {
                        if (buffer.size < MAX_BUFFER_SIZE) buffer.addFirst(pending[i])
                    }
                }
            } else {
                bufferMutex.withLock { buffer.clear() }
            }
            logger.debug("Telemetry batch failed: ${result.exceptionOrNull()?.message}")
        }
    }

    override suspend fun clearPending() {
        bufferMutex.withLock { buffer.clear() }
    }

    private suspend fun telemetryEnabled(): Boolean =
        runCatching { tweaksRepository.getTelemetryEnabled().first() }
            .getOrDefault(false)

    // ── helpers ─────────────────────────────────────────────────────

    private fun enqueue(
        eventType: String,
        repoId: Long? = null,
        queryHash: String? = null,
        resultCount: Int? = null,
        success: Boolean? = null,
        errorCode: String? = null,
    ) {
        appScope.launch {
            if (!telemetryEnabled()) return@launch

            val deviceId = runCatching { deviceIdentity.getDeviceId() }.getOrNull() ?: return@launch

            val event = EventRequest(
                deviceId = deviceId,
                platform = platformSlug(),
                appVersion = BuildKonfig.VERSION_NAME,
                eventType = eventType,
                repoId = repoId,
                queryHash = queryHash,
                resultCount = resultCount,
                success = success,
                errorCode = errorCode,
            )

            bufferMutex.withLock {
                if (buffer.size >= MAX_BUFFER_SIZE) {
                    buffer.removeFirst()
                }
                buffer.add(event)
            }
        }
    }

    private fun enqueueExt(
        eventType: String,
        trigger: String? = null,
        strategy: String? = null,
        confidenceBucket: String? = null,
        countBucket: String? = null,
        candidateCountBucket: String? = null,
        durationMsBucket: String? = null,
        rowsAddedBucket: String? = null,
        statusCodeBucket: String? = null,
        sdkIntBucket: String? = null,
        source: String? = null,
        persisted: String? = null,
        granted: Boolean? = null,
        retried: Boolean? = null,
    ) {
        appScope.launch {
            if (!telemetryEnabled()) return@launch

            val deviceId = runCatching { deviceIdentity.getDeviceId() }.getOrNull() ?: return@launch

            val event = EventRequest(
                deviceId = deviceId,
                platform = platformSlug(),
                appVersion = BuildKonfig.VERSION_NAME,
                eventType = eventType,
                trigger = trigger,
                strategy = strategy,
                confidenceBucket = confidenceBucket,
                countBucket = countBucket,
                candidateCountBucket = candidateCountBucket,
                durationMsBucket = durationMsBucket,
                rowsAddedBucket = rowsAddedBucket,
                statusCodeBucket = statusCodeBucket,
                sdkIntBucket = sdkIntBucket,
                source = source,
                persisted = persisted,
                granted = granted,
                retried = retried,
            )

            bufferMutex.withLock {
                if (buffer.size >= MAX_BUFFER_SIZE) {
                    buffer.removeFirst()
                }
                buffer.add(event)
            }
        }
    }

    private fun platformSlug(): String = when (platform) {
        Platform.ANDROID -> "android"
        Platform.MACOS -> "desktop-macos"
        Platform.WINDOWS -> "desktop-windows"
        Platform.LINUX -> "desktop-linux"
    }

    private companion object {
        private const val FLUSH_INTERVAL_MS = 30_000L
        private const val MAX_BATCH_SIZE = 50
        private const val MAX_BUFFER_SIZE = 500
    }
}
