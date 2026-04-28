package zed.rainxch.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import zed.rainxch.core.data.network.ProxyManager
import zed.rainxch.core.domain.logging.GitHubStoreLogger
import zed.rainxch.core.domain.model.ProxyConfig
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.core.domain.repository.ProxyRepository
import zed.rainxch.core.domain.telemetry.ProductTelemetry
import zed.rainxch.core.domain.telemetry.ProductTelemetryEvents
import zed.rainxch.core.domain.telemetry.ProductTelemetryProps

/**
 * Persists one [ProxyConfig] per [ProxyScope] in DataStore, writes
 * changes through to [ProxyManager] so live HTTP clients rebuild
 * with the new settings.
 *
 * **Legacy migration**: installs that predate scoped proxies wrote a
 * single global configuration under the unprefixed keys (`proxy_type`,
 * `proxy_host`, …). On read, if a scope has no value of its own, we
 * fall back to those legacy keys — so existing users' saved proxy
 * silently applies to all three scopes until they customise one.
 * The legacy keys are never written to again; once the user saves
 * any scope, that scope's dedicated keys take over.
 */
class ProxyRepositoryImpl(
    private val preferences: DataStore<Preferences>,
    private val logger: GitHubStoreLogger,
    private val productTelemetry: ProductTelemetry,
) : ProxyRepository {
    // Legacy (pre-scope) keys — read-only, used as a fallback seed.
    private val legacyType = stringPreferencesKey("proxy_type")
    private val legacyHost = stringPreferencesKey("proxy_host")
    private val legacyPort = intPreferencesKey("proxy_port")
    private val legacyUsername = stringPreferencesKey("proxy_username")
    private val legacyPassword = stringPreferencesKey("proxy_password")

    private data class ScopeKeys(
        val type: Preferences.Key<String>,
        val host: Preferences.Key<String>,
        val port: Preferences.Key<Int>,
        val username: Preferences.Key<String>,
        val password: Preferences.Key<String>,
    )

    private fun keysFor(scope: ProxyScope): ScopeKeys {
        val prefix =
            when (scope) {
                ProxyScope.DISCOVERY -> "discovery"
                ProxyScope.DOWNLOAD -> "download"
                ProxyScope.TRANSLATION -> "translation"
            }
        return ScopeKeys(
            type = stringPreferencesKey("${prefix}_proxy_type"),
            host = stringPreferencesKey("${prefix}_proxy_host"),
            port = intPreferencesKey("${prefix}_proxy_port"),
            username = stringPreferencesKey("${prefix}_proxy_username"),
            password = stringPreferencesKey("${prefix}_proxy_password"),
        )
    }

    override fun getProxyConfig(scope: ProxyScope): Flow<ProxyConfig> =
        preferences.data.map { prefs -> readConfigForScope(prefs, scope) }

    private fun readConfigForScope(
        prefs: Preferences,
        scope: ProxyScope,
    ): ProxyConfig {
        val keys = keysFor(scope)
        // Scoped value present → use it directly.
        if (prefs[keys.type] != null) {
            return parseConfig(
                type = prefs[keys.type],
                host = prefs[keys.host],
                port = prefs[keys.port],
                username = prefs[keys.username],
                password = prefs[keys.password],
            )
        }
        // No scoped value yet — lazy-fall back to the legacy single-key
        // config so upgrading users don't lose their proxy setup.
        return parseConfig(
            type = prefs[legacyType],
            host = prefs[legacyHost],
            port = prefs[legacyPort],
            username = prefs[legacyUsername],
            password = prefs[legacyPassword],
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
                    ProxyConfig.Http(
                        host = validHost,
                        port = validPort,
                        username = username,
                        password = password,
                    )
                } else {
                    // Malformed saved proxy — the user *asked for* a proxy,
                    // so falling back to System (honouring OS-level rules)
                    // is safer than silently switching to a direct
                    // connection. Logged so "my proxy stopped working" is
                    // diagnosable.
                    logger.warn(
                        "Malformed HTTP proxy config (type=$type, host=$host, port=$port); falling back to System",
                    )
                    ProxyConfig.System
                }
            }
            "socks" -> {
                val validHost = host?.takeIf { it.isNotBlank() }
                val validPort = port?.takeIf { it in 1..65535 }
                if (validHost != null && validPort != null) {
                    ProxyConfig.Socks(
                        host = validHost,
                        port = validPort,
                        username = username,
                        password = password,
                    )
                } else {
                    logger.warn(
                        "Malformed SOCKS proxy config (type=$type, host=$host, port=$port); falling back to System",
                    )
                    ProxyConfig.System
                }
            }
            else -> ProxyConfig.System
        }

    override suspend fun setProxyConfig(
        scope: ProxyScope,
        config: ProxyConfig,
    ) {
        val keys = keysFor(scope)
        preferences.edit { prefs ->
            when (config) {
                is ProxyConfig.None -> {
                    prefs[keys.type] = "none"
                    prefs.remove(keys.host)
                    prefs.remove(keys.port)
                    prefs.remove(keys.username)
                    prefs.remove(keys.password)
                }
                is ProxyConfig.System -> {
                    prefs[keys.type] = "system"
                    prefs.remove(keys.host)
                    prefs.remove(keys.port)
                    prefs.remove(keys.username)
                    prefs.remove(keys.password)
                }
                is ProxyConfig.Http -> {
                    prefs[keys.type] = "http"
                    prefs[keys.host] = config.host
                    prefs[keys.port] = config.port
                    writeOrRemove(prefs, keys.username, config.username)
                    writeOrRemove(prefs, keys.password, config.password)
                }
                is ProxyConfig.Socks -> {
                    prefs[keys.type] = "socks"
                    prefs[keys.host] = config.host
                    prefs[keys.port] = config.port
                    writeOrRemove(prefs, keys.username, config.username)
                    writeOrRemove(prefs, keys.password, config.password)
                }
            }
        }
        ProxyManager.setConfig(scope, config)

        // Best-effort: a save shouldn't fail because telemetry hiccupped.
        runCatching {
            productTelemetry.fire(
                name = ProductTelemetryEvents.PROXY_CONFIGURED,
                props =
                    mapOf(
                        ProductTelemetryProps.TYPE to
                            when (config) {
                                is ProxyConfig.Http -> "http"
                                is ProxyConfig.Socks -> "socks5"
                                ProxyConfig.None, ProxyConfig.System -> "none"
                            },
                    ),
            )
        }.onFailure { logger.debug("PROXY_CONFIGURED telemetry failed: ${it.message}") }
    }

    private fun writeOrRemove(
        prefs: MutablePreferences,
        key: Preferences.Key<String>,
        value: String?,
    ) {
        if (value != null) {
            prefs[key] = value
        } else {
            prefs.remove(key)
        }
    }
}
