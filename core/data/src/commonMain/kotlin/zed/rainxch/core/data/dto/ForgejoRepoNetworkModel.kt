package zed.rainxch.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ForgejoRepoNetworkModel(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("owner") val owner: GithubOwnerNetworkModel,
    @SerialName("description") val description: String? = null,
    @SerialName("default_branch") val defaultBranch: String? = null,
    @SerialName("html_url") val htmlUrl: String,
    @SerialName("stars_count") val starsCount: Int = 0,
    @SerialName("forks_count") val forksCount: Int = 0,
    @SerialName("language") val language: String? = null,
    @SerialName("archived") val archived: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("pushed_at") val pushedAt: String? = null,
    @SerialName("open_issues_count") val openIssuesCount: Int = 0,
)
