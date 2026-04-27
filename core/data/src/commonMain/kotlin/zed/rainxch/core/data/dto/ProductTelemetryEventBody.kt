package zed.rainxch.core.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ProductTelemetryEventBody(
    val name: String,
    val sessionId: String,
    val timestamp: Long,
    val platform: String? = null,
    val appVersion: String? = null,
    val props: JsonObject? = null,
)

@Serializable
data class ProductTelemetryBatch(
    val events: List<ProductTelemetryEventBody>,
)
