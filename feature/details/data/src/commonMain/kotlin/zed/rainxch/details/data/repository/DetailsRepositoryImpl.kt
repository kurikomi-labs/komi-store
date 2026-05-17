package zed.rainxch.details.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import zed.rainxch.core.data.cache.CacheManager
import zed.rainxch.core.data.cache.CacheManager.CacheTtl.README
import zed.rainxch.core.data.cache.CacheManager.CacheTtl.RELEASES
import zed.rainxch.core.data.cache.CacheManager.CacheTtl.REPO_DETAILS
import zed.rainxch.core.data.cache.CacheManager.CacheTtl.REPO_STATS
import zed.rainxch.core.data.cache.CacheManager.CacheTtl.USER_PROFILE
import zed.rainxch.core.data.dto.GithubReadmeResponseDto
import zed.rainxch.core.data.dto.ReleaseNetwork
import zed.rainxch.core.data.dto.RepoByIdNetwork
import zed.rainxch.core.data.dto.RepoInfoNetwork
import zed.rainxch.core.data.dto.UserProfileNetwork
import zed.rainxch.details.data.dto.AttestationsResponse
import zed.rainxch.core.data.dto.BackendRepoResponse
import zed.rainxch.core.data.mappers.toDomain
import zed.rainxch.core.data.mappers.toSummary
import zed.rainxch.core.data.network.BackendApiClient
import zed.rainxch.core.data.network.BackendException
import zed.rainxch.core.data.network.RateLimitedException
import zed.rainxch.core.data.network.RefreshBudgetExhaustedException
import zed.rainxch.core.data.network.RefreshCooldownException
import zed.rainxch.core.data.network.RepoArchivedException
import zed.rainxch.core.data.network.RepoNotFoundException
import zed.rainxch.core.data.network.shouldFallbackToGithubOrRethrow as sharedShouldFallback
import zed.rainxch.core.data.network.GitHubClientProvider
import zed.rainxch.core.data.network.executeRequest
import zed.rainxch.core.data.services.LocalizationManager
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.GithubRelease
import zed.rainxch.core.domain.model.GithubRepoSummary
import zed.rainxch.core.domain.model.GithubUser
import zed.rainxch.core.domain.model.GithubUserProfile
import zed.rainxch.core.domain.model.RefreshError
import zed.rainxch.core.domain.model.RefreshException
import zed.rainxch.details.data.utils.ReadmeLocalizationHelper
import zed.rainxch.details.data.utils.preprocessMarkdown
import zed.rainxch.details.domain.model.RepoStats
import zed.rainxch.details.domain.repository.DetailsRepository
import kotlin.coroutines.cancellation.CancellationException

class DetailsRepositoryImpl(
    private val clientProvider: GitHubClientProvider,
    private val backendApiClient: BackendApiClient,
    private val localizationManager: LocalizationManager,
    private val logger: GitHubStoreLogger,
    private val cacheManager: CacheManager,
) : DetailsRepository {
    private val httpClient: HttpClient get() = clientProvider.client

    @Serializable
    private data class CachedReadme(
        val content: String,
        val languageCode: String?,
        val path: String,
    )

    private val readmeHelper = ReadmeLocalizationHelper(localizationManager)

    /**
     * Decides whether a backend failure should trigger the direct-to-GitHub
     * fallback. **Side effect:** rethrows `CancellationException` to preserve
     * structured concurrency — callers don't need a separate CE check before
     * invoking this.
     *
     * Returns `true` for:
     *   - Any non-[BackendException] throwable (network errors, timeouts,
     *     parse failures — all treated as infra)
     *   - [BackendException] with status in 500..599
     *
     * Returns `false` for:
     *   - [BackendException] with status in 400..499 — backend's answer is
     *     authoritative (cached 404, 401 auth failure, 429 rate limit, etc.)
     *     and GitHub-direct would return the same answer. **Note:** this
     *     includes 429 and 408 — if the backend is rate-limiting us or
     *     timing out on its own pipeline, retrying via GitHub direct
     *     doesn't help and only burns more quota.
     */
    private fun shouldFallbackToGithubOrRethrow(cause: Throwable): Boolean =
        sharedShouldFallback(cause)

    private fun BackendRepoResponse.toBackendSummary(): GithubRepoSummary = toSummary()

    private fun RepoByIdNetwork.toGithubRepoSummary(): GithubRepoSummary =
        GithubRepoSummary(
            id = id,
            name = name,
            fullName = fullName,
            owner =
                GithubUser(
                    id = owner.id,
                    login = owner.login,
                    avatarUrl = owner.avatarUrl,
                    htmlUrl = owner.htmlUrl,
                ),
            description = description,
            htmlUrl = htmlUrl,
            stargazersCount = stars,
            forksCount = forks,
            language = language,
            topics = topics,
            releasesUrl = "https://api.github.com/repos/${owner.login}/$name/releases{/id}",
            updatedAt = updatedAt,
            defaultBranch = defaultBranch,
        )

    override suspend fun getRepositoryById(id: Long): GithubRepoSummary {
        val cacheKey = "details:repo_id:$id"

        cacheManager.get<GithubRepoSummary>(cacheKey)?.let { cached ->
            logger.debug("Cache hit for repo id=$id")
            return cached
        }

        return try {
            val result =
                httpClient
                    .executeRequest<RepoByIdNetwork> {
                        get("/repositories/$id") {
                            header(HttpHeaders.Accept, "application/vnd.github+json")
                        }
                    }.getOrThrow()
                    .toGithubRepoSummary()
            cacheManager.put(cacheKey, result, REPO_DETAILS)
            result
        } catch (e: Exception) {
            cacheManager.getStale<GithubRepoSummary>(cacheKey)?.let { stale ->
                logger.debug("Network error, using stale cache for repo id=$id")
                return stale
            }
            throw e
        }
    }

    override suspend fun getRepositoryByOwnerAndName(
        owner: String,
        name: String,
    ): GithubRepoSummary {
        val cacheKey = "details:repo:$owner/$name"

        cacheManager.get<GithubRepoSummary>(cacheKey)?.let { cached ->
            logger.debug("Cache hit for repo $owner/$name")
            return cached
        }

        // Try backend first. Phase 5.1: backend now lazy-caches unknown
        // repos, so success rate is high even for non-curated repos.
        val backendResult = backendApiClient.getRepo(owner, name)
        backendResult.fold(
            onSuccess = { backendRepo ->
                logger.debug("Backend hit for repo $owner/$name")
                val result = backendRepo.toBackendSummary()
                cacheManager.put(cacheKey, result, REPO_DETAILS)
                return result
            },
            onFailure = { e ->
                if (!shouldFallbackToGithubOrRethrow(e)) {
                    // Backend 4xx — GitHub would give the same answer.
                    // Serve stale if we have it, otherwise propagate the
                    // error so the VM can show the right state.
                    cacheManager.getStale<GithubRepoSummary>(cacheKey)?.let { stale ->
                        logger.debug("Backend 4xx for $owner/$name, serving stale cache")
                        return stale
                    }
                    throw e
                }
                logger.debug("Backend infra error for $owner/$name (${e.message}), falling back to GitHub")
            },
        )

        // Fallback to GitHub API (only reached on backend 5xx / network error)
        return try {
            val result =
                httpClient
                    .executeRequest<RepoByIdNetwork> {
                        get("/repos/$owner/$name") {
                            header(HttpHeaders.Accept, "application/vnd.github+json")
                        }
                    }.getOrThrow()
                    .toGithubRepoSummary()

            cacheManager.put(cacheKey, result, REPO_DETAILS)
            result
        } catch (e: Exception) {
            cacheManager.getStale<GithubRepoSummary>(cacheKey)?.let { stale ->
                logger.debug("Network error, using stale cache for $owner/$name")
                return stale
            }
            throw e
        }
    }

    override suspend fun refreshRepository(
        owner: String,
        name: String,
    ): GithubRepoSummary {
        val outcome = backendApiClient.refreshRepo(owner, name)
        outcome.exceptionOrNull()?.let { throw it.toRefreshException() }
        val backendRepo = outcome.getOrThrow()
        val result = backendRepo.toBackendSummary()
        val cacheKey = "details:repo:$owner/$name"
        cacheManager.put(cacheKey, result, REPO_DETAILS)
        cacheManager.invalidate("details:repo_id:${result.id}")
        cacheManager.invalidate("details:stats:v3:$owner/$name")
        cacheManager.invalidate("details:latest_release:$owner/$name")
        cacheManager.invalidate("details:releases:$owner/$name")
        return result
    }

    private fun Throwable.toRefreshException(): Throwable =
        when (this) {
            is CancellationException -> this
            is RefreshCooldownException ->
                RefreshException(RefreshError.COOLDOWN, retryAfterSeconds)
            is RefreshBudgetExhaustedException ->
                RefreshException(RefreshError.BUDGET_EXHAUSTED, retryAfterSeconds)
            is RateLimitedException ->
                RefreshException(RefreshError.COOLDOWN, retryAfterSeconds)
            is RepoArchivedException ->
                RefreshException(RefreshError.ARCHIVED)
            is RepoNotFoundException ->
                RefreshException(RefreshError.NOT_FOUND)
            is BackendException -> RefreshException(
                if (statusCode in 500..599) RefreshError.UPSTREAM else RefreshError.GENERIC,
            )
            else -> RefreshException(RefreshError.GENERIC)
        }

    override suspend fun getLatestPublishedRelease(
        owner: String,
        repo: String,
        defaultBranch: String,
    ): GithubRelease? {
        val cacheKey = "details:latest_release:$owner/$repo"

        cacheManager.get<GithubRelease>(cacheKey)?.let { cached ->
            logger.debug("Cache hit for latest release $owner/$repo")
            return cached
        }

        return try {
            val releases =
                httpClient
                    .executeRequest<List<ReleaseNetwork>> {
                        get("/repos/$owner/$repo/releases") {
                            header(HttpHeaders.Accept, "application/vnd.github+json")
                            parameter("per_page", 10)
                        }
                    }.getOrNull() ?: return null

            val latest =
                releases
                    .asSequence()
                    .filter { (it.draft != true) && (it.prerelease != true) }
                    .maxByOrNull { it.publishedAt ?: it.createdAt ?: "" }
                    ?: return null

            val result =
                latest
                    .copy(
                        body = processReleaseBody(latest.body, owner, repo, defaultBranch),
                    ).toDomain()

            cacheManager.put(cacheKey, result, RELEASES)
            result
        } catch (e: Exception) {
            cacheManager.getStale<GithubRelease>(cacheKey)?.let { stale ->
                logger.debug("Network error, using stale cache for latest release $owner/$repo")
                return stale
            }
            throw e
        }
    }

    override suspend fun getAllReleases(
        owner: String,
        repo: String,
        defaultBranch: String,
    ): List<GithubRelease> {
        val cacheKey = "details:releases:$owner/$repo"

        cacheManager.get<List<GithubRelease>>(cacheKey)?.let { cached ->
            if (cached.isNotEmpty()) {
                logger.debug("Cache hit for all releases $owner/$repo: ${cached.size} releases")
                return cached
            }
        }

        // Backend-first. Phase 5.1 routes /v1/releases via the backend cache
        // + ETag revalidation, China-reachable via Gcore/api-direct.
        val backendResult = backendApiClient.getReleases(owner, repo)
        backendResult.fold(
            onSuccess = { releases ->
                val result = releases
                    .filter { it.draft != true }
                    .map { release ->
                        release.copy(
                            body = processReleaseBody(release.body, owner, repo, defaultBranch),
                        ).toDomain()
                    }.sortedByDescending { it.publishedAt }
                if (result.isNotEmpty()) {
                    cacheManager.put(cacheKey, result, RELEASES)
                }
                return result
            },
            onFailure = { e ->
                if (!shouldFallbackToGithubOrRethrow(e)) {
                    cacheManager.getStale<List<GithubRelease>>(cacheKey)?.let { stale ->
                        logger.debug("Backend 4xx for releases $owner/$repo, serving stale cache")
                        return stale
                    }
                    throw e
                }
                logger.debug("Backend infra error for releases $owner/$repo (${e.message}), falling back to GitHub")
            },
        )

        // Fallback to GitHub API directly (only reached on backend 5xx / network error)
        return try {
            val releases =
                httpClient
                    .executeRequest<List<ReleaseNetwork>> {
                        get("/repos/$owner/$repo/releases") {
                            header(HttpHeaders.Accept, "application/vnd.github+json")
                            parameter("per_page", 30)
                        }
                    }.getOrNull() ?: return emptyList()

            val result =
                releases
                    .filter { it.draft != true }
                    .map { release ->
                        release
                            .copy(
                                body = processReleaseBody(release.body, owner, repo, defaultBranch),
                            ).toDomain()
                    }.sortedByDescending { it.publishedAt }

            if (result.isNotEmpty()) {
                cacheManager.put(cacheKey, result, RELEASES)
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: SerializationException) {
            // Parse failure signals a DTO/API drift — surface loudly so it's
            // findable in logs and crash reports. Still prefer returning a
            // stale cache rather than throwing, so the UI can keep rendering
            // the last known good data while we figure out the new shape.
            logger.error("Failed to parse releases for $owner/$repo: ${e.message}", e)
            cacheManager.getStale<List<GithubRelease>>(cacheKey)?.let { stale ->
                logger.debug("Serving stale cache for releases $owner/$repo after parse failure")
                return stale
            }
            throw e
        } catch (e: Exception) {
            cacheManager.getStale<List<GithubRelease>>(cacheKey)?.let { stale ->
                logger.debug("Network error, using stale cache for releases $owner/$repo")
                return stale
            }
            throw e
        }
    }

    private fun processReleaseBody(
        body: String?,
        owner: String,
        repo: String,
        defaultBranch: String,
    ): String? =
        body
            ?.replace("\r\n", "\n")
            ?.let { rawMarkdown ->
                preprocessMarkdown(
                    markdown = rawMarkdown,
                    baseUrl = "https://raw.githubusercontent.com/$owner/$repo/$defaultBranch/",
                )
            }

    override suspend fun getReadme(
        owner: String,
        repo: String,
        defaultBranch: String,
    ): Triple<String, String?, String>? {
        // v2 — bumped after markdown preprocessor overhaul (alerts,
        // emoji, details, image-row). Forces re-fetch so users get a
        // properly-processed readme instead of waiting for the stale
        // v1 entry to expire.
        val cacheKey = "details:readme:v4:$owner/$repo"

        cacheManager.get<CachedReadme>(cacheKey)?.let { cached ->
            logger.debug("Cache hit for readme $owner/$repo")
            return Triple(cached.content, cached.languageCode, cached.path)
        }

        // Backend-first. Phase 5.2: /v1/readme proxies GitHub's contents API,
        // which returns base64-encoded markdown — different shape from the
        // raw.githubusercontent.com path below, but the post-processing
        // pipeline is the same.
        val backendResult = backendApiClient.getReadme(owner, repo)
        backendResult.fold(
            onSuccess = { dto ->
                val processed = processReadmeFromBackend(dto, owner, repo, defaultBranch)
                if (processed != null) {
                    cacheManager.put(
                        cacheKey,
                        CachedReadme(
                            content = processed.first,
                            languageCode = processed.second,
                            path = processed.third,
                        ),
                        README,
                    )
                    return processed
                }
                // Decode/processing failed — fall through to the raw-URL path
                logger.debug("Backend readme decode failed for $owner/$repo, falling back to raw URL")
            },
            onFailure = { e ->
                if (!shouldFallbackToGithubOrRethrow(e)) {
                    cacheManager.getStale<CachedReadme>(cacheKey)?.let { stale ->
                        logger.debug("Backend 4xx for readme $owner/$repo, serving stale cache")
                        return Triple(stale.content, stale.languageCode, stale.path)
                    }
                    // No stale — no readme exists or user can't access. Treat
                    // as "no readme" rather than propagating as an error;
                    // matches how fetchReadmeFromApi returned null.
                    return null
                }
                logger.debug("Backend infra error for readme $owner/$repo (${e.message}), falling back to raw URL")
            },
        )

        // Fallback to raw.githubusercontent.com (only reached on backend
        // infra error or on successful backend response that we couldn't decode)
        val result = fetchReadmeFromApi(owner, repo, defaultBranch)

        if (result != null) {
            val cachedReadme =
                CachedReadme(
                    content = result.first,
                    languageCode = result.second,
                    path = result.third,
                )
            cacheManager.put(cacheKey, cachedReadme, README)
        }

        return result
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun processReadmeFromBackend(
        dto: GithubReadmeResponseDto,
        owner: String,
        repo: String,
        defaultBranch: String,
    ): Triple<String, String?, String>? {
        // GitHub's contents API base64-encodes with embedded newlines; Mime
        // variant tolerates all whitespace transparently so we don't have
        // to pre-strip. Narrow catch: only IAE is decode-related, other
        // throwables (OOM, etc.) propagate.
        val decoded = try {
            Base64.Mime.decode(dto.content).decodeToString()
        } catch (e: IllegalArgumentException) {
            logger.warn("Failed to base64-decode backend readme for $owner/$repo: ${e.message}")
            return null
        }
        val path = dto.path?.takeIf { it.isNotBlank() } ?: "README.md"
        val baseUrl = "https://raw.githubusercontent.com/$owner/$repo/$defaultBranch/"
        val processed = preprocessMarkdown(markdown = decoded, baseUrl = baseUrl)
        val detectedLang = readmeHelper.detectReadmeLanguage(processed)
        logger.debug("Fetched README via backend (detected language: ${detectedLang ?: "unknown"})")
        return Triple(processed, detectedLang, path)
    }

    private suspend fun fetchReadmeFromApi(
        owner: String,
        repo: String,
        defaultBranch: String,
    ): Triple<String, String?, String>? {
        val baseUrl = "https://raw.githubusercontent.com/$owner/$repo/$defaultBranch/"
        val path = "README.md"

        return try {
            val rawMarkdown =
                httpClient
                    .executeRequest<String> {
                        get("$baseUrl$path")
                    }.getOrNull()

            if (rawMarkdown != null) {
                val processed = preprocessMarkdown(markdown = rawMarkdown, baseUrl = baseUrl)
                val detectedLang = readmeHelper.detectReadmeLanguage(processed)
                logger.debug("Fetched README.md (detected language: ${detectedLang ?: "unknown"})")
                Triple(processed, detectedLang, path)
            } else {
                logger.error("Failed to fetch README.md for $owner/$repo")
                null
            }
        } catch (e: Throwable) {
            logger.error("Failed to fetch README.md: ${e.message}")
            null
        }
    }

    override suspend fun getRepoStats(
        owner: String,
        repo: String,
    ): RepoStats {
        // v3 — backend now supplies license. Bumping the key forces re-fetch
        // so post-upgrade users get a populated license instead of waiting
        // 6h for the stale v2 entry (license=null) to expire.
        val cacheKey = "details:stats:v3:$owner/$repo"

        cacheManager.get<RepoStats>(cacheKey)?.let { cached ->
            logger.debug("Cache hit for repo stats $owner/$repo")
            return cached
        }

        // Try backend first — provides stars/forks/openIssues/license/downloadCount.
        // No more direct GitHub enrichment for license (was 1 quota hit per
        // signed-in user per stats fetch); backend is now authoritative.
        val backendResult = backendApiClient.getRepo(owner, repo)
        backendResult.fold(
            onSuccess = { backendRepo ->
                logger.debug("Backend hit for repo stats $owner/$repo")
                val result = RepoStats(
                    stars = backendRepo.stargazersCount,
                    forks = backendRepo.forksCount,
                    openIssues = backendRepo.openIssuesCount,
                    license = backendRepo.license?.spdxId ?: backendRepo.license?.name,
                    totalDownloads = backendRepo.downloadCount,
                )
                cacheManager.put(cacheKey, result, REPO_STATS)
                return result
            },
            onFailure = { e ->
                if (!shouldFallbackToGithubOrRethrow(e)) {
                    cacheManager.getStale<RepoStats>(cacheKey)?.let { stale ->
                        logger.debug("Backend 4xx for stats $owner/$repo, serving stale cache")
                        return stale
                    }
                    throw e
                }
                logger.debug("Backend infra error for stats $owner/$repo (${e.message}), falling back to GitHub")
            },
        )

        // Fallback to GitHub API
        return try {
            logger.debug("Backend miss for stats $owner/$repo, falling back to GitHub API")
            val info =
                httpClient
                    .executeRequest<RepoInfoNetwork> {
                        get("/repos/$owner/$repo") {
                            header(HttpHeaders.Accept, "application/vnd.github+json")
                        }
                    }.getOrThrow()

            val result =
                RepoStats(
                    stars = info.stars,
                    forks = info.forks,
                    openIssues = info.openIssues,
                    license = info.license?.spdxId ?: info.license?.name,
                    totalDownloads = 0,
                )

            cacheManager.put(cacheKey, result, REPO_STATS)
            result
        } catch (e: Exception) {
            cacheManager.getStale<RepoStats>(cacheKey)?.let { stale ->
                logger.debug("Network error, using stale cache for stats $owner/$repo")
                return stale
            }
            throw e
        }
    }

    override suspend fun getUserProfile(username: String): GithubUserProfile {
        val cacheKey = "details:profile:$username"

        cacheManager.get<GithubUserProfile>(cacheKey)?.let { cached ->
            logger.debug("Cache hit for user profile $username")
            return cached
        }

        // Backend-first. Phase 5.3: /v1/user proxies GitHub's users API with
        // aggressive edge caching (7-day TTL on Gcore).
        val backendResult = backendApiClient.getUser(username)
        backendResult.fold(
            onSuccess = { user ->
                val result = user.toDomainProfile()
                cacheManager.put(cacheKey, result, USER_PROFILE)
                return result
            },
            onFailure = { e ->
                if (!shouldFallbackToGithubOrRethrow(e)) {
                    cacheManager.getStale<GithubUserProfile>(cacheKey)?.let { stale ->
                        logger.debug("Backend 4xx for profile $username, serving stale cache")
                        return stale
                    }
                    throw e
                }
                logger.debug("Backend infra error for profile $username (${e.message}), falling back to GitHub")
            },
        )

        // Fallback to GitHub direct (only reached on backend 5xx / network error)
        return try {
            val user =
                httpClient
                    .executeRequest<UserProfileNetwork> {
                        get("/users/$username") {
                            header(HttpHeaders.Accept, "application/vnd.github+json")
                        }
                    }.getOrThrow()

            val result = user.toDomainProfile()
            cacheManager.put(cacheKey, result, USER_PROFILE)
            result
        } catch (e: Exception) {
            cacheManager.getStale<GithubUserProfile>(cacheKey)?.let { stale ->
                logger.debug("Network error, using stale cache for profile $username")
                return stale
            }
            throw e
        }
    }

    private fun UserProfileNetwork.toDomainProfile(): GithubUserProfile =
        GithubUserProfile(
            id = id,
            login = login,
            name = name,
            bio = bio,
            avatarUrl = avatarUrl,
            htmlUrl = htmlUrl,
            followers = followers,
            following = following,
            publicRepos = publicRepos,
            location = location,
            company = company,
            blog = blog,
            twitterUsername = twitterUsername,
        )

    override suspend fun checkAttestations(
        owner: String,
        repo: String,
        sha256Digest: String,
    ): Boolean =
        try {
            val response =
                httpClient
                    .executeRequest<AttestationsResponse> {
                        get("/repos/$owner/$repo/attestations/sha256:$sha256Digest") {
                            header(HttpHeaders.Accept, "application/vnd.github+json")
                        }
                    }.getOrNull()
            response != null && response.attestations.isNotEmpty()
        } catch (e: Exception) {
            logger.debug("Attestation check failed for $owner/$repo: ${e.message}")
            false
        }

}
