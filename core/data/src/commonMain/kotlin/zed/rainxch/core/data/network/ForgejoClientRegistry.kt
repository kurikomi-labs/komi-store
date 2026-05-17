package zed.rainxch.core.data.network

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
import zed.rainxch.core.domain.model.ProxyConfig

class ForgejoClientRegistry(
    private val proxyConfigFlow: StateFlow<ProxyConfig>,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private val clients = mutableMapOf<String, ForgejoApiClient>()

    init {
        proxyConfigFlow
            .drop(1)
            .distinctUntilChanged()
            .onEach { _ ->
                // Invalidate cached clients so the next clientFor()
                // rebuilds them against the new proxy config.
                mutex.withLock { clients.clear() }
            }.launchIn(scope)
    }

    suspend fun clientFor(host: String): ForgejoApiClient {
        val key = host.lowercase().trim()
        return mutex.withLock {
            clients.getOrPut(key) {
                ForgejoApiClient(key, proxyConfigFlow.value)
            }
        }
    }

    fun close() {
        scope.cancel()
    }
}
