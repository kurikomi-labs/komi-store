package zed.rainxch.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BackendFeedResponse(
    val items: List<BackendRepoResponse> = emptyList(),
    val page: Int = 1,
    val hasMore: Boolean = false,
    val generatedAt: String? = null,
    val rotation: String = "",
)
