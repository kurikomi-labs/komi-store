package zed.rainxch.repopages.presentation.issuedetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.PersistentSet
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.inputs.KomiTextField
import zed.rainxch.core.presentation.personality.utils.PersonalityPreview
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.repo_pages_comment_hint
import zed.rainxch.githubstore.core.presentation.res.repo_pages_comment_send
import zed.rainxch.githubstore.core.presentation.res.repo_pages_comment_sign_in
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issue_comments_section
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issue_no_body
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issue_opened_by
import zed.rainxch.repopages.domain.model.IssueComment
import zed.rainxch.repopages.domain.model.IssueLabel
import zed.rainxch.repopages.domain.model.IssueState
import zed.rainxch.repopages.domain.model.RepoIssueDetail
import zed.rainxch.repopages.presentation.components.RepoMarkdown
import zed.rainxch.repopages.presentation.components.RepoPagesError
import zed.rainxch.repopages.presentation.components.RepoPagesLoading
import zed.rainxch.repopages.presentation.components.RepoPagesTopBar

@Composable
fun IssueDetailRoot(
    onNavigateBack: () -> Unit,
    viewModel: IssueDetailViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is IssueDetailEvent.OnMessage -> {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    IssueDetailScreen(
        state = state,
        onAction = { action ->
            when (action) {
                IssueDetailAction.OnBackClick -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
        },
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun IssueDetailScreen(
    state: IssueDetailUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (IssueDetailAction) -> Unit
) {
    Scaffold(
        topBar = {
            RepoPagesTopBar(
                title = "#${state.issueNumber}",
                onBack = {
                    onAction(IssueDetailAction.OnBackClick)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.detail != null) {
                CommentComposer(
                    text = state.commentText,
                    isLoggedIn = state.isLoggedIn,
                    isPosting = state.isPostingComment,
                    onTextChange = {
                        onAction(IssueDetailAction.OnCommentChange(it))
                    },
                    onSend = {
                        onAction(IssueDetailAction.OnPostComment)
                    },
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val content = Modifier
            .fillMaxWidth()
            .padding(innerPadding)
        when {
            state.isLoading -> RepoPagesLoading(content)
            state.errorMessage != null -> RepoPagesError(
                message = state.errorMessage,
                onRetry = { onAction(IssueDetailAction.OnRetryClick) },
                modifier = content
            )

            state.detail != null -> IssueDetailContent(
                detail = state.detail,
                isReactingIssue = state.isReactingIssue,
                reactingCommentIds = state.reactingCommentIds,
                onReactIssue = {
                    onAction(IssueDetailAction.OnReactIssue)
                },
                onReactComment = {
                    onAction(IssueDetailAction.OnReactComment(it))
                },
                modifier = content,
            )
        }
    }
}

@Composable
private fun IssueDetailContent(
    detail: RepoIssueDetail,
    isReactingIssue: Boolean,
    reactingCommentIds: PersistentSet<Long>,
    onReactIssue: () -> Unit,
    onReactComment: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = detail.title,
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "#${detail.number} · " +
                            stringResource(
                                Res.string.repo_pages_issue_opened_by,
                                detail.authorLogin
                            ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "body") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (detail.bodyMarkdown.isBlank()) {
                        Text(
                            text = stringResource(Res.string.repo_pages_issue_no_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        RepoMarkdown(
                            content = detail.bodyMarkdown,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    ThumbChip(
                        count = detail.reactionThumbsUp,
                        enabled = !isReactingIssue,
                        onClick = onReactIssue,
                    )
                }
            }
        }

        if (detail.comments.isNotEmpty()) {
            item(key = "comments_header") {
                HorizontalDivider()
                Text(
                    text = stringResource(Res.string.repo_pages_issue_comments_section),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(detail.comments, key = { it.id }) { comment ->
                CommentCard(
                    comment = comment,
                    isReacting = comment.id in reactingCommentIds,
                    onReact = { onReactComment(comment.id) },
                )
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: IssueComment,
    isReacting: Boolean,
    onReact: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = comment.authorLogin,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            RepoMarkdown(content = comment.bodyMarkdown, modifier = Modifier.fillMaxWidth())
            ThumbChip(count = comment.reactionThumbsUp, enabled = !isReacting, onClick = onReact)
        }
    }
}

@Composable
private fun ThumbChip(
    count: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "👍",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (count > 0) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommentComposer(
    text: String,
    isLoggedIn: Boolean,
    isPosting: Boolean,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            if (!isLoggedIn) {
                Text(
                    text = stringResource(Res.string.repo_pages_comment_sign_in),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    KomiTextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier.weight(1f),
                        placeholder = stringResource(Res.string.repo_pages_comment_hint),
                        multiline = true,
                        rows = 4,
                        enabled = !isPosting,
                    )

                    KomiButton(
                        onClick = onSend,
                        label = stringResource(Res.string.repo_pages_comment_send),
                        enabled = !isPosting && text.isNotBlank(),
                        loading = isPosting,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun IssueDetailScreenPreview() {
    PersonalityPreview {
        IssueDetailScreen(
            state = IssueDetailUiState(
                issueNumber = 123,
                detail = RepoIssueDetail(
                    number = 123,
                    title = "Sample Issue Title",
                    state = IssueState.OPEN,
                    authorLogin = "sampleuser",
                    authorAvatarUrl = null,
                    bodyMarkdown = "This is a sample issue body with **markdown** support.",
                    createdAt = "2023-10-27T10:00:00Z",
                    labels = listOf(
                        IssueLabel(name = "bug", color = "ff0000"),
                        IssueLabel(name = "high priority", color = "00ff00")
                    ),
                    comments = listOf(
                        IssueComment(
                            id = 1,
                            authorLogin = "commenter1",
                            authorAvatarUrl = null,
                            bodyMarkdown = "This is a comment.",
                            createdAt = "2023-10-27T11:00:00Z",
                            reactionThumbsUp = 5
                        ),
                        IssueComment(
                            id = 2,
                            authorLogin = "commenter2",
                            authorAvatarUrl = null,
                            bodyMarkdown = "Another comment with `code`.",
                            createdAt = "2023-10-27T12:00:00Z",
                            reactionThumbsUp = 2
                        )
                    ),
                    reactionThumbsUp = 10
                ),
                isLoggedIn = true
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onAction = {}
        )
    }
}
