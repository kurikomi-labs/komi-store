package zed.rainxch.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WhatsNewEntryDto(
    @SerialName("versionCode") val versionCode: Int,
    @SerialName("versionName") val versionName: String,
    @SerialName("releaseDate") val releaseDate: String,
    @SerialName("sections") val sections: List<WhatsNewSectionDto>,
    @SerialName("showAsSheet") val showAsSheet: Boolean = true,
)
