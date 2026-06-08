package zed.rainxch.core.data.network

import zed.rainxch.core.domain.model.mirror.TrafficKind

data class MirrorActive(
    val template: String,
    val trafficKinds: Set<TrafficKind>,
)
