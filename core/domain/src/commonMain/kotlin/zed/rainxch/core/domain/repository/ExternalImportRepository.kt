package zed.rainxch.core.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.system.ExternalAppCandidate
import zed.rainxch.core.domain.system.ExternalDecisionSnapshot
import zed.rainxch.core.domain.system.RepoMatchResult
import zed.rainxch.core.domain.system.ScanResult

interface ExternalImportRepository {
    fun pendingCandidatesFlow(): Flow<List<ExternalAppCandidate>>

    fun pendingCandidateCountFlow(): Flow<Int>

    suspend fun scheduleInitialScanIfNeeded()

    suspend fun runFullScan(): ScanResult

    suspend fun runDeltaScan(changedPackageNames: Set<String>): ScanResult

    suspend fun resolveMatches(candidates: List<ExternalAppCandidate>): List<RepoMatchResult>

    suspend fun linkManually(
        packageName: String,
        owner: String,
        repo: String,
        source: String,
    ): Result<Unit>

    suspend fun skipPackage(
        packageName: String,
        neverAsk: Boolean = false,
    )

    suspend fun unlink(packageName: String)

    suspend fun snapshotDecision(packageName: String): ExternalDecisionSnapshot?

    suspend fun restoreDecision(snapshot: ExternalDecisionSnapshot)

    suspend fun rescanSinglePackage(packageName: String): RepoMatchResult?

    suspend fun searchRepos(query: String): Result<List<zed.rainxch.core.domain.system.RepoMatchSuggestion>>

    suspend fun syncSigningFingerprintSeed()

    suspend fun pruneExpiredSkips()

    suspend fun isPermissionGranted(): Boolean
}
