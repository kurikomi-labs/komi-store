package zed.rainxch.repopages.domain.model

data class IssuesPage(
    val issues: List<RepoIssue>,
    val hasMore: Boolean,
)
