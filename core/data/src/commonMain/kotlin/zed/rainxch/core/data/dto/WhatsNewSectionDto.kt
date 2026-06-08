package zed.rainxch.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WhatsNewSectionDto(
    @SerialName("type") val type: String,
    @SerialName("bullets") val bullets: List<String>,
)
