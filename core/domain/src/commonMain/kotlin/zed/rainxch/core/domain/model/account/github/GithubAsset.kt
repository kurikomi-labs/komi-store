package zed.rainxch.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GithubAsset(
    val id: Long,
    val name: String,
    val contentType: String,
    val size: Long,
    val downloadUrl: String,
    val uploader: GithubUser? = null,
    val downloadCount: Long = 0,
    val digest: String? = null,
)
