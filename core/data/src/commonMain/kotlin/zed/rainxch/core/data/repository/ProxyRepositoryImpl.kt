package zed.rainxch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.anifantakis.lib.ksafe.KSafe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import zed.rainxch.core.data.network.ProxyManager
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.ProxyConfig
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.core.domain.repository.ProxyRepository

class ProxyRepositoryImpl(
    private val ksafe: KSafe,
    private val legacyDataStore: DataStore<Preferences>,
    private val logger: GitHubStoreLogger,
) : ProxyRepository {

    private val migrationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val migrationLock = Mutex()

    @Volatile private var migrated: Boolean = false

    init {
        migrationScope.launch { migrateIfNeeded() }
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

    override fun getProxyConfig(scope: ProxyScope): Flow<ProxyConfig> {
        val keys = keysFor(scope)
        return combine(
            ksafe.getFlow<String?>(keys.type, null),
            ksafe.getFlow<String?>(keys.host, null),
            ksafe.getFlow<Int?>(keys.port, null),
            ksafe.getFlow<String?>(keys.username, null),
            ksafe.getFlow<String?>(keys.password, null),
        ) { type, host, port, user, pass ->
            parseConfig(type, host, port, user, pass)
        }
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
        val keys = keysFor(scope)
        when (config) {
            is ProxyConfig.None -> {
                ksafe.put(keys.type, "none")
                ksafe.delete(keys.host); ksafe.delete(keys.port)
                ksafe.delete(keys.username); ksafe.delete(keys.password)
            }
            is ProxyConfig.System -> {
                ksafe.put(keys.type, "system")
                ksafe.delete(keys.host); ksafe.delete(keys.port)
                ksafe.delete(keys.username); ksafe.delete(keys.password)
            }
            is ProxyConfig.Http -> {
                ksafe.put(keys.type, "http")
                ksafe.put(keys.host, config.host)
                ksafe.put(keys.port, config.port)
                writeOrClear(keys.username, config.username)
                writeOrClear(keys.password, config.password)
            }
            is ProxyConfig.Socks -> {
                ksafe.put(keys.type, "socks")
                ksafe.put(keys.host, config.host)
                ksafe.put(keys.port, config.port)
                writeOrClear(keys.username, config.username)
                writeOrClear(keys.password, config.password)
            }
        }
        ProxyManager.setConfig(scope, config)
    }

    private suspend fun writeOrClear(key: String, value: String?) {
        if (value != null) ksafe.put(key, value) else ksafe.delete(key)
    }

    private suspend fun migrateIfNeeded() {
        if (migrated) return
        migrationLock.withLock {
            if (migrated) return
            val alreadyMarked = runCatching { ksafe.get(MIGRATION_MARKER, false) }.getOrDefault(false)
            if (alreadyMarked) {
                migrated = true
                return
            }
            val snapshot = runCatching { legacyDataStore.data.first() }.getOrNull()
            if (snapshot == null) {
                runCatching { ksafe.put(MIGRATION_MARKER, true) }
                migrated = true
                return
            }

            ProxyScope.entries.forEach { scope ->
                val keys = keysFor(scope)
                val scopeType = snapshot[stringPreferencesKey(keys.type)]
                val source = if (scopeType != null) scope else null
                val prefix = when (scope) {
                    ProxyScope.DISCOVERY -> "discovery"
                    ProxyScope.DOWNLOAD -> "download"
                    ProxyScope.TRANSLATION -> "translation"
                }
                val type = scopeType ?: snapshot[stringPreferencesKey("proxy_type")] ?: return@forEach
                val host = snapshot[stringPreferencesKey("${prefix}_proxy_host")]
                    ?: snapshot[stringPreferencesKey("proxy_host")]
                val port = snapshot[intPreferencesKey("${prefix}_proxy_port")]
                    ?: snapshot[intPreferencesKey("proxy_port")]
                val user = snapshot[stringPreferencesKey("${prefix}_proxy_username")]
                    ?: snapshot[stringPreferencesKey("proxy_username")]
                val pass = snapshot[stringPreferencesKey("${prefix}_proxy_password")]
                    ?: snapshot[stringPreferencesKey("proxy_password")]

                runCatching { ksafe.put(keys.type, type) }
                host?.let { runCatching { ksafe.put(keys.host, it) } }
                port?.let { runCatching { ksafe.put(keys.port, it) } }
                user?.let { runCatching { ksafe.put(keys.username, it) } }
                pass?.let { runCatching { ksafe.put(keys.password, it) } }
                @Suppress("UNUSED_EXPRESSION") source
            }

            runCatching {
                legacyDataStore.edit { prefs ->
                    listOf(
                        "proxy_type", "proxy_host", "proxy_username", "proxy_password",
                        "discovery_proxy_type", "discovery_proxy_host", "discovery_proxy_username", "discovery_proxy_password",
                        "download_proxy_type", "download_proxy_host", "download_proxy_username", "download_proxy_password",
                        "translation_proxy_type", "translation_proxy_host", "translation_proxy_username", "translation_proxy_password",
                    ).forEach { prefs.remove(stringPreferencesKey(it)) }
                    listOf(
                        "proxy_port", "discovery_proxy_port", "download_proxy_port", "translation_proxy_port",
                    ).forEach { prefs.remove(intPreferencesKey(it)) }
                }
            }
            runCatching { ksafe.put(MIGRATION_MARKER, true) }
            migrated = true
        }
    }

    private companion object {
        const val MIGRATION_MARKER = "__migrated_proxy_v1__"
    }
}
