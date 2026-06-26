package zed.rainxch.feed.domain.model

import zed.rainxch.core.domain.model.account.github.GithubRepoSummary

data class FeedPage(
    val items: List<GithubRepoSummary>,
    val page: Int,
    val hasMore: Boolean,
    val rotation: String,
    val fromCache: Boolean = false,
    val isOffline: Boolean = false,
)
