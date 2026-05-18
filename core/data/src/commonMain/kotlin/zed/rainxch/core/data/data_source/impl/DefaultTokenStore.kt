package zed.rainxch.core.data.data_source.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import zed.rainxch.core.data.data_source.TokenStore
import zed.rainxch.core.data.dto.GithubDeviceTokenSuccessDto
import kotlin.time.Clock

class DefaultTokenStore(
    private val ksafe: KSafe,
    private val legacyDataStore: DataStore<Preferences>,
) : TokenStore {
    private val tokenKey = TOKEN_KEY_NAME
    private val legacyKey = stringPreferencesKey("token")
    private val json = Json { ignoreUnknownKeys = true }

    private val migrationLock = Mutex()
    private val migrationDeferred = CompletableDeferred<Unit>()
    private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        migrationScope.launch {
            runCatching { migrateIfNeeded() }
            migrationDeferred.complete(Unit)
        }
    }

    override suspend fun save(token: GithubDeviceTokenSuccessDto) {
        migrationDeferred.await()
        val stamped = token.copy(
            savedAtEpochMillis = token.savedAtEpochMillis ?: Clock.System.now().toEpochMilliseconds(),
        )
        ksafe.put(tokenKey, stamped)
    }

    override fun tokenFlow(): Flow<GithubDeviceTokenSuccessDto?> = flow {
        migrationDeferred.await()
        emitAll(ksafe.getFlow<GithubDeviceTokenSuccessDto?>(tokenKey, null))
    }

    override suspend fun currentToken(): GithubDeviceTokenSuccessDto? {
        migrationDeferred.await()
        return runCatching {
            ksafe.get<GithubDeviceTokenSuccessDto?>(tokenKey, null)
        }.getOrNull()
    }

    override fun blockingCurrentToken(): GithubDeviceTokenSuccessDto? =
        runBlocking { currentToken() }

    override suspend fun clear() {
        migrationDeferred.await()
        val ksafeError = runCatching { ksafe.delete(tokenKey) }.exceptionOrNull()
        // Legacy cleanup is best-effort: do it regardless so a sign-out
        // never leaves a stale plaintext token behind. The original ksafe
        // exception still surfaces to the caller below.
        runCatching { legacyDataStore.edit { it.remove(legacyKey) } }
        if (ksafeError != null) throw ksafeError
    }

    override suspend fun isTokenExpired(): Boolean {
        val token = currentToken() ?: return true
        val savedAt = token.savedAtEpochMillis ?: return false
        val expiresIn = token.expiresIn ?: return false
        val expiresAtMillis = savedAt + (expiresIn * 1000L)
        return Clock.System.now().toEpochMilliseconds() > expiresAtMillis
    }

    private suspend fun migrateIfNeeded() = migrationLock.withLock {
        val existing = runCatching {
            ksafe.get<GithubDeviceTokenSuccessDto?>(tokenKey, null)
        }.getOrNull()
        if (existing != null) return@withLock

        val legacyRaw = runCatching {
            legacyDataStore.data.first()[legacyKey]
        }.getOrNull() ?: return@withLock

        val parsed = runCatching {
            json.decodeFromString(GithubDeviceTokenSuccessDto.serializer(), legacyRaw)
        }.getOrNull() ?: run {
            // Legacy value present but unparseable — leave it in DataStore
            // (don't drop the only copy) and bail out without marking
            // migrated, so a code change that fixes the parse can retry.
            return@withLock
        }

        val putResult = runCatching { ksafe.put(tokenKey, parsed) }
        if (putResult.isFailure) {
            // Never drop the legacy entry if we couldn't persist into KSafe.
            // The user's token stays readable via the slow path next launch.
            return@withLock
        }
        runCatching { legacyDataStore.edit { it.remove(legacyKey) } }
    }

    private companion object {
        const val TOKEN_KEY_NAME = "github_token"
    }
}
