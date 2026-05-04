package zed.rainxch.core.data.repository

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import zed.rainxch.core.data.dto.ReleaseNetwork
import zed.rainxch.core.data.local.db.AppDatabase
import zed.rainxch.core.data.local.db.dao.InstalledAppDao
import zed.rainxch.core.data.local.db.dao.UpdateHistoryDao
import zed.rainxch.core.data.local.db.entities.UpdateHistoryEntity
import zed.rainxch.core.data.mappers.toDomain
import zed.rainxch.core.data.mappers.toEntity
import zed.rainxch.core.data.network.GitHubClientProvider
import zed.rainxch.core.data.network.executeRequest
import zed.rainxch.core.domain.model.GithubAsset
import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.core.domain.model.InstallSource
import zed.rainxch.core.domain.model.InstalledApp
import zed.rainxch.core.domain.repository.InstalledAppsRepository
import zed.rainxch.core.domain.repository.MatchingPreview
import zed.rainxch.core.domain.system.Installer
import zed.rainxch.core.domain.model.isEffectivelyPreRelease
import zed.rainxch.core.domain.util.AssetFilter
import zed.rainxch.core.domain.util.AssetVariant
import zed.rainxch.core.domain.util.VersionMath

class InstalledAppsRepositoryImpl(
    private val database: AppDatabase,
    private val installedAppsDao: InstalledAppDao,
    private val historyDao: UpdateHistoryDao,
    private val installer: Installer,
    private val clientProvider: GitHubClientProvider,
    private val backendApiClient: zed.rainxch.core.data.network.BackendApiClient,
) : InstalledAppsRepository {
    // Reads the current Ktor client at every call site so any proxy
    // change (ProxyManager rebuilds the client via [clientProvider])
    // is picked up immediately on the next request without requiring
    // the repository itself to be reconstructed.
    private val httpClient: HttpClient get() = clientProvider.client

    private companion object {
        /**
         * How many releases the update checker fetches in one request.
         * Picked to balance:
         *  - Monorepos that ship multiple sibling apps in close succession
         *    (need a few releases of headroom to find a match for the
         *    targeted app via [InstalledApp.fallbackToOlderReleases])
         *  - Avoiding unnecessary GitHub API quota burn for the common case
         *    of a single-app repo where 1 release is enough.
         *
         * 50 is the GitHub API per_page maximum that doesn't require
         * pagination, and is enough to cover ~3 months of daily releases.
         */
        const val RELEASE_WINDOW = 50
    }

    override suspend fun <R> executeInTransaction(block: suspend () -> R): R =
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                block()
            }
        }

    override fun getAllInstalledApps(): Flow<List<InstalledApp>> =
        installedAppsDao
            .getAllInstalledApps()
            .map { it.map { app -> app.toDomain() } }

    override fun getAppsWithUpdates(): Flow<List<InstalledApp>> =
        installedAppsDao
            .getAppsWithUpdates()
            .map { it.map { app -> app.toDomain() } }

    override fun getUpdateCount(): Flow<Int> = installedAppsDao.getUpdateCount()

    override suspend fun getAppByPackage(packageName: String): InstalledApp? =
        installedAppsDao
            .getAppByPackage(packageName)
            ?.toDomain()

    override suspend fun getAppByRepoId(repoId: Long): InstalledApp? = installedAppsDao.getAppByRepoId(repoId)?.toDomain()

    override fun getAppByRepoIdAsFlow(repoId: Long): Flow<InstalledApp?> =
        installedAppsDao.getAppByRepoIdAsFlow(repoId).map { it?.toDomain() }

    override suspend fun getAppsByRepoId(repoId: Long): List<InstalledApp> =
        installedAppsDao.getAppsByRepoId(repoId).map { it.toDomain() }

    override fun getAppsByRepoIdAsFlow(repoId: Long): Flow<List<InstalledApp>> =
        installedAppsDao.getAppsByRepoIdAsFlow(repoId).map { list -> list.map { it.toDomain() } }

    override suspend fun isAppInstalled(repoId: Long): Boolean = installedAppsDao.getAppByRepoId(repoId) != null

    override suspend fun saveInstalledApp(app: InstalledApp) {
        installedAppsDao.insertApp(app.toEntity())
    }

    override suspend fun deleteInstalledApp(packageName: String) {
        installedAppsDao.deleteByPackageName(packageName)
    }

    /**
     * Fetches up to [RELEASE_WINDOW] releases for [owner]/[repo], filters
     * out drafts, applies the pre-release policy, and returns them sorted
     * by `publishedAt` descending. Empty list on failure (logged at error).
     *
     * Pre-release policy: a release is filtered out when
     * `includePreReleases = false` AND either the GitHub `prerelease`
     * flag is `true` OR the tag/name contains a recognised pre-release
     * marker (see [GithubRelease.isEffectivelyPreRelease]). The tag
     * heuristic catches the common maintainer mistake of tagging
     * `v2.0.0-rc.1` with `prerelease: false`. Whenever the flag and
     * heuristic disagree we emit a diagnostic so the drift is
     * traceable in session logs.
     */
    private suspend fun fetchReleaseWindow(
        owner: String,
        repo: String,
        includePreReleases: Boolean,
    ): List<GithubRelease> {
        val backendResult = backendApiClient.getReleases(owner, repo, perPage = RELEASE_WINDOW)
        val backendReleases = backendResult.fold(
            onSuccess = { it },
            onFailure = { error ->
                if (!zed.rainxch.core.data.network.shouldFallbackToGithubOrRethrow(error)) {
                    return emptyList()
                }
                null
            },
        )
        if (backendReleases != null) {
            return backendReleases
                .asSequence()
                .filter { it.draft != true }
                .sortedByDescending { it.publishedAt ?: it.createdAt ?: "" }
                .map { it.toDomain() }
                .filter { includePreReleases || !it.isEffectivelyPreRelease() }
                .toList()
        }

        return try {
            val releases =
                httpClient
                    .executeRequest<List<ReleaseNetwork>> {
                        get("/repos/$owner/$repo/releases") {
                            header(HttpHeaders.Accept, "application/vnd.github+json")
                            parameter("per_page", RELEASE_WINDOW)
                        }
                    }.getOrNull() ?: return emptyList()

            releases
                .asSequence()
                .filter { it.draft != true }
                .sortedByDescending { it.publishedAt ?: it.createdAt ?: "" }
                .map { it.toDomain() }
                .onEach { release ->
                    val flagSays = release.isPrerelease
                    val tagSays =
                        VersionMath.isPreReleaseTag(release.tagName) ||
                            VersionMath.isPreReleaseTag(release.name)
                    if (flagSays != tagSays) {
                        Logger.w {
                            "Pre-release flag/tag mismatch for $owner/$repo " +
                                "release '${release.tagName}' (name='${release.name}'): " +
                                "apiFlag=$flagSays, tagMarker=$tagSays — " +
                                "treating as pre-release=${flagSays || tagSays}"
                        }
                    }
                }
                .filter { includePreReleases || !it.isEffectivelyPreRelease() }
                .toList()
        } catch (e: CancellationException) {
            // Structured concurrency: cancellation must propagate. Never
            // silently convert a cancelled fetch into an empty result.
            throw e
        } catch (e: Exception) {
            Logger.e { "Failed to fetch releases for $owner/$repo: ${e.message}" }
            emptyList()
        }
    }

    /**
     * Result of [resolveTrackedRelease] — a candidate release plus the asset
     * the installer should download for it. `null` when no release in the
     * window contains a usable asset (after filter + arch matching).
     *
     * [variantWasLost] is true when the user has a [InstalledApp.preferredAssetVariant]
     * set but none of this release's assets matched it. The caller flips
     * `preferredVariantStale` based on this so the UI can prompt the user
     * to pick a new variant.
     */
    private data class ResolvedRelease(
        val release: GithubRelease,
        val primaryAsset: GithubAsset,
        val variantWasLost: Boolean,
    )

    /**
     * Walks [releases] (already in newest-first order) and returns the first
     * release whose installable asset list — after applying [filter] — yields
     * a usable asset. The picker tries, in order:
     *
     *   1. **Token-set match** — pinned token fingerprint equals the asset's
     *   2. **Glob match** — pinned glob pattern equals the asset's
     *   3. **Tail-string match** — legacy substring-tail equality
     *   4. **Same-position fallback** — same index, same total count of
     *      installable assets as when the user originally pinned
     *   5. **Platform auto-pick** — architecture-aware default
     *
     * Layers 1–3 are wrapped behind [AssetVariant.resolvePreferredAsset].
     * Layer 4 is consulted only when 1–3 all miss but the new release
     * has exactly the same number of installable assets as the picked
     * release. Layer 5 keeps updates flowing even when the variant is
     * completely lost — the caller flips `variantWasLost` so the UI
     * can surface the discrepancy.
     *
     * When [filter] is null, only the first release in the window is
     * considered: this preserves the pre-existing behaviour for apps that
     * don't track a monorepo.
     *
     * When [filter] is non-null and [fallbackToOlderReleases] is false, the
     * walker still only inspects the first release. The semantics are:
     *   "Apply the filter to the latest release, but don't dig further."
     * This matches Obtainium's defaults and avoids accidental downgrades for
     * apps where the user just wants a stricter asset picker.
     */
    private fun resolveTrackedRelease(
        releases: List<GithubRelease>,
        filter: AssetFilter?,
        fallbackToOlderReleases: Boolean,
        preferredVariant: String?,
        preferredTokens: Set<String>,
        preferredGlob: String?,
        pickedIndex: Int?,
        pickedSiblingCount: Int?,
    ): ResolvedRelease? {
        if (releases.isEmpty()) return null

        val candidates =
            if (filter != null && fallbackToOlderReleases) {
                releases
            } else {
                releases.take(1)
            }

        // "Has any pin" tracks whether the user has *something* stored
        // for variant identity — used to decide whether the
        // `variantWasLost` flag should flip on. Without this, an app
        // that's never been pinned would always look "lost".
        val hasAnyPin =
            preferredVariant != null ||
                preferredTokens.isNotEmpty() ||
                !preferredGlob.isNullOrBlank()

        for (release in candidates) {
            val installableForPlatform =
                release.assets.filter { installer.isAssetInstallable(it.name) }
            val installableForApp =
                if (filter == null) installableForPlatform
                else installableForPlatform.filter { filter.matches(it.name) }

            if (installableForApp.isEmpty()) continue

            // Layers 1–3: token set, glob, then legacy tail string.
            val fingerprintMatch =
                AssetVariant.resolvePreferredAsset(
                    assets = installableForApp,
                    pinnedVariant = preferredVariant,
                    pinnedTokens = preferredTokens.takeIf { it.isNotEmpty() },
                    pinnedGlob = preferredGlob,
                )

            // Layer 4: same-position fallback. Only consulted when no
            // fingerprint matched and the user actually pinned
            // *something* (otherwise the index is meaningless).
            val positionMatch =
                if (fingerprintMatch == null && hasAnyPin) {
                    AssetVariant.resolveBySamePosition(
                        assets = installableForApp,
                        originalIndex = pickedIndex,
                        siblingCountAtPickTime = pickedSiblingCount,
                    )
                } else {
                    null
                }

            // Layer 5: platform auto-pick (last resort, never null
            // unless the platform installer can't pick anything).
            val primary = fingerprintMatch
                ?: positionMatch
                ?: installer.choosePrimaryAsset(installableForApp)
                ?: continue

            // The variant is "lost" when the user had a pin but neither
            // a fingerprint nor a same-position match recovered it.
            // Same-position rescues silently (it's a confidence-trick
            // — the user can't tell anything went wrong) so we don't
            // flag it as lost; otherwise the UI would nag every check.
            val variantWasLost =
                hasAnyPin && fingerprintMatch == null && positionMatch == null

            return ResolvedRelease(release, primary, variantWasLost)
        }

        return null
    }

    override suspend fun checkForUpdates(packageName: String): Boolean {
        val app = installedAppsDao.getAppByPackage(packageName) ?: return false

        try {
            val releases =
                fetchReleaseWindow(
                    owner = app.repoOwner,
                    repo = app.repoName,
                    includePreReleases = app.includePreReleases,
                )

            if (releases.isEmpty()) {
                // The repo has no visible releases (or the fetch failed
                // softly). Drop any stale update metadata so the badge
                // doesn't outlive the release that set it.
                installedAppsDao.clearUpdateMetadata(packageName, System.currentTimeMillis())
                return false
            }

            // Compile the per-app filter once. Invalid regexes are treated as
            // "no filter" so we don't break the app silently — the user is
            // told about the syntax error in the advanced settings sheet.
            val compiledFilter =
                AssetFilter.parse(app.assetFilterRegex)
                    ?.onFailure { error ->
                        Logger.w {
                            "Invalid asset filter for $packageName " +
                                "(${app.assetFilterRegex}): ${error.message} — ignoring"
                        }
                    }?.getOrNull()

            val resolved =
                resolveTrackedRelease(
                    releases = releases,
                    filter = compiledFilter,
                    fallbackToOlderReleases = app.fallbackToOlderReleases,
                    preferredVariant = app.preferredAssetVariant,
                    preferredTokens = AssetVariant.deserializeTokens(app.preferredAssetTokens),
                    preferredGlob = app.assetGlobPattern,
                    pickedIndex = app.pickedAssetIndex,
                    pickedSiblingCount = app.pickedAssetSiblingCount,
                )

            if (resolved == null) {
                Logger.d {
                    "No matching release found for ${app.appName} in window of ${releases.size}; " +
                        "filter=${app.assetFilterRegex}, fallback=${app.fallbackToOlderReleases}"
                }
                // Filter matches nothing in the fetched window — clear
                // any cached latest-release metadata so the UI doesn't
                // keep pointing at an asset that no longer matches.
                installedAppsDao.clearUpdateMetadata(packageName, System.currentTimeMillis())
                return false
            }

            val (matchedRelease, primaryAsset, variantWasLost) = resolved

            val isUpdateAvailable =
                VersionMath.isVersionNewer(
                    candidate = matchedRelease.tagName,
                    current = app.installedVersion,
                )

            Logger.d {
                "Update check for ${app.appName}: " +
                    "installedTag=${app.installedVersion}, " +
                    "matchedTag=${matchedRelease.tagName}, " +
                    "matchedAsset=${primaryAsset.name}, " +
                    "isUpdate=$isUpdateAvailable, variantLost=$variantWasLost"
            }

            installedAppsDao.updateVersionInfo(
                packageName = packageName,
                available = isUpdateAvailable,
                version = matchedRelease.tagName,
                assetName = primaryAsset.name,
                assetUrl = primaryAsset.downloadUrl,
                assetSize = primaryAsset.size,
                releaseNotes = matchedRelease.description ?: "",
                timestamp = System.currentTimeMillis(),
                latestVersionName = matchedRelease.tagName,
                latestVersionCode = null,
                latestReleasePublishedAt = matchedRelease.publishedAt,
            )

            // Sync the staleness flag with what the resolver actually
            // observed: flip on when the user's pinned variant has
            // disappeared from the latest matching release, flip off
            // (and only when previously set) when it's back in business.
            if (variantWasLost != app.preferredVariantStale) {
                installedAppsDao.updateVariantStaleness(packageName, variantWasLost)
            }

            return isUpdateAvailable
        } catch (e: Exception) {
            Logger.e { "Failed to check updates for $packageName: ${e.message}" }
            installedAppsDao.updateLastChecked(packageName, System.currentTimeMillis())
        }

        return false
    }

    override suspend fun checkAllForUpdates() {
        val apps = installedAppsDao.getAllInstalledApps().first()
        apps.forEach { app ->
            if (app.updateCheckEnabled) {
                try {
                    checkForUpdates(app.packageName)
                } catch (e: Exception) {
                    Logger.w { "Failed to check updates for ${app.packageName}: ${e.message}" }
                }
            }
        }
    }

    override suspend fun updateAppVersion(
        packageName: String,
        newTag: String,
        newAssetName: String,
        newAssetUrl: String,
        newVersionName: String,
        newVersionCode: Long,
        signingFingerprint: String?,
        isPendingInstall: Boolean,
    ) {
        val app = installedAppsDao.getAppByPackage(packageName) ?: return

        Logger.d {
            "Updating app version: $packageName from ${app.installedVersion} to $newTag"
        }

        historyDao.insertHistory(
            UpdateHistoryEntity(
                packageName = packageName,
                appName = app.appName,
                repoOwner = app.repoOwner,
                repoName = app.repoName,
                fromVersion = app.installedVersion,
                toVersion = newTag,
                updatedAt = System.currentTimeMillis(),
                updateSource = InstallSource.THIS_APP,
                success = true,
            ),
        )

        installedAppsDao.updateApp(
            app.copy(
                installedVersion = newTag,
                installedAssetName = newAssetName,
                installedAssetUrl = newAssetUrl,
                installedVersionName = newVersionName,
                installedVersionCode = newVersionCode,
                latestVersion = newTag,
                latestAssetName = newAssetName,
                latestAssetUrl = newAssetUrl,
                latestVersionName = newVersionName,
                latestVersionCode = newVersionCode,
                isUpdateAvailable = false,
                isPendingInstall = isPendingInstall,
                lastUpdatedAt = System.currentTimeMillis(),
                lastCheckedAt = System.currentTimeMillis(),
                signingFingerprint = signingFingerprint,
                // When this update settled (isPendingInstall = false),
                // wipe any leftover parked-install pointer so the apps
                // screen stops advertising an Install CTA on a file
                // the system already swapped under the row. Caller
                // (DetailsViewModel.saveInstalledAppToDatabase) re-sets
                // these explicitly via setPendingInstallFilePath
                // afterwards on the still-pending path.
                pendingInstallFilePath =
                    if (isPendingInstall) app.pendingInstallFilePath else null,
                pendingInstallVersion =
                    if (isPendingInstall) app.pendingInstallVersion else null,
                pendingInstallAssetName =
                    if (isPendingInstall) app.pendingInstallAssetName else null,
            ),
        )
    }

    override suspend fun updateApp(app: InstalledApp) {
        installedAppsDao.updateApp(app.toEntity())
    }

    override suspend fun updateInstalledVersion(
        packageName: String,
        installedVersion: String,
        installedVersionName: String?,
        installedVersionCode: Long,
        isUpdateAvailable: Boolean,
    ) {
        installedAppsDao.updateInstalledVersion(
            packageName = packageName,
            installedVersion = installedVersion,
            installedVersionName = installedVersionName,
            installedVersionCode = installedVersionCode,
            isUpdateAvailable = isUpdateAvailable,
        )
    }

    override suspend fun updatePendingStatus(
        packageName: String,
        isPending: Boolean,
    ) {
        val app = installedAppsDao.getAppByPackage(packageName) ?: return
        installedAppsDao.updateApp(app.copy(isPendingInstall = isPending))
    }

    override suspend fun setIncludePreReleases(
        packageName: String,
        enabled: Boolean,
    ) {
        installedAppsDao.updateIncludePreReleases(packageName, enabled)
    }

    override suspend fun setAssetFilter(
        packageName: String,
        regex: String?,
        fallbackToOlderReleases: Boolean,
    ) {
        val normalized = regex?.trim()?.takeIf { it.isNotEmpty() }
        installedAppsDao.updateAssetFilter(
            packageName = packageName,
            regex = normalized,
            fallback = fallbackToOlderReleases,
        )

        // Persisting is the authoritative operation — if the follow-up
        // re-check fails (network down, rate limited, cancelled) we still
        // keep the new filter. The next periodic worker run will catch up.
        try {
            checkForUpdates(packageName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w {
                "Saved new asset filter for $packageName but immediate " +
                    "re-check failed: ${e.message}"
            }
        }
    }

    override suspend fun setPreferredVariant(
        packageName: String,
        variant: String?,
        tokens: String?,
        glob: String?,
        pickedIndex: Int?,
        siblingCount: Int?,
    ) {
        val normalizedVariant = variant?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedTokens = tokens?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedGlob = glob?.trim()?.takeIf { it.isNotEmpty() }
        installedAppsDao.updatePreferredVariant(
            packageName = packageName,
            variant = normalizedVariant,
            tokens = normalizedTokens,
            glob = normalizedGlob,
            pickedIndex = pickedIndex,
            siblingCount = siblingCount?.takeIf { it > 0 },
        )

        // Re-run the update check so cached `latestAsset*` columns point
        // at the variant the user just chose. Failures here are
        // non-fatal: persistence is the authoritative step.
        try {
            checkForUpdates(packageName)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logger.w {
                "Saved new variant for $packageName but immediate " +
                    "re-check failed: ${e.message}"
            }
        }
    }

    override suspend fun clearPreferredVariant(packageName: String) {
        setPreferredVariant(
            packageName = packageName,
            variant = null,
            tokens = null,
            glob = null,
            pickedIndex = null,
            siblingCount = null,
        )
    }

    override suspend fun setPendingInstallFilePath(
        packageName: String,
        path: String?,
        version: String?,
        assetName: String?,
    ) {
        installedAppsDao.updatePendingInstallFilePath(
            packageName = packageName,
            path = path,
            version = version,
            assetName = assetName,
        )
    }

    override suspend fun previewMatchingAssets(
        owner: String,
        repo: String,
        regex: String?,
        includePreReleases: Boolean,
        fallbackToOlderReleases: Boolean,
    ): MatchingPreview {
        val parseResult = AssetFilter.parse(regex)
        if (parseResult != null && parseResult.isFailure) {
            return MatchingPreview(
                release = null,
                matchedAssets = emptyList(),
                regexError = parseResult.exceptionOrNull()?.message,
            )
        }
        val filter = parseResult?.getOrNull()

        val releases = fetchReleaseWindow(owner, repo, includePreReleases)
        if (releases.isEmpty()) {
            return MatchingPreview(release = null, matchedAssets = emptyList())
        }

        val candidates =
            if (filter != null && fallbackToOlderReleases) {
                releases
            } else {
                releases.take(1)
            }

        for (release in candidates) {
            val installableForPlatform =
                release.assets.filter { installer.isAssetInstallable(it.name) }
            val matched =
                if (filter == null) installableForPlatform
                else installableForPlatform.filter { filter.matches(it.name) }
            if (matched.isNotEmpty()) {
                return MatchingPreview(release = release, matchedAssets = matched)
            }
        }

        return MatchingPreview(
            release = releases.firstOrNull(),
            matchedAssets = emptyList(),
        )
    }

    // Version normalization + comparison lives in
    // `core.domain.util.VersionMath` so the periodic update check,
    // the external-install verdict in `PackageEventReceiver`, and any
    // future surfaces all share one comparator. See #378.
}
