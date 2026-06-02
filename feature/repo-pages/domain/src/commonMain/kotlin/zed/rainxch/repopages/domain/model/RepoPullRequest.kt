package zed.rainxch.repopages.domain.model

data class RepoPullRequest(
    val number: Int,
    val title: String,
    val state: PullRequestState,
    val authorLogin: String,
    val authorAvatarUrl: String?,
    val isDraft: Boolean,
    val commentCount: Int,
    val createdAt: String,
    val labels: List<IssueLabel>,
)
