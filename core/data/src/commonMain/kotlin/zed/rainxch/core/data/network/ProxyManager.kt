package zed.rainxch.core.data.network

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import zed.rainxch.core.domain.model.MirrorPreference
import zed.rainxch.core.domain.model.ProxyConfig
import zed.rainxch.core.domain.model.ProxyScope
import zed.rainxch.core.domain.model.TrafficKind
import zed.rainxch.core.domain.repository.MirrorRepository

data class MirrorActive(
    val template: String,
    val trafficKinds: Set<TrafficKind>,
)

object ProxyManager {
    private val flows: Map<ProxyScope, MutableStateFlow<ProxyConfig>> =
        ProxyScope.entries.associateWith { MutableStateFlow<ProxyConfig>(ProxyConfig.System) }

    private val mirror = AtomicReference<MirrorActive?>(null)
    private var mirrorCollectorJob: Job? = null

    fun configFlow(scope: ProxyScope): StateFlow<ProxyConfig> = flows.getValue(scope).asStateFlow()

    fun currentConfig(scope: ProxyScope): ProxyConfig = flows.getValue(scope).value

    fun setConfig(
        scope: ProxyScope,
        config: ProxyConfig,
    ) {
        flows.getValue(scope).value = config
    }

    fun currentMirror(): MirrorActive? = mirror.get()

    fun currentMirrorTemplate(): String? = mirror.get()?.template

    fun startMirrorCollector(
        repository: MirrorRepository,
        scope: CoroutineScope,
    ) {
        if (mirrorCollectorJob?.isActive == true) return
        mirrorCollectorJob =
            scope.launch {
                combine(
                    repository.observePreference(),
                    repository.observeCatalog(),
                ) { pref, catalog ->
                    when (pref) {
                        MirrorPreference.Direct -> null
                        is MirrorPreference.Custom ->
                            MirrorActive(
                                template = pref.template,
                                trafficKinds = setOf(TrafficKind.RELEASE_ASSET, TrafficKind.RAW_FILE),
                            )
                        is MirrorPreference.Selected -> {
                            val cfg = catalog.firstOrNull { it.id == pref.id }
                            val template = cfg?.urlTemplate
                            if (cfg == null || template == null) {
                                null
                            } else {
                                MirrorActive(template = template, trafficKinds = cfg.trafficKinds)
                            }
                        }
                    }
                }.collect { active ->
                    mirror.set(active)
                }
            }
    }
}
