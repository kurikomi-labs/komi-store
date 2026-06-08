package zed.rainxch.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementsResponseDto(
    @SerialName("version") val version: Int = 1,
    @SerialName("fetchedAt") val fetchedAt: String? = null,
    @SerialName("items") val items: List<AnnouncementDto> = emptyList(),
)
