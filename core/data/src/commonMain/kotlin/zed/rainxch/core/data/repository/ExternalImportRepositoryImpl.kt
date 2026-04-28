package zed.rainxch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import co.touchlab.kermit.Logger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import zed.rainxch.core.data.dto.ExternalMatchRequest
import zed.rainxch.core.data.local.db.dao.ExternalLinkDao
import zed.rainxch.core.data.local.db.dao.SigningFingerprintDao
import zed.rainxch.core.data.local.db.entities.ExternalLinkEntity
import zed.rainxch.core.data.local.db.entities.SigningFingerprintEntity
import zed.rainxch.core.data.mappers.toRepoMatchResults
import zed.rainxch.core.data.mappers.toRequestItem
import zed.rainxch.core.data.network.BackendApiClient
import zed.rainxch.core.data.network.BackendException
import zed.rainxch.core.data.network.ExternalMatchApi
import zed.rainxch.core.data.network.RateLimitedException
import zed.rainxch.core.domain.repository.ExternalImportRepository
import zed.rainxch.core.domain.repository.TelemetryRepository
import zed.rainxch.core.domain.system.ExternalAppCandidate
import zed.rainxch.core.domain.system.ExternalAppScanner
import zed.rainxch.core.domain.system.ExternalDecisionSnapshot
import zed.rainxch.core.domain.system.ExternalLinkState
import zed.rainxch.core.domain.system.RepoMatchResult
import zed.rainxch.core.domain.system.RepoMatchSource
import zed.rainxch.core.domain.system.RepoMatchSuggestion
import zed.rainxch.core.domain.system.ScanResult

class ExternalImportRepositoryImpl(
    private val scanner: ExternalAppScanner,
    private val externalLinkDao: ExternalLinkDao,
    private val signingFingerprintDao: SigningFingerprintDao,
    private val preferences: DataStore<Preferences>,
    private val externalMatchApi: ExternalMatchApi,
    private val backendClient: BackendApiClient,
    private val telemetry: TelemetryRepository,
) : ExternalImportRepository {
    // Snapshot cache survives only for the lifetime of the process. Decisions
    // (linked / skipped / never-ask) are persisted in `external_links`; the
    // raw candidate metadata (label, fingerprint, hint) is regenerated on the
    // next scan rather than persisted to keep the schema small.
    private val candidateSnapshot = MutableStateFlow<Map<String, ExternalAppCandidate>>(emptyMap())


    override fun pendingCandidatesFlow(): Flow<List<ExternalAppCandidate>> =
        combine(
            candidateSnapshot,
            externalLinkDao.observePendingReview(),
        ) { snapshot, pendingRows ->
            val pendingPackages = pendingRows.map { it.packageName }.toSet()
            pendingPackages.mapNotNull { snapshot[it] }
        }

    override fun pendingCandidateCountFlow(): Flow<Int> = externalLinkDao.observePendingReviewCount()

    override suspend fun scheduleInitialScanIfNeeded() {
        val firstLaunch = preferences.data.first()[INITIAL_SCAN_COMPLETED_AT_KEY] == null
        runCatching {
            if (firstLaunch) {
                runCatching { telemetry.importScanStarted(trigger = "first_launch") }
                    .onFailure { Logger.d { "telemetry importScanStarted failed: ${it.message}" } }
            }
            runFullScan()
        }.onSuccess {
            if (firstLaunch) markInitialScanComplete()
        }.onFailure {
            Logger.w(it) { "External scan failed; will retry on next launch." }
        }
    }

    override suspend fun runFullScan(): ScanResult {
        val started = nowMillis()
        val granted = scanner.isPermissionGranted()
        val rawCandidates = scanner.snapshot()
        val candidates = rawCandidates.filter { hasPositiveEvidence(it) }
        candidateSnapshot.update { candidates.associateBy { it.packageName } }

        val now = nowMillis()
        var newCandidates = 0
        var pendingReview = 0
        var preservedDecisions = 0

        candidates.forEach { candidate ->
            val existing = externalLinkDao.get(candidate.packageName)
            val updated = mergeCandidate(existing, candidate, now)
            if (existing == null) newCandidates++
            if (updated.state == ExternalLinkState.PENDING_REVIEW.name) pendingReview++
            if (existing != null && updated.state != ExternalLinkState.PENDING_REVIEW.name) preservedDecisions++
            externalLinkDao.upsert(updated)
        }

        val livePackages = candidates.map { it.packageName }.toSet()
        runCatching { externalLinkDao.prunePendingReviewNotIn(livePackages) }
            .onFailure { Logger.d { "prune pending failed: ${it.message}" } }

        val durationMs = nowMillis() - started
        runCatching {
            telemetry.importScanCompleted(
                candidateCountBucket = bucketCandidateCount(candidates.size),
                durationMsBucket = bucketDurationMs(durationMs),
            )
        }.onFailure { Logger.d { "telemetry importScanCompleted failed: ${it.message}" } }

        return ScanResult(
            totalCandidates = candidates.size,
            newCandidates = newCandidates,
            autoLinked = 0,
            pendingReview = pendingReview,
            durationMillis = durationMs,
            permissionGranted = granted,
        )
    }

    override suspend fun runDeltaScan(changedPackageNames: Set<String>): ScanResult {
        val started = nowMillis()
        val granted = scanner.isPermissionGranted()
        val now = nowMillis()
        var newCandidates = 0
        var pendingReview = 0
        val deltaCandidates = mutableListOf<ExternalAppCandidate>()

        changedPackageNames.forEach { pkg ->
            val rawCandidate = scanner.snapshotSingle(pkg)
            val existing = externalLinkDao.get(pkg)

            if (rawCandidate == null) {
                // Package is genuinely gone (uninstalled). Hard-delete the row.
                if (existing != null) {
                    externalLinkDao.deleteByPackageName(pkg)
                }
                return@forEach
            }

            if (!hasPositiveEvidence(rawCandidate)) {
                // Package exists but currently has no positive evidence. Only
                // drop PENDING_REVIEW rows here — preserved decisions
                // (MATCHED / NEVER_ASK / SKIPPED) must survive a transient
                // evidence miss (e.g., F-Droid seed not yet synced, manifest
                // hint changed across reinstall). Deleting them on a single
                // transient miss would silently wipe the user's decisions.
                if (existing?.state == ExternalLinkState.PENDING_REVIEW.name) {
                    externalLinkDao.deleteByPackageName(pkg)
                }
                return@forEach
            }

            val candidate = rawCandidate
            deltaCandidates += candidate
            val updated = mergeCandidate(existing, candidate, now)
            if (existing == null) newCandidates++
            if (updated.state == ExternalLinkState.PENDING_REVIEW.name) pendingReview++
            externalLinkDao.upsert(updated)
        }

        if (deltaCandidates.isNotEmpty()) {
            candidateSnapshot.update { current ->
                current.toMutableMap().apply {
                    deltaCandidates.forEach { put(it.packageName, it) }
                }
            }
        }

        return ScanResult(
            totalCandidates = deltaCandidates.size,
            newCandidates = newCandidates,
            autoLinked = 0,
            pendingReview = pendingReview,
            durationMillis = nowMillis() - started,
            permissionGranted = granted,
        )
    }

    override suspend fun resolveMatches(candidates: List<ExternalAppCandidate>): List<RepoMatchResult> {
        if (candidates.isEmpty()) return emptyList()

        // Strategy 3: signing-fingerprint lookup against the local seed
        // table. Hits are the strongest non-manifest signal we have —
        // signature equality is cryptographic, no string fuzzing.
        val fingerprintHits = mutableMapOf<String, RepoMatchSuggestion>()
        candidates.forEach { candidate ->
            val fp = candidate.signingFingerprint ?: return@forEach
            val hit = runCatching { signingFingerprintDao.lookup(fp) }
                .onFailure { Logger.d { "signing fingerprint lookup failed: ${it.message}" } }
                .getOrNull() ?: return@forEach
            fingerprintHits[candidate.packageName] = RepoMatchSuggestion(
                owner = hit.repoOwner,
                repo = hit.repoName,
                confidence = FINGERPRINT_CONFIDENCE,
                source = RepoMatchSource.FINGERPRINT,
            )
        }

        val backendResults = mutableMapOf<String, MutableList<RepoMatchSuggestion>>()
        for (batch in candidates.chunked(MATCH_BATCH_SIZE)) {
            val request =
                ExternalMatchRequest(
                    platform = "android",
                    candidates = batch.map { it.toRequestItem() },
                )
            externalMatchApi
                .match(request)
                .onSuccess { response ->
                    response.toRepoMatchResults().forEach { result ->
                        backendResults
                            .getOrPut(result.packageName) { mutableListOf() }
                            .addAll(result.suggestions)
                    }
                }.onFailure { error ->
                    Logger.w(error) { "external-match batch failed; continuing" }
                    runCatching {
                        telemetry.externalMatchApiFailure(
                            statusCodeBucket = bucketApiFailure(error),
                            retried = false,
                        )
                    }.onFailure { Logger.d { "telemetry externalMatchApiFailure failed: ${it.message}" } }
                }
        }

        return candidates.map { candidate ->
            val suggestions = mutableListOf<RepoMatchSuggestion>()
            candidate.manifestHint?.let { hint ->
                suggestions += RepoMatchSuggestion(
                    owner = hint.owner,
                    repo = hint.repo,
                    confidence = hint.confidence,
                    source = RepoMatchSource.MANIFEST,
                )
            }
            fingerprintHits[candidate.packageName]?.let { suggestions += it }
            backendResults[candidate.packageName]?.let { suggestions += it }
            val deduped = suggestions
                .distinctBy { "${it.owner}/${it.repo}" }
                .sortedByDescending { it.confidence }

            // Emit one `import_match_attempted` per strategy that
            // produced a hit for this candidate. Bucketed confidence
            // only — never owner/repo/package name.
            deduped.groupBy { it.source }.forEach { (source, hits) ->
                val top = hits.maxByOrNull { it.confidence } ?: return@forEach
                runCatching {
                    telemetry.importMatchAttempted(
                        strategy = source.telemetryStrategy(),
                        confidenceBucket = bucketConfidence(top.confidence),
                    )
                }.onFailure { Logger.d { "telemetry importMatchAttempted failed: ${it.message}" } }
            }

            RepoMatchResult(packageName = candidate.packageName, suggestions = deduped)
        }
    }

    override suspend fun linkManually(
        packageName: String,
        owner: String,
        repo: String,
        source: String,
    ): Result<Unit> {
        val now = nowMillis()
        return runCatching {
            val existing = externalLinkDao.get(packageName)
            val base = existing ?: ExternalLinkEntity(
                packageName = packageName,
                state = ExternalLinkState.MATCHED.name,
                repoOwner = owner,
                repoName = repo,
                matchSource = source,
                matchConfidence = 1.0,
                signingFingerprint = null,
                installerKind = null,
                firstSeenAt = now,
                lastReviewedAt = now,
                skipExpiresAt = null,
            )
            externalLinkDao.upsert(
                base.copy(
                    state = ExternalLinkState.MATCHED.name,
                    repoOwner = owner,
                    repoName = repo,
                    matchSource = source,
                    matchConfidence = 1.0,
                    lastReviewedAt = now,
                ),
            )
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun skipPackage(
        packageName: String,
        neverAsk: Boolean,
    ) {
        val existing = externalLinkDao.get(packageName)
        val state = if (neverAsk) ExternalLinkState.NEVER_ASK else ExternalLinkState.SKIPPED
        val now = nowMillis()
        val skipExpiresAt = if (neverAsk) null else now + SKIP_TTL_MILLIS
        val row =
            existing?.copy(
                state = state.name,
                lastReviewedAt = now,
                skipExpiresAt = skipExpiresAt,
            ) ?: ExternalLinkEntity(
                packageName = packageName,
                state = state.name,
                repoOwner = null,
                repoName = null,
                matchSource = null,
                matchConfidence = null,
                signingFingerprint = null,
                installerKind = null,
                firstSeenAt = now,
                lastReviewedAt = now,
                skipExpiresAt = skipExpiresAt,
            )
        externalLinkDao.upsert(row)
    }

    override suspend fun unlink(packageName: String) {
        externalLinkDao.deleteByPackageName(packageName)
        candidateSnapshot.update { it - packageName }
    }

    override suspend fun snapshotDecision(packageName: String): ExternalDecisionSnapshot? {
        val row = externalLinkDao.get(packageName) ?: return null
        return ExternalDecisionSnapshot(
            packageName = row.packageName,
            state = runCatching { ExternalLinkState.valueOf(row.state) }.getOrNull(),
            repoOwner = row.repoOwner,
            repoName = row.repoName,
            matchSource = row.matchSource,
            matchConfidence = row.matchConfidence,
            skipExpiresAt = row.skipExpiresAt,
        )
    }

    override suspend fun restoreDecision(snapshot: ExternalDecisionSnapshot) {
        val now = nowMillis()
        val state = snapshot.state ?: ExternalLinkState.PENDING_REVIEW
        val existing = externalLinkDao.get(snapshot.packageName)
        externalLinkDao.upsert(
            (existing ?: ExternalLinkEntity(
                packageName = snapshot.packageName,
                state = state.name,
                repoOwner = snapshot.repoOwner,
                repoName = snapshot.repoName,
                matchSource = snapshot.matchSource,
                matchConfidence = snapshot.matchConfidence,
                signingFingerprint = null,
                installerKind = null,
                firstSeenAt = now,
                lastReviewedAt = now,
                skipExpiresAt = snapshot.skipExpiresAt,
            )).copy(
                state = state.name,
                repoOwner = snapshot.repoOwner,
                repoName = snapshot.repoName,
                matchSource = snapshot.matchSource,
                matchConfidence = snapshot.matchConfidence,
                skipExpiresAt = snapshot.skipExpiresAt,
                lastReviewedAt = now,
            ),
        )
    }

    override suspend fun rescanSinglePackage(packageName: String): RepoMatchResult? {
        val candidate = scanner.snapshotSingle(packageName) ?: return null
        candidateSnapshot.update { it + (packageName to candidate) }
        return resolveMatches(listOf(candidate)).firstOrNull()
    }

    override suspend fun searchRepos(query: String): Result<List<RepoMatchSuggestion>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return Result.success(emptyList())
        val capped = if (trimmed.length > MAX_SEARCH_QUERY_LEN) {
            trimmed.substring(0, MAX_SEARCH_QUERY_LEN)
        } else {
            trimmed
        }
        return backendClient
            .search(query = capped, platform = "android", limit = SEARCH_LIMIT)
            .map { response ->
                response.items.map { item ->
                    RepoMatchSuggestion(
                        owner = item.owner.login,
                        repo = item.name,
                        // Search is the user-driven override path. The
                        // 0.5 confidence is a placeholder — UX is "I'll
                        // pick this myself", not a confidence bet.
                        confidence = SEARCH_OVERRIDE_CONFIDENCE,
                        source = RepoMatchSource.SEARCH,
                        stars = item.stargazersCount,
                        description = item.description,
                    )
                }
            }
    }

    override suspend fun syncSigningFingerprintSeed() {
        val started = nowMillis()
        var rowsAdded = 0
        try {
            val lastObservedAt = runCatching { signingFingerprintDao.lastSyncTimestamp() }
                .getOrNull()
            var cursor: String? = null
            var pages = 0
            paging@ while (pages < MAX_SEED_PAGES) {
                pages++
                val pageResult = backendClient.getSigningSeeds(
                    since = lastObservedAt,
                    cursor = cursor,
                )
                val response = pageResult.getOrElse { error ->
                    if (error is CancellationException) throw error
                    Logger.w(error) { "signing-seeds fetch failed on page $pages; aborting" }
                    break@paging
                }
                val rows = response.rows.map { row ->
                    SigningFingerprintEntity(
                        fingerprint = row.fingerprint,
                        repoOwner = row.owner,
                        repoName = row.repo,
                        source = SEED_SOURCE_BACKEND,
                        observedAt = row.observedAt,
                    )
                }
                if (rows.isNotEmpty()) {
                    runCatching { signingFingerprintDao.upsertAll(rows) }
                        .onSuccess { rowsAdded += rows.size }
                        .onFailure { e ->
                            if (e is CancellationException) throw e
                            Logger.w(e) { "signing-seeds upsert failed on page $pages; continuing" }
                        }
                }
                cursor = response.nextCursor ?: break@paging
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "signing-seeds sync aborted" }
        }
        emitSeedSyncTelemetry(rowsAdded, nowMillis() - started)
    }

    private suspend fun emitSeedSyncTelemetry(rowsAdded: Int, durationMs: Long) {
        runCatching {
            telemetry.signingSeedSyncCompleted(
                rowsAddedBucket = bucketSeedRowsAdded(rowsAdded),
                durationMsBucket = bucketDurationMs(durationMs),
            )
        }.onFailure { Logger.d { "telemetry signingSeedSyncCompleted failed: ${it.message}" } }
    }

    override suspend fun pruneExpiredSkips() {
        externalLinkDao.pruneExpiredSkips(nowMillis())
    }

    override suspend fun isPermissionGranted(): Boolean = scanner.isPermissionGranted()

    private suspend fun markInitialScanComplete() {
        preferences.edit { prefs ->
            prefs[INITIAL_SCAN_COMPLETED_AT_KEY] = nowMillis()
        }
    }

    private suspend fun hasPositiveEvidence(candidate: ExternalAppCandidate): Boolean {
        if (candidate.installerKind in TRUSTED_GITHUB_INSTALLERS) return true
        if (candidate.manifestHint != null) return true
        val fp = candidate.signingFingerprint ?: return false
        return runCatching { signingFingerprintDao.lookup(fp) != null }.getOrDefault(false)
    }

    private fun mergeCandidate(
        existing: ExternalLinkEntity?,
        candidate: ExternalAppCandidate,
        now: Long,
    ): ExternalLinkEntity {
        if (existing != null && shouldPreserveDecision(existing, now)) {
            return existing.copy(
                signingFingerprint = candidate.signingFingerprint ?: existing.signingFingerprint,
                // installerKind is authoritative per-scan from PackageManager; signingFingerprint may briefly be null on extraction failure, so we hold the previous value.
                installerKind = candidate.installerKind.name,
            )
        }

        val hint = candidate.manifestHint
        return ExternalLinkEntity(
            packageName = candidate.packageName,
            state = ExternalLinkState.PENDING_REVIEW.name,
            repoOwner = hint?.owner ?: existing?.repoOwner,
            repoName = hint?.repo ?: existing?.repoName,
            matchSource = if (hint != null) RepoMatchSource.MANIFEST.name else existing?.matchSource,
            matchConfidence = hint?.confidence ?: existing?.matchConfidence,
            signingFingerprint = candidate.signingFingerprint,
            installerKind = candidate.installerKind.name,
            firstSeenAt = existing?.firstSeenAt ?: now,
            lastReviewedAt = now,
            skipExpiresAt = null,
        )
    }

    private fun shouldPreserveDecision(
        existing: ExternalLinkEntity,
        now: Long,
    ): Boolean =
        when (existing.state) {
            ExternalLinkState.MATCHED.name -> true
            ExternalLinkState.NEVER_ASK.name -> true
            ExternalLinkState.SKIPPED.name -> (existing.skipExpiresAt ?: 0) > now
            else -> false
        }

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun bucketCount(n: Int): String =
        when {
            n <= 0 -> "0"
            n <= 2 -> "1-2"
            n <= 9 -> "3-9"
            n <= 49 -> "10-49"
            else -> "50+"
        }

    private fun bucketCandidateCount(n: Int): String =
        when {
            n <= 0 -> "0"
            n <= 9 -> "1-9"
            n <= 49 -> "10-49"
            n <= 199 -> "50-199"
            else -> "200+"
        }

    private fun bucketDurationMs(ms: Long): String =
        when {
            ms < 500L -> "<500"
            ms < 2_000L -> "500-2000"
            ms < 5_000L -> "2000-5000"
            else -> ">5000"
        }

    private fun bucketConfidence(c: Double): String =
        when {
            c < 0.5 -> "<0.5"
            c < 0.85 -> "0.5-0.85"
            else -> ">=0.85"
        }

    private fun bucketSeedRowsAdded(n: Int): String =
        when {
            n <= 0 -> "0"
            n <= 99 -> "1-99"
            n <= 999 -> "100-999"
            else -> "1000+"
        }

    private fun bucketApiFailure(error: Throwable): String =
        when (error) {
            is BackendException -> {
                val code = error.statusCode
                if (code in 400..499) "4xx" else "5xx"
            }
            is RateLimitedException -> "4xx"
            else -> "network"
        }

    private fun RepoMatchSource.telemetryStrategy(): String =
        when (this) {
            RepoMatchSource.MANIFEST -> "manifest"
            RepoMatchSource.SEARCH -> "search"
            RepoMatchSource.FINGERPRINT -> "fingerprint"
            RepoMatchSource.MANUAL -> "manual"
        }

    companion object {
        private val INITIAL_SCAN_COMPLETED_AT_KEY = longPreferencesKey("external_import_initial_scan_at")
        private const val SKIP_TTL_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
        private const val MATCH_BATCH_SIZE = 25
        private const val FINGERPRINT_CONFIDENCE = 0.92
        private const val SEARCH_OVERRIDE_CONFIDENCE = 0.5
        private const val SEARCH_LIMIT = 10
        private const val MAX_SEARCH_QUERY_LEN = 100
        private const val MAX_SEED_PAGES = 50
        private const val SEED_SOURCE_BACKEND = "backend_seed"

        // Stores whose entire catalog is sourced from GitHub releases — apps installed
        // through them are surfaced even without a manifest hint or fingerprint match.
        private val TRUSTED_GITHUB_INSTALLERS =
            setOf(
                zed.rainxch.core.domain.system.InstallerKind.STORE_OBTAINIUM,
                zed.rainxch.core.domain.system.InstallerKind.STORE_FDROID,
            )
    }
}
