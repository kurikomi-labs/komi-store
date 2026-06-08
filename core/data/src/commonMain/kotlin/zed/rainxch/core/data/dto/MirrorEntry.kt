package zed.rainxch.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MirrorEntry(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("url_template") val urlTemplate: String?,
    @SerialName("type") val type: String,
    @SerialName("status") val status: String,
    @SerialName("latency_ms") val latencyMs: Int? = null,
    @SerialName("last_checked_at") val lastCheckedAt: String? = null,
    @SerialName("traffic_kinds") val trafficKinds: List<String>? = null,
)
