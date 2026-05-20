package zed.rainxch.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForgejoSearchResponse(
    @SerialName("ok") val ok: Boolean = true,
    @SerialName("data") val data: List<ForgejoRepoNetworkModel> = emptyList(),
)
