package zed.rainxch.core.data.network.interceptor

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import zed.rainxch.core.domain.model.HostNames
import zed.rainxch.core.domain.repository.HostTokenRepository

/**
 * Attaches `Authorization: Bearer <token>` from [HostTokenRepository]
 * to outgoing requests whose URL host has a stored token.
 *
 * Skips when an `Authorization` header already present (so other auth
 * paths like OAuth's TokenStore still win for github.com).
 */
class HostTokenInterceptorConfig {
    lateinit var repository: HostTokenRepository
}

val HostTokenInterceptor = createClientPlugin("HostTokenInterceptor", ::HostTokenInterceptorConfig) {
    val repo = pluginConfig.repository
    onRequest { request, _ ->
        val existing = request.headers[HttpHeaders.Authorization]
        if (!existing.isNullOrBlank()) return@onRequest
        val host = HostNames.normalize(request.url.host)
        if (host.isBlank()) return@onRequest
        val token = runCatching { runBlocking { repo.get(host)?.token } }.getOrNull()
        if (!token.isNullOrBlank()) {
            request.header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
