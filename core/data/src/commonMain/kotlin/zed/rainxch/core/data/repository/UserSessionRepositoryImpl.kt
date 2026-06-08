package zed.rainxch.core.data.repository

import kotlinx.coroutines.CancellationException
import co.touchlab.kermit.Logger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import zed.rainxch.core.data.cache.CacheManager
import zed.rainxch.core.data.cache.CacheManager.CacheTtl.USER_PROFILE
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.core.data.dto.UserProfileNetwork
import zed.rainxch.core.data.mappers.toUserProfile
import zed.rainxch.core.data.network.executeRequest
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.account.UserProfile
import zed.rainxch.core.domain.repository.UserSessionRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class UserSessionRepositoryImpl(
    private val tokenStore: TokenStore,
    private val cacheManager: CacheManager,
    private val httpClientProvider: () -> HttpClient,
    private val logger: GitHubStoreLogger
) : UserSessionRepository {
    private val httpClient: HttpClient get() = httpClientProvider()

    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    private val sessionExpiredMutex = Mutex()

    private var _failingTokenSnapshot: String? = null
    private var _firstFailureAtMillis: Long = 0L
    private var _consecutiveFailures: Int = 0

    override fun isUserLoggedIn(): Flow<Boolean> =
        tokenStore
            .tokenFlow()
            .map { it != null }

    override suspend fun isCurrentlyUserLoggedIn(): Boolean = tokenStore.currentToken() != null

    override fun getUser(): Flow<UserProfile?> = flow {
        val token = tokenStore.currentToken()
        if (token == null) {
            cacheManager.invalidate(CACHE_KEY)
            emit(null)
            return@flow
        }

        val cached = cacheManager.get<UserProfile>(CACHE_KEY)
        if (cached != null) {
            logger.debug("Profile cache hit")
            emit(cached)
            return@flow
        }

        try {
            val networkProfile =
                httpClient
                    .executeRequest<UserProfileNetwork> {
                        get("/user") {
                            header(HttpHeaders.Accept, "application/vnd.github+json")
                        }
                    }.getOrThrow()

            val userProfile = networkProfile.toUserProfile()
            cacheManager.put(CACHE_KEY, userProfile, USER_PROFILE)
            logger.debug("Fetched and cached user profile: ${userProfile.username}")
            emit(userProfile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.error("Failed to fetch user profile: ${e.message}")

            val stale = cacheManager.getStale<UserProfile>(CACHE_KEY)
            if (stale != null) {
                logger.debug("Using stale cached profile as fallback")
                emit(stale)
            } else {
                emit(null)
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun notifySessionExpired(tokenKey: String?) {
        if (tokenKey.isNullOrEmpty()) return
        sessionExpiredMutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            if (tokenKey != _failingTokenSnapshot ||
                now - _firstFailureAtMillis > FAILURE_WINDOW_MS
            ) {
                _failingTokenSnapshot = tokenKey
                _firstFailureAtMillis = now
                _consecutiveFailures = 1
            } else {
                _consecutiveFailures += 1
            }

            if (_consecutiveFailures < REQUIRED_CONSECUTIVE_FAILURES) {
                Logger.w(TAG) {
                    "notifySessionExpired: 401 count=$_consecutiveFailures (need " +
                        "$REQUIRED_CONSECUTIVE_FAILURES); deferring sign-out"
                }
                return@withLock
            }

            val current = tokenStore.currentToken()?.accessToken
            if (current != tokenKey) {
                Logger.w(TAG) {
                    "notifySessionExpired: stored token rotated since the failing " +
                        "request; skipping clear"
                }
                resetCounter()
                return@withLock
            }

            Logger.w(TAG) {
                "notifySessionExpired: $_consecutiveFailures consecutive 401s within " +
                    "window; clearing token"
            }
            tokenStore.clear()
            resetCounter()
            _sessionExpiredEvent.emit(Unit)
        }
    }

    override suspend fun notifyRequestSucceeded(tokenKey: String?) {
        if (tokenKey.isNullOrEmpty()) return
        sessionExpiredMutex.withLock {
            if (tokenKey == _failingTokenSnapshot) {
                resetCounter()
            }
        }
    }

    private fun resetCounter() {
        _failingTokenSnapshot = null
        _firstFailureAtMillis = 0L
        _consecutiveFailures = 0
    }

    override suspend fun logout() {
        tokenStore.clear()
        cacheManager.clearAll()
    }

    private companion object {
        const val TAG = "AuthState"
        const val REQUIRED_CONSECUTIVE_FAILURES = 2
        const val FAILURE_WINDOW_MS = 60_000L
        private const val CACHE_KEY = "profile:me"
    }
}
