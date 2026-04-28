package zed.rainxch.core.data.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.core.data.dto.BackendExploreResponse
import zed.rainxch.core.data.dto.BackendRepoResponse
import zed.rainxch.core.data.dto.BackendSearchResponse
import zed.rainxch.core.data.dto.EventRequest
import zed.rainxch.core.data.dto.ExternalMatchRequest
import zed.rainxch.core.data.dto.ExternalMatchResponse
import zed.rainxch.core.data.dto.ProductTelemetryBatch
import zed.rainxch.core.data.dto.ProductTelemetryEventBody
import zed.rainxch.core.data.dto.GithubReadmeResponseDto
import zed.rainxch.core.data.dto.ReleaseNetwork
import zed.rainxch.core.data.dto.SigningFingerprintSeedResponse
import zed.rainxch.core.data.dto.UserProfileNetwork
import zed.rainxch.core.domain.model.ProxyConfig
import kotlin.coroutines.cancellation.CancellationException

/**
 * Client for GitHub Store's own backend (trending/popular/search).
 * Treated as *discovery* traffic — routes through the discovery-scope
 * proxy so users configuring a proxy for GitHub browsing also have
 * their backend discovery requests proxied consistently.
 */
class BackendApiClient(
    proxyConfigFlow: StateFlow<ProxyConfig>,
    private val tokenStore: TokenStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    @Volatile
    private var httpClient: HttpClient = buildClient(proxyConfigFlow.value)

    init {
        proxyConfigFlow
            .drop(1)
            .distinctUntilChanged()
            .onEach { config ->
                mutex.withLock {
                    httpClient.close()
                    httpClient = buildClient(config)
                }
            }.launchIn(scope)
    }

    private fun buildClient(proxyConfig: ProxyConfig): HttpClient =
        createPlatformHttpClient(proxyConfig).config {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 5_000
                connectTimeoutMillis = 3_000
                socketTimeoutMillis = 5_000
            }
            defaultRequest {
                url(BASE_URL)
            }
            expectSuccess = false
        }

    fun close() {
        httpClient.close()
        scope.cancel()
    }

    suspend fun getCategory(category: String, platform: String): Result<List<BackendRepoResponse>> =
        safeCall {
            val response = httpClient.get("categories/$category/$platform")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(BackendException(response.status.value))
            }
        }

    suspend fun getTopic(bucket: String, platform: String): Result<List<BackendRepoResponse>> =
        safeCall {
            val response = httpClient.get("topics/$bucket/$platform")
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(BackendException(response.status.value))
            }
        }

    suspend fun search(
        query: String,
        platform: String? = null,
        sort: String? = null,
        limit: Int = 20,
        offset: Int = 0,
    ): Result<BackendSearchResponse> =
        safeCall {
            val token = currentUserGithubToken()
            val response = httpClient.get("search") {
                parameter("q", query)
                if (platform != null) parameter("platform", platform)
                if (sort != null) parameter("sort", sort)
                parameter("limit", limit)
                parameter("offset", offset)
                if (token != null) header(X_GITHUB_TOKEN_HEADER, token)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(BackendException(response.status.value))
            }
        }

    suspend fun searchExplore(
        query: String,
        platform: String? = null,
        page: Int = 1,
    ): Result<BackendExploreResponse> =
        safeCall {
            val token = currentUserGithubToken()
            val response = httpClient.get("search/explore") {
                parameter("q", query)
                if (platform != null) parameter("platform", platform)
                parameter("page", page)

                timeout {
                    requestTimeoutMillis = 30_000
                    socketTimeoutMillis = 30_000
                }

                if (token != null) header(X_GITHUB_TOKEN_HEADER, token)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(BackendException(response.status.value))
            }
        }

    private suspend fun currentUserGithubToken(): String? =
        try {
            tokenStore.currentToken()?.accessToken?.trim()?.takeIf { it.isNotEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    suspend fun getRepo(owner: String, name: String): Result<BackendRepoResponse> =
        safeCall {
            val token = currentUserGithubToken()
            val response = httpClient.get("repo/$owner/$name") {
                if (token != null) header(X_GITHUB_TOKEN_HEADER, token)
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(BackendException(response.status.value))
            }
        }

    suspend fun getReleases(
        owner: String,
        name: String,
        page: Int = 1,
        perPage: Int = 100,
    ): Result<List<ReleaseNetwork>> =
        safeCall {
            val token = currentUserGithubToken()
            val response = httpClient.get("releases/$owner/$name") {
                parameter("page", page)
                parameter("per_page", perPage)
                if (token != null) header(X_GITHUB_TOKEN_HEADER, token)
                // Cold path: backend goes to GitHub + paginates. 15s covers p99.
                timeout {
                    requestTimeoutMillis = 15_000
                    socketTimeoutMillis = 15_000
                }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(BackendException(response.status.value))
            }
        }

    suspend fun getReadme(
        owner: String,
        name: String,
    ): Result<GithubReadmeResponseDto> =
        safeCall {
            val token = currentUserGithubToken()
            val response = httpClient.get("readme/$owner/$name") {
                if (token != null) header(X_GITHUB_TOKEN_HEADER, token)
                timeout {
                    requestTimeoutMillis = 15_000
                    socketTimeoutMillis = 15_000
                }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(BackendException(response.status.value))
            }
        }

    suspend fun getUser(username: String): Result<UserProfileNetwork> =
        safeCall {
            val token = currentUserGithubToken()
            val response = httpClient.get("user/$username") {
                if (token != null) header(X_GITHUB_TOKEN_HEADER, token)
                timeout {
                    requestTimeoutMillis = 15_000
                    socketTimeoutMillis = 15_000
                }
            }
            if (response.status.isSuccess()) {
                Result.success(response.body())
            } else {
                Result.failure(BackendException(response.status.value))
            }
        }

    suspend fun postExternalMatch(body: ExternalMatchRequest): Result<ExternalMatchResponse> =
        safeCall {
            val response = httpClient.post("external-match") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            when {
                response.status.isSuccess() ->
                    Result.success(response.body())
                response.status == HttpStatusCode.TooManyRequests ->
                    Result.failure(RateLimitedException())
                else ->
                    Result.failure(BackendException(response.status.value))
            }
        }

    suspend fun getSigningSeeds(
        since: Long? = null,
        cursor: String? = null,
        platform: String = "android",
    ): Result<SigningFingerprintSeedResponse> =
        safeCall {
            val response = httpClient.get("signing-seeds") {
                parameter("platform", platform)
                if (since != null) parameter("since", since)
                if (cursor != null) parameter("cursor", cursor)
            }
            when {
                response.status.isSuccess() ->
                    Result.success(response.body())
                response.status == HttpStatusCode.TooManyRequests ->
                    Result.failure(RateLimitedException())
                else ->
                    Result.failure(BackendException(response.status.value))
            }
        }

    suspend fun postEvents(events: List<EventRequest>): Result<Unit> =
        safeCall {
            val response = httpClient.post("events") {
                contentType(ContentType.Application.Json)
                setBody(events)
            }
            when {
                response.status == HttpStatusCode.NoContent || response.status.isSuccess() ->
                    Result.success(Unit)
                response.status == HttpStatusCode.TooManyRequests ->
                    Result.failure(RateLimitedException())
                else ->
                    Result.failure(BackendException(response.status.value))
            }
        }

    suspend fun postProductTelemetryEvents(events: List<ProductTelemetryEventBody>): Result<Unit> =
        safeCall {
            val response = httpClient.post("telemetry/events") {
                contentType(ContentType.Application.Json)
                setBody(ProductTelemetryBatch(events))
            }
            when {
                response.status == HttpStatusCode.NoContent || response.status.isSuccess() ->
                    Result.success(Unit)
                response.status == HttpStatusCode.TooManyRequests ->
                    Result.failure(RateLimitedException())
                else ->
                    Result.failure(BackendException(response.status.value))
            }
        }

    private inline fun <T> safeCall(block: () -> Result<T>): Result<T> =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }

    companion object {
        private const val BASE_URL = BACKEND_BASE_URL
        private const val X_GITHUB_TOKEN_HEADER = "X-GitHub-Token"
    }
}

class BackendException(
    val statusCode: Int,
    message: String = "HTTP $statusCode",
) : Exception(message)

class RateLimitedException : Exception("Rate limited by backend (429)")
