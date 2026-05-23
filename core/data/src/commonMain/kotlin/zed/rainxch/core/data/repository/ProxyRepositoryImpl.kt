package zed.rainxch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import zed.rainxch.core.data.network.ProxyManager
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.ProxyConfig
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.core.domain.repository.ProxyRepository
import zed.rainxch.core.data.secure.safeDelete
import zed.rainxch.core.data.secure.safeGet
import zed.rainxch.core.data.secure.safeGetFlow
import zed.rainxch.core.data.secure.safePut

class ProxyRepositoryImpl(
    private val ksafe: KSafe,
    private val legacyDataStore: DataStore<Preferences>,
    private val logger: GitHubStoreLogger,
) : ProxyRepository {

    private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val migrationLock = Mutex()
    private val migrationDeferred = CompletableDeferred<Unit>()

    @Volatile private var migrated: Boolean = false

    init {
        migrationScope.launch {
            runCatching { migrateIfNeeded() }
            migrationDeferred.complete(Unit)
        }
    }

    private data class ScopeKeys(
        val type: String,
        val host: String,
        val port: String,
        val username: String,
        val password: String,
    )

    private fun keysFor(scope: ProxyScope): ScopeKeys {
        val prefix = when (scope) {
            ProxyScope.DISCOVERY -> "discovery"
            ProxyScope.DOWNLOAD -> "download"
            ProxyScope.TRANSLATION -> "translation"
        }
        return ScopeKeys(
            type = "${prefix}_proxy_type",
            host = "${prefix}_proxy_host",
            port = "${prefix}_proxy_port",
            username = "${prefix}_proxy_username",
            password = "${prefix}_proxy_password",
        )
    }

    private object MasterKeys {
        const val TYPE = "master_proxy_type"
        const val HOST = "master_proxy_host"
        const val PORT = "master_proxy_port"
        const val USERNAME = "master_proxy_username"
        const val PASSWORD = "master_proxy_password"
    }

    private fun useMasterKeyFor(scope: ProxyScope): String =
        when (scope) {
            ProxyScope.DISCOVERY -> "discovery_proxy_use_master"
            ProxyScope.DOWNLOAD -> "download_proxy_use_master"
            ProxyScope.TRANSLATION -> "translation_proxy_use_master"
        }

    override fun getProxyConfig(scope: ProxyScope): Flow<ProxyConfig> = flow {
        migrationDeferred.await()
        val keys = keysFor(scope)
        emitAll(
            combine(
                ksafe.safeGetFlow<String?>(keys.type, null),
                ksafe.safeGetFlow<String?>(keys.host, null),
                ksafe.safeGetFlow<Int?>(keys.port, null),
                ksafe.safeGetFlow<String?>(keys.username, null),
                ksafe.safeGetFlow<String?>(keys.password, null),
            ) { type, host, port, user, pass ->
                parseConfig(type, host, port, user, pass)
            },
        )
    }

    private fun parseConfig(
        type: String?,
        host: String?,
        port: Int?,
        username: String?,
        password: String?,
    ): ProxyConfig =
        when (type) {
            "system" -> ProxyConfig.System
            "none" -> ProxyConfig.None
            "http" -> {
                val validHost = host?.takeIf { it.isNotBlank() }
                val validPort = port?.takeIf { it in 1..65535 }
                if (validHost != null && validPort != null) {
                    ProxyConfig.Http(validHost, validPort, username, password)
                } else {
                    logger.warn("Malformed HTTP proxy (host=$host port=$port); fallback System")
                    ProxyConfig.System
                }
            }
            "socks" -> {
                val validHost = host?.takeIf { it.isNotBlank() }
                val validPort = port?.takeIf { it in 1..65535 }
                if (validHost != null && validPort != null) {
                    ProxyConfig.Socks(validHost, validPort, username, password)
                } else {
                    logger.warn("Malformed SOCKS proxy (host=$host port=$port); fallback System")
                    ProxyConfig.System
                }
            }
            else -> ProxyConfig.System
        }

    override suspend fun setProxyConfig(scope: ProxyScope, config: ProxyConfig) {
        migrationDeferred.await()
        val keys = keysFor(scope)
        when (config) {
            is ProxyConfig.None -> {
                ksafe.safePut(keys.type, "none")
                ksafe.safeDelete(keys.host); ksafe.safeDelete(keys.port)
                ksafe.safeDelete(keys.username); ksafe.safeDelete(keys.password)
            }
            is ProxyConfig.System -> {
                ksafe.safePut(keys.type, "system")
                ksafe.safeDelete(keys.host); ksafe.safeDelete(keys.port)
                ksafe.safeDelete(keys.username); ksafe.safeDelete(keys.password)
            }
            is ProxyConfig.Http -> {
                ksafe.safePut(keys.type, "http")
                ksafe.safePut(keys.host, config.host)
                ksafe.safePut(keys.port, config.port)
                writeOrClear(keys.username, config.username)
                writeOrClear(keys.password, config.password)
            }
            is ProxyConfig.Socks -> {
                ksafe.safePut(keys.type, "socks")
                ksafe.safePut(keys.host, config.host)
                ksafe.safePut(keys.port, config.port)
                writeOrClear(keys.username, config.username)
                writeOrClear(keys.password, config.password)
            }
        }
        ProxyManager.setConfig(scope, config)
    }

    private suspend fun writeOrClear(key: String, value: String?) {
        if (value != null) ksafe.safePut(key, value) else ksafe.safeDelete(key)
    }

    override fun getMasterProxyConfig(): Flow<ProxyConfig?> = flow {
        migrationDeferred.await()
        emitAll(
            combine(
                ksafe.safeGetFlow<String?>(MasterKeys.TYPE, null),
                ksafe.safeGetFlow<String?>(MasterKeys.HOST, null),
                ksafe.safeGetFlow<Int?>(MasterKeys.PORT, null),
                ksafe.safeGetFlow<String?>(MasterKeys.USERNAME, null),
                ksafe.safeGetFlow<String?>(MasterKeys.PASSWORD, null),
            ) { type, host, port, user, pass ->
                if (type == null) null else parseConfig(type, host, port, user, pass)
            },
        )
    }

    override suspend fun setMasterProxyConfig(config: ProxyConfig) {
        migrationDeferred.await()
        when (config) {
            is ProxyConfig.None -> {
                ksafe.safePut(MasterKeys.TYPE, "none")
                ksafe.safeDelete(MasterKeys.HOST); ksafe.safeDelete(MasterKeys.PORT)
                ksafe.safeDelete(MasterKeys.USERNAME); ksafe.safeDelete(MasterKeys.PASSWORD)
            }
            is ProxyConfig.System -> {
                ksafe.safePut(MasterKeys.TYPE, "system")
                ksafe.safeDelete(MasterKeys.HOST); ksafe.safeDelete(MasterKeys.PORT)
                ksafe.safeDelete(MasterKeys.USERNAME); ksafe.safeDelete(MasterKeys.PASSWORD)
            }
            is ProxyConfig.Http -> {
                ksafe.safePut(MasterKeys.TYPE, "http")
                ksafe.safePut(MasterKeys.HOST, config.host)
                ksafe.safePut(MasterKeys.PORT, config.port)
                writeOrClear(MasterKeys.USERNAME, config.username)
                writeOrClear(MasterKeys.PASSWORD, config.password)
            }
            is ProxyConfig.Socks -> {
                ksafe.safePut(MasterKeys.TYPE, "socks")
                ksafe.safePut(MasterKeys.HOST, config.host)
                ksafe.safePut(MasterKeys.PORT, config.port)
                writeOrClear(MasterKeys.USERNAME, config.username)
                writeOrClear(MasterKeys.PASSWORD, config.password)
            }
        }
    }

    override fun getUseMaster(scope: ProxyScope): Flow<Boolean> = flow {
        migrationDeferred.await()
        emitAll(ksafe.safeGetFlow(useMasterKeyFor(scope), false))
    }

    override suspend fun setUseMaster(scope: ProxyScope, useMaster: Boolean) {
        migrationDeferred.await()
        ksafe.safePut(useMasterKeyFor(scope), useMaster)
    }

    private suspend fun migrateIfNeeded() {
        if (migrated) return
        migrationLock.withLock {
            if (migrated) return
            val alreadyMarked = runCatching { ksafe.safeGet(MIGRATION_MARKER, false) }.getOrDefault(false)
            if (alreadyMarked) {
                migrated = true
                return
            }
            val snapshot = runCatching { legacyDataStore.data.first() }.getOrNull()
            if (snapshot == null) {

                return
            }

            var anyFailure = false
            val keysToClear = mutableListOf<Preferences.Key<*>>()

            ProxyScope.entries.forEach { scope ->
                val keys = keysFor(scope)
                val prefix = when (scope) {
                    ProxyScope.DISCOVERY -> "discovery"
                    ProxyScope.DOWNLOAD -> "download"
                    ProxyScope.TRANSLATION -> "translation"
                }
                val typeLegacyKey = stringPreferencesKey("${prefix}_proxy_type")
                val hostLegacyKey = stringPreferencesKey("${prefix}_proxy_host")
                val portLegacyKey = intPreferencesKey("${prefix}_proxy_port")
                val userLegacyKey = stringPreferencesKey("${prefix}_proxy_username")
                val passLegacyKey = stringPreferencesKey("${prefix}_proxy_password")

                val scopeType = snapshot[typeLegacyKey]
                val type = scopeType ?: snapshot[stringPreferencesKey("proxy_type")] ?: return@forEach
                val host = snapshot[hostLegacyKey] ?: snapshot[stringPreferencesKey("proxy_host")]
                val port = snapshot[portLegacyKey] ?: snapshot[intPreferencesKey("proxy_port")]
                val user = snapshot[userLegacyKey] ?: snapshot[stringPreferencesKey("proxy_username")]
                val pass = snapshot[passLegacyKey] ?: snapshot[stringPreferencesKey("proxy_password")]

                val typeOk = ksafe.safePut(keys.type, type)
                if (!typeOk) { anyFailure = true; return@forEach }
                scopeType?.let { keysToClear += typeLegacyKey }

                if (host != null) {
                    if (ksafe.safePut(keys.host, host)) keysToClear += hostLegacyKey
                    else anyFailure = true
                }
                if (port != null) {
                    if (ksafe.safePut(keys.port, port)) keysToClear += portLegacyKey
                    else anyFailure = true
                }
                if (user != null) {
                    if (ksafe.safePut(keys.username, user)) keysToClear += userLegacyKey
                    else anyFailure = true
                }
                if (pass != null) {
                    if (ksafe.safePut(keys.password, pass)) keysToClear += passLegacyKey
                    else anyFailure = true
                }
            }

            val anyScopeTouched = keysToClear.isNotEmpty()
            if (anyScopeTouched) {
                keysToClear += stringPreferencesKey("proxy_type")
                keysToClear += stringPreferencesKey("proxy_host")
                keysToClear += intPreferencesKey("proxy_port")
                keysToClear += stringPreferencesKey("proxy_username")
                keysToClear += stringPreferencesKey("proxy_password")
            }

            if (keysToClear.isNotEmpty()) {
                val cleared = runCatching {
                    legacyDataStore.edit { prefs ->
                        keysToClear.forEach { prefs.remove(it) }
                    }
                }
                if (cleared.isFailure) anyFailure = true
            }

            if (!anyFailure) {
                runCatching { ksafe.safePut(MIGRATION_MARKER, true) }
                migrated = true
            }
        }
    }

    private companion object {
        const val MIGRATION_MARKER = "__migrated_proxy_v1__"
    }
}
