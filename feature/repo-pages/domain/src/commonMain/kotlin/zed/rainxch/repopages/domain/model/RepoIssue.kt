package zed.rainxch.repopages.domain.model

data class RepoIssue(
    val number: Int,
    val title: String,
    val state: IssueState,
    val authorLogin: String,
    val authorAvatarUrl: String?,
    val commentCount: Int,
    val createdAt: String,
    val labels: List<IssueLabel>,
)

data class IssueLabel(
    val name: String,
    val color: String,
)

enum class IssueState {
    OPEN,
    CLOSED,
}
