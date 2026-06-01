package zed.rainxch.repopages.presentation.issues

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issue_opened_by
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issues_empty_closed
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issues_empty_open
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issues_filter_closed
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issues_filter_open
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issues_title
import zed.rainxch.repopages.domain.model.IssueLabel
import zed.rainxch.repopages.domain.model.IssueState
import zed.rainxch.repopages.domain.model.RepoIssue
import zed.rainxch.repopages.presentation.components.RepoPagesEmpty
import zed.rainxch.repopages.presentation.components.RepoPagesError
import zed.rainxch.repopages.presentation.components.RepoPagesLoading
import zed.rainxch.repopages.presentation.components.RepoPagesTopBar

@Composable
fun IssuesRoot(
    owner: String,
    repo: String,
    onNavigateBack: () -> Unit,
    onOpenIssue: (number: Int) -> Unit,
    viewModel: IssuesViewModel = koinViewModel { parametersOf(owner, repo) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    IssuesScreen(
        state = state,
        onBack = onNavigateBack,
        onRetry = viewModel::retry,
        onFilterChange = viewModel::setFilter,
        onLoadMore = viewModel::loadNextPage,
        onOpenIssue = onOpenIssue,
    )
}

@Composable
private fun IssuesScreen(
    state: IssuesUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onFilterChange: (IssueState) -> Unit,
    onLoadMore: () -> Unit,
    onOpenIssue: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        RepoPagesTopBar(
            title = stringResource(Res.string.repo_pages_issues_title),
            onBack = onBack,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = state.filter == IssueState.OPEN,
                onClick = { onFilterChange(IssueState.OPEN) },
                label = { Text(stringResource(Res.string.repo_pages_issues_filter_open)) },
            )
            FilterChip(
                selected = state.filter == IssueState.CLOSED,
                onClick = { onFilterChange(IssueState.CLOSED) },
                label = { Text(stringResource(Res.string.repo_pages_issues_filter_closed)) },
            )
        }

        when {
            state.isLoading -> RepoPagesLoading()

            state.errorMessage != null -> RepoPagesError(
                message = state.errorMessage,
                onRetry = onRetry,
            )

            state.issues.isEmpty() -> {
                val emptyText = if (state.filter == IssueState.OPEN) {
                    stringResource(Res.string.repo_pages_issues_empty_open)
                } else {
                    stringResource(Res.string.repo_pages_issues_empty_closed)
                }
                RepoPagesEmpty(message = emptyText)
            }

            else -> IssuesList(
                state = state,
                onLoadMore = onLoadMore,
                onOpenIssue = onOpenIssue,
            )
        }
    }
}

@Composable
private fun IssuesList(
    state: IssuesUiState,
    onLoadMore: () -> Unit,
    onOpenIssue: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            state.issues.isNotEmpty() && last >= state.issues.size - 4
        }
    }
    androidx.compose.runtime.LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(state.issues, key = { it.number }) { issue ->
            IssueRow(issue = issue, onClick = { onOpenIssue(issue.number) })
        }
        if (state.isLoadingMore) {
            item(key = "loading_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun IssueRow(
    issue: RepoIssue,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(10.dp)
                    .background(
                        color = if (issue.state == IssueState.OPEN) {
                            Color(0xFF2DA44E)
                        } else {
                            Color(0xFF8957E5)
                        },
                        shape = CircleShape,
                    ),
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "#${issue.number} · " +
                        stringResource(Res.string.repo_pages_issue_opened_by, issue.authorLogin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (issue.labels.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        issue.labels.take(6).forEach { label ->
                            LabelChip(label)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LabelChip(label: IssueLabel) {
    val color = parseLabelColor(label.color)
    Box(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = label.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun parseLabelColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#").trim()
        if (clean.length == 6) Color(("FF$clean").toLong(16)) else Color(0xFF888888)
    } catch (e: Exception) {
        Color(0xFF888888)
    }
}
