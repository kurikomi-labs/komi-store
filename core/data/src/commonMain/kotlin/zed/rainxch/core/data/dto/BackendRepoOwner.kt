package zed.rainxch.core.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BackendRepoOwner(
    val login: String,
    val avatarUrl: String? = null,
)
