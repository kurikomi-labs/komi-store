package zed.rainxch.core.domain.model.account.github

import kotlinx.serialization.Serializable
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform

@Serializable
data class GithubRepoSummary(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: GithubUser,
    val description: String?,
    val defaultBranch: String,
    val htmlUrl: String,
    val stargazersCount: Int,
    val forksCount: Int,
    val language: String?,
    val topics: List<String>?,
    val releasesUrl: String,
    val updatedAt: String,
    val isFork: Boolean = false,
    val availablePlatforms: List<DiscoveryPlatform> = emptyList(),
    val downloadCount: Long = 0,
    val sourceHost: String? = null,
)
