package zed.rainxch.core.domain.model.repository
import kotlinx.serialization.Serializable
import zed.rainxch.core.domain.model.account.github.GithubRepoSummary

@Serializable
data class PaginatedDiscoveryRepositories(
    val repos: List<GithubRepoSummary>,
    val hasMore: Boolean,
    val nextPageIndex: Int,
    val totalCount: Int? = null,
    val passthroughAttempted: Boolean? = null,
)
