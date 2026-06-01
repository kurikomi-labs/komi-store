package zed.rainxch.repopages.presentation.issuedetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issue_comments_section
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issue_no_body
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issue_opened_by
import zed.rainxch.repopages.domain.model.IssueComment
import zed.rainxch.repopages.domain.model.RepoIssueDetail
import zed.rainxch.repopages.presentation.components.RepoMarkdown
import zed.rainxch.repopages.presentation.components.RepoPagesError
import zed.rainxch.repopages.presentation.components.RepoPagesLoading
import zed.rainxch.repopages.presentation.components.RepoPagesTopBar

@Composable
fun IssueDetailRoot(
    owner: String,
    repo: String,
    number: Int,
    onNavigateBack: () -> Unit,
    viewModel: IssueDetailViewModel = koinViewModel { parametersOf(owner, repo, number) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    IssueDetailScreen(
        state = state,
        number = number,
        onBack = onNavigateBack,
        onRetry = viewModel::retry,
    )
}

@Composable
private fun IssueDetailScreen(
    state: IssueDetailUiState,
    number: Int,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        RepoPagesTopBar(title = "#$number", onBack = onBack)
        when {
            state.isLoading -> RepoPagesLoading()
            state.errorMessage != null -> RepoPagesError(message = state.errorMessage, onRetry = onRetry)
            state.detail != null -> IssueDetailContent(detail = state.detail)
        }
    }
}

@Composable
private fun IssueDetailContent(detail: RepoIssueDetail) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
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
                        stringResource(Res.string.repo_pages_issue_opened_by, detail.authorLogin),
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
                if (detail.bodyMarkdown.isBlank()) {
                    Text(
                        text = stringResource(Res.string.repo_pages_issue_no_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp),
                    )
                } else {
                    RepoMarkdown(
                        content = detail.bodyMarkdown,
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
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
            items(detail.comments) { comment ->
                CommentCard(comment)
            }
        }
    }
}

@Composable
private fun CommentCard(comment: IssueComment) {
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
        }
    }
}
