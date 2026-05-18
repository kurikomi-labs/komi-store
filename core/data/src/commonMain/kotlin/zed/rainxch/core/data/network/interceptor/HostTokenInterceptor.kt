package zed.rainxch.core.data.network.interceptor

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlin.coroutines.cancellation.CancellationException
import zed.rainxch.core.domain.model.HostNames
import zed.rainxch.core.domain.repository.HostTokenRepository

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
        val token = try {
            repo.get(host)?.token
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Throwable) {
            null
        }
        if (!token.isNullOrBlank()) {
            request.header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
