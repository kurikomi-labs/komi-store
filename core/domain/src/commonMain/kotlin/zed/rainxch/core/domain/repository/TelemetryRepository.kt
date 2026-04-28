package zed.rainxch.core.domain.repository

interface TelemetryRepository {
    fun recordSearchPerformed(query: String, resultCount: Int)

    fun recordSearchResultClicked(repoId: Long)

    fun recordRepoViewed(repoId: Long)

    fun recordReleaseDownloaded(repoId: Long)

    fun recordInstallStarted(repoId: Long)

    fun recordInstallSucceeded(repoId: Long)

    fun recordInstallFailed(repoId: Long, errorCode: String?)

    fun recordAppOpenedAfterInstall(repoId: Long)

    fun recordUninstalled(repoId: Long)

    fun recordFavorited(repoId: Long)

    fun recordUnfavorited(repoId: Long)

    // ── E1 external-import telemetry ────────────────────────────────
    // All payloads are bucketed/enum strings — never package names,
    // repo names, app labels, or fingerprints. See E1 plan §8.

    suspend fun importScanStarted(trigger: String)

    suspend fun importScanCompleted(candidateCountBucket: String, durationMsBucket: String)

    suspend fun importMatchAttempted(strategy: String, confidenceBucket: String)

    suspend fun importAutoLinked(countBucket: String)

    suspend fun importManuallyLinked(countBucket: String, source: String)

    suspend fun importSkipped(countBucket: String, persisted: String)

    suspend fun importUnlinkedFromDetails()

    suspend fun importPermissionRequested()

    suspend fun importPermissionOutcome(granted: Boolean, sdkIntBucket: String)

    suspend fun importSearchOverrideUsed()

    suspend fun importSearchOverrideNoResults()

    suspend fun signingSeedSyncCompleted(rowsAddedBucket: String, durationMsBucket: String)

    suspend fun externalMatchApiFailure(statusCodeBucket: String, retried: Boolean)

    suspend fun flushPending()

    /**
     * Drops any buffered events that have not yet been transmitted.
     * Called when the user resets their analytics ID so events that
     * were recorded under the old ID don't leak out attached to it.
     */
    suspend fun clearPending()
}
