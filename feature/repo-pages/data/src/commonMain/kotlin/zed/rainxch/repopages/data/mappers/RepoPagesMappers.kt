package zed.rainxch.repopages.data.mappers

import zed.rainxch.repopages.data.dto.IssueCommentDto
import zed.rainxch.repopages.data.dto.IssueDto
import zed.rainxch.repopages.data.dto.LabelDto
import zed.rainxch.repopages.data.dto.SecurityAdvisoryDto
import zed.rainxch.repopages.domain.model.AdvisorySeverity
import zed.rainxch.repopages.domain.model.IssueComment
import zed.rainxch.repopages.domain.model.IssueLabel
import zed.rainxch.repopages.domain.model.IssueState
import zed.rainxch.repopages.domain.model.RepoIssue
import zed.rainxch.repopages.domain.model.SecurityAdvisory

fun IssueDto.toRepoIssue(): RepoIssue = RepoIssue(
    number = number,
    title = title,
    state = state.toIssueState(),
    authorLogin = user?.login.orEmpty(),
    authorAvatarUrl = user?.avatarUrl,
    commentCount = comments,
    createdAt = createdAt,
    labels = labels.map { it.toIssueLabel() },
)

fun LabelDto.toIssueLabel(): IssueLabel = IssueLabel(name = name, color = color)

fun IssueCommentDto.toIssueComment(): IssueComment = IssueComment(
    authorLogin = user?.login.orEmpty(),
    authorAvatarUrl = user?.avatarUrl,
    bodyMarkdown = body.orEmpty(),
    createdAt = createdAt,
)

fun SecurityAdvisoryDto.toSecurityAdvisory(): SecurityAdvisory = SecurityAdvisory(
    ghsaId = ghsaId,
    summary = summary,
    description = description,
    severity = AdvisorySeverity.fromApi(severity),
    cveId = cveId,
    publishedAt = publishedAt,
    htmlUrl = htmlUrl,
)

fun String.toIssueState(): IssueState = when (lowercase()) {
    "closed" -> IssueState.CLOSED
    else -> IssueState.OPEN
}

fun IssueState.toApiValue(): String = when (this) {
    IssueState.OPEN -> "open"
    IssueState.CLOSED -> "closed"
}
