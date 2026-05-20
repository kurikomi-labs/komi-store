package zed.rainxch.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GithubReadmeResponseDto(
    @SerialName("name") val name: String? = null,
    @SerialName("path") val path: String? = null,
    // Directory-listing entries (Forgejo `/contents/` with no filepath)
    // have no `content` of their own — the field is omitted. Make it
    // nullable so the same DTO can be reused for both single-file and
    // listing responses.
    @SerialName("content") val content: String? = null,
    @SerialName("encoding") val encoding: String? = null,
    @SerialName("type") val type: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null,
)
