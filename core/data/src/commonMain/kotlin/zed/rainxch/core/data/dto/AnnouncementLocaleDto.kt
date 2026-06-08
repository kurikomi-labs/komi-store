package zed.rainxch.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementLocaleDto(
    @SerialName("title") val title: String? = null,
    @SerialName("body") val body: String? = null,
    @SerialName("ctaUrl") val ctaUrl: String? = null,
    @SerialName("ctaLabel") val ctaLabel: String? = null,
)
