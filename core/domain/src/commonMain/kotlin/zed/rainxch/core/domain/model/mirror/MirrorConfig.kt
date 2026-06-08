package zed.rainxch.core.domain.model.mirror

import zed.rainxch.core.domain.model.mirror.TrafficKind
import kotlin.time.Instant

data class MirrorConfig(
    val id: String,
    val name: String,
    val urlTemplate: String?,
    val type: MirrorType,
    val status: MirrorStatus,
    val latencyMs: Int?,
    val lastCheckedAt: Instant?,
    val trafficKinds: Set<TrafficKind> = setOf(TrafficKind.RELEASE_ASSET, TrafficKind.RAW_FILE),
)
