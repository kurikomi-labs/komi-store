package zed.rainxch.core.data.data_source.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
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

    @Volatile private var migrated: Boolean = false

    private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    init {
        migrationScope.launch { migrateIfNeeded() }
    }

    override suspend fun save(token: GithubDeviceTokenSuccessDto) {
        val stamped = token.copy(
            savedAtEpochMillis = token.savedAtEpochMillis ?: Clock.System.now().toEpochMilliseconds(),
        )
        ksafe.put(tokenKey, stamped)
    }

    override fun tokenFlow(): Flow<GithubDeviceTokenSuccessDto?> = callbackFlow {
        migrateIfNeeded()
        val current = runCatching {
            ksafe.get<GithubDeviceTokenSuccessDto?>(tokenKey, null)
        }.getOrNull()
        trySend(current)
        awaitClose { }
    }

    override suspend fun currentToken(): GithubDeviceTokenSuccessDto? {
        migrateIfNeeded()
        return runCatching {
            ksafe.get<GithubDeviceTokenSuccessDto?>(tokenKey, null)
        }.getOrNull()
    }

    override fun blockingCurrentToken(): GithubDeviceTokenSuccessDto? =
        runBlocking { currentToken() }

    override suspend fun clear() {
        runCatching { ksafe.delete(tokenKey) }
        runCatching { legacyDataStore.edit { it.remove(legacyKey) } }
    }

    override suspend fun isTokenExpired(): Boolean {
        val token = currentToken() ?: return true
        val savedAt = token.savedAtEpochMillis ?: return false
        val expiresIn = token.expiresIn ?: return false
        val expiresAtMillis = savedAt + (expiresIn * 1000L)
        return Clock.System.now().toEpochMilliseconds() > expiresAtMillis
    }

    private suspend fun migrateIfNeeded() {
        if (migrated) return
        migrationLock.withLock {
            if (migrated) return
            val existing = runCatching {
                ksafe.get<GithubDeviceTokenSuccessDto?>(tokenKey, null)
            }.getOrNull()
            if (existing != null) {
                migrated = true
                return
            }
            val legacyRaw = runCatching {
                legacyDataStore.data.first()[legacyKey]
            }.getOrNull()
            if (legacyRaw != null) {
                val parsed = runCatching {
                    json.decodeFromString(GithubDeviceTokenSuccessDto.serializer(), legacyRaw)
                }.getOrNull()
                if (parsed != null) {
                    runCatching { ksafe.put(tokenKey, parsed) }
                }
                runCatching { legacyDataStore.edit { it.remove(legacyKey) } }
            }
            migrated = true
        }
    }

    private companion object {
        const val TOKEN_KEY_NAME = "github_token"
    }
}
