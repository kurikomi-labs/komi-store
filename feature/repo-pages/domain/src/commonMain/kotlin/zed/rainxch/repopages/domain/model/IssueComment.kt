package zed.rainxch.repopages.domain.model

data class IssueComment(
    val id: Long,
    val authorLogin: String,
    val authorAvatarUrl: String?,
    val bodyMarkdown: String,
    val createdAt: String,
    val reactionThumbsUp: Int = 0,
)
