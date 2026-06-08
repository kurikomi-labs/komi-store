package zed.rainxch.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MirrorListResponse(
    @SerialName("mirrors") val mirrors: List<MirrorEntry>,
    @SerialName("generated_at") val generatedAt: String,
)
