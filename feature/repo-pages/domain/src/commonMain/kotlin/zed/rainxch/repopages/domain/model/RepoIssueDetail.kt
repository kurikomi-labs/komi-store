package zed.rainxch.repopages.domain.model

data class RepoIssueDetail(
    val number: Int,
    val title: String,
    val state: IssueState,
    val authorLogin: String,
    val authorAvatarUrl: String?,
    val bodyMarkdown: String,
    val createdAt: String,
    val labels: List<IssueLabel>,
    val comments: List<IssueComment>,
)

data class IssueComment(
    val authorLogin: String,
    val authorAvatarUrl: String?,
    val bodyMarkdown: String,
    val createdAt: String,
)
