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
    private val forgejoClientRegistry: zed.rainxch.core.data.network.ForgejoClientRegistry,
) : DetailsRepository {
    private val httpClient: HttpClient get() = clientProvider.client

    private fun zed.rainxch.core.data.dto.ForgejoRepoNetworkModel.toForgejoSummary(
        sourceHost: String,
    ): GithubRepoSummary = GithubRepoSummary(
        id = zed.rainxch.core.domain.util.RepoIdCodec.encode(sourceHost, id),
        name = name,
        fullName = fullName ?: "${owner.login}/$name",
        owner = GithubUser(
            id = owner.id,
            login = owner.login,
            avatarUrl = owner.avatarUrl,
            htmlUrl = owner.htmlUrl,
        ),
        description = description,
        htmlUrl = htmlUrl,
        stargazersCount = starsCount,
        forksCount = forksCount,
        language = language,
        topics = null,
        releasesUrl = "$htmlUrl/releases",
        updatedAt = updatedAt ?: "",
        defaultBranch = defaultBranch ?: "main",
        sourceHost = sourceHost,
    )

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
        sourceHost: String?,
    ): GithubRepoSummary {
        if (sourceHost != null) return getForgejoRepository(owner, name, sourceHost)
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
        sourceHost: String?,
    ): GithubRelease? {
        if (sourceHost != null) {
            return getForgejoAllReleases(owner, repo, sourceHost)
                .firstOrNull { !it.isPrerelease }
        }
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
        sourceHost: String?,
    ): List<GithubRelease> {
        if (sourceHost != null) return getForgejoAllReleases(owner, repo, sourceHost)
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
        sourceHost: String?,
    ): Triple<String, String?, String>? {
        if (sourceHost != null) return getForgejoReadme(owner, repo, defaultBranch, sourceHost)
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
        val rawContent = dto.content ?: return null
        val decoded = try {
            Base64.Mime.decode(rawContent).decodeToString()
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
        sourceHost: String?,
    ): RepoStats {
        if (sourceHost != null) return getForgejoRepoStats(owner, repo, sourceHost)
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

    // ── Forgejo / Codeberg branch ─────────────────────────────────────
    //
    // No backend proxy, no GitHub fallback — all reads go straight to the
    // forge instance. Cache keys are namespaced by host so the same
    // `owner/repo` slug on github.com and codeberg.org never collide. Stats
    // are derived from the repo response (stars / forks / openIssues /
    // license fields exposed by Gitea/Forgejo API v1).

    private suspend fun getForgejoRepository(
        owner: String,
        name: String,
        sourceHost: String,
    ): GithubRepoSummary {
        val cacheKey = "details:repo:forgejo:$sourceHost:$owner/$name"
        cacheManager.get<GithubRepoSummary>(cacheKey)?.let { return it }
        val client = forgejoClientRegistry.clientFor(sourceHost)
        return try {
            val result = client.getRepository(owner, name).getOrThrow()
                .toForgejoSummary(sourceHost)
            cacheManager.put(cacheKey, result, REPO_DETAILS)
            result
        } catch (e: Exception) {
            cacheManager.getStale<GithubRepoSummary>(cacheKey)?.let { return it }
            throw e
        }
    }

    private suspend fun getForgejoAllReleases(
        owner: String,
        repo: String,
        sourceHost: String,
    ): List<GithubRelease> {
        val cacheKey = "details:releases:forgejo:$sourceHost:$owner/$repo"
        cacheManager.get<List<GithubRelease>>(cacheKey)?.takeIf { it.isNotEmpty() }?.let { return it }
        val client = forgejoClientRegistry.clientFor(sourceHost)
        return try {
            val releases = client.getReleases(owner, repo).getOrNull().orEmpty()
            val result = releases
                .filter { it.draft != true }
                .map { it.toDomain() }
                .sortedByDescending { it.publishedAt }
            if (result.isNotEmpty()) cacheManager.put(cacheKey, result, RELEASES)
            result
        } catch (e: Exception) {
            cacheManager.getStale<List<GithubRelease>>(cacheKey)?.let { return it }
            throw e
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun getForgejoReadme(
        owner: String,
        repo: String,
        defaultBranch: String,
        sourceHost: String,
    ): Triple<String, String?, String>? {
        // v2 — moved off the non-existent `/readme` endpoint (404s on every
        // Forgejo / Gitea instance) onto `/contents/README.md`.
        val cacheKey = "details:readme:forgejo:v2:$sourceHost:$owner/$repo"
        cacheManager.get<CachedReadme>(cacheKey)?.let {
            return Triple(it.content, it.languageCode, it.path)
        }
        val client = forgejoClientRegistry.clientFor(sourceHost)

        // Forgejo / Gitea does NOT implement GitHub's `/repos/{o}/{r}/readme`
        // convenience endpoint — verified live against codeberg.org. We hit
        // the contents endpoint directly. Try `README.md` first (covers the
        // overwhelming majority including Gadgetbridge), and on 404 fall
        // back to listing the repo root and scanning for any
        // `^README(\..+)?$` file.
        val dto = client.getContentsFile(owner, repo, "README.md", defaultBranch).getOrNull()
            ?: client.listContentsRoot(owner, repo, defaultBranch).getOrNull()
                ?.firstOrNull { entry ->
                    entry.type == "file" && entry.name?.let { READMEFileNameRegex.matches(it) } == true
                }
                ?.let { entry ->
                    client.getContentsFile(owner, repo, entry.path ?: entry.name!!, defaultBranch).getOrNull()
                }
            ?: run {
                cacheManager.getStale<CachedReadme>(cacheKey)?.let {
                    return Triple(it.content, it.languageCode, it.path)
                }
                return null
            }

        // Forgejo's contents response is base64 (single continuous line,
        // no MIME wrapping — Mime variant tolerates both forms transparently).
        val rawContent = dto.content ?: return null
        val decoded = try {
            Base64.Mime.decode(rawContent).decodeToString()
        } catch (e: IllegalArgumentException) {
            logger.warn("Failed to decode Forgejo readme for $sourceHost/$owner/$repo: ${e.message}")
            return null
        }
        val path = dto.path?.takeIf { it.isNotBlank() } ?: "README.md"
        // Relative image refs in a Forgejo README need the per-host raw URL
        // base. Forgejo's raw path shape is `/{o}/{r}/raw/branch/{ref}/`.
        val baseUrl = "https://$sourceHost/$owner/$repo/raw/branch/$defaultBranch/"
        val processed = preprocessMarkdown(markdown = decoded, baseUrl = baseUrl)
        val detected = readmeHelper.detectReadmeLanguage(processed)
        cacheManager.put(
            cacheKey,
            CachedReadme(content = processed, languageCode = detected, path = path),
            README,
        )
        return Triple(processed, detected, path)
    }

    private companion object {
        // README, README.md, README.rst, README.adoc, Readme.txt, etc.
        private val READMEFileNameRegex = Regex("""^README(\..+)?$""", RegexOption.IGNORE_CASE)
    }

    private suspend fun getForgejoRepoStats(
        owner: String,
        repo: String,
        sourceHost: String,
    ): RepoStats {
        // v2 — added license sniffing (from /contents/LICENSE) +
        // aggregated download count (from release assets), neither of
        // which Forgejo exposes on the /repos endpoint itself.
        val cacheKey = "details:stats:forgejo:v2:$sourceHost:$owner/$repo"
        cacheManager.get<RepoStats>(cacheKey)?.let { return it }
        val client = forgejoClientRegistry.clientFor(sourceHost)
        return try {
            val info = client.getRepository(owner, repo).getOrThrow()
            // Best-effort license + downloads enrichment. Both can fail
            // silently — repo stats still render with the core counters
            // pulled directly from the /repos payload.
            val license = detectForgejoLicense(client, owner, repo, info.defaultBranch ?: "main")
            val downloads = sumForgejoReleaseDownloads(sourceHost, owner, repo)
            val result = RepoStats(
                stars = info.starsCount,
                forks = info.forksCount,
                openIssues = info.openIssuesCount,
                license = license,
                totalDownloads = downloads,
            )
            cacheManager.put(cacheKey, result, REPO_STATS)
            result
        } catch (e: Exception) {
            cacheManager.getStale<RepoStats>(cacheKey)?.let { return it }
            throw e
        }
    }

    /**
     * Forgejo / Gitea does NOT expose a `license` field on
     * `/repos/{o}/{r}`. The dedicated `/license` endpoint is
     * GitHub-specific (404 on Forgejo). Best-effort: fetch the LICENSE
     * file from the repo root and regex-match the first ~200 chars
     * against canonical license headers. Returns SPDX-style id when
     * matched, falls back to a short label, returns `null` on miss.
     */
    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun detectForgejoLicense(
        client: zed.rainxch.core.data.network.ForgejoApiClient,
        owner: String,
        repo: String,
        ref: String,
    ): String? {
        // Try common LICENSE filenames in priority order. Most repos use
        // bare `LICENSE`; common alternates covered below.
        val candidates = listOf("LICENSE", "LICENSE.md", "LICENSE.txt", "COPYING")
        val dto = candidates.firstNotNullOfOrNull { name ->
            client.getContentsFile(owner, repo, name, ref).getOrNull()
        } ?: return null
        val raw = dto.content ?: return null
        val text = try {
            Base64.Mime.decode(raw).decodeToString().take(400)
        } catch (e: IllegalArgumentException) {
            return null
        }
        return spdxFromLicenseHeader(text)
    }

    private fun spdxFromLicenseHeader(text: String): String? {
        val upper = text.uppercase()
        return when {
            upper.contains("GNU AFFERO GENERAL PUBLIC LICENSE") && upper.contains("VERSION 3") -> "AGPL-3.0"
            upper.contains("GNU LESSER GENERAL PUBLIC LICENSE") && upper.contains("VERSION 3") -> "LGPL-3.0"
            upper.contains("GNU LESSER GENERAL PUBLIC LICENSE") && upper.contains("VERSION 2.1") -> "LGPL-2.1"
            upper.contains("GNU GENERAL PUBLIC LICENSE") && upper.contains("VERSION 3") -> "GPL-3.0"
            upper.contains("GNU GENERAL PUBLIC LICENSE") && upper.contains("VERSION 2") -> "GPL-2.0"
            upper.contains("APACHE LICENSE") && upper.contains("VERSION 2.0") -> "Apache-2.0"
            upper.contains("MIT LICENSE") || (upper.startsWith("MIT ") && upper.contains("PERMISSION")) -> "MIT"
            upper.contains("BSD 3-CLAUSE") || upper.contains("BSD-3-CLAUSE") -> "BSD-3-Clause"
            upper.contains("BSD 2-CLAUSE") || upper.contains("BSD-2-CLAUSE") -> "BSD-2-Clause"
            upper.contains("MOZILLA PUBLIC LICENSE") && upper.contains("VERSION 2.0") -> "MPL-2.0"
            upper.contains("THE UNLICENSE") || upper.contains("UNLICENSE") -> "Unlicense"
            upper.contains("CREATIVE COMMONS") && upper.contains("CC0") -> "CC0-1.0"
            upper.contains("EUROPEAN UNION PUBLIC LICENCE") -> "EUPL-1.2"
            else -> null
        }
    }

    /**
     * Forgejo's `/repos/{o}/{r}` response carries no aggregate download
     * count. Each release `asset.download_count` is per-asset, and the
     * stable channel for total downloads is the sum across all release
     * assets. Reuses the cached releases list when present so we don't
     * double-fetch when stats + releases load in parallel from the VM.
     */
    private suspend fun sumForgejoReleaseDownloads(
        sourceHost: String,
        owner: String,
        repo: String,
    ): Long {
        val cacheKey = "details:releases:forgejo:$sourceHost:$owner/$repo"
        val cached = cacheManager.get<List<GithubRelease>>(cacheKey).orEmpty()
        val releases = if (cached.isNotEmpty()) {
            cached
        } else {
            try {
                getForgejoAllReleases(owner, repo, sourceHost)
            } catch (e: Exception) {
                logger.debug("Forgejo downloads sum: releases fetch failed: ${e.message}")
                return 0L
            }
        }
        return releases.sumOf { release ->
            release.assets.sumOf { it.downloadCount }
        }
    }

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
