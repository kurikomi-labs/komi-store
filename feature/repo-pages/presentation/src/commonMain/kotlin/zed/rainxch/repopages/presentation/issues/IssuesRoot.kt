package zed.rainxch.repopages.presentation.issues

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.theme.LocalStatusColors
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issue_opened_by
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issues_empty_closed
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issues_empty_open
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issues_filter_closed
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issues_filter_open
import zed.rainxch.githubstore.core.presentation.res.repo_pages_issues_title
import zed.rainxch.githubstore.core.presentation.res.repo_pages_new_issue
import zed.rainxch.githubstore.core.presentation.res.repo_pages_new_issue_body_hint
import zed.rainxch.githubstore.core.presentation.res.repo_pages_new_issue_submit
import zed.rainxch.githubstore.core.presentation.res.repo_pages_new_issue_title_hint
import zed.rainxch.repopages.domain.model.IssueLabel
import zed.rainxch.repopages.domain.model.IssueState
import zed.rainxch.repopages.domain.model.RepoIssue
import zed.rainxch.repopages.presentation.components.LabelChip
import zed.rainxch.repopages.presentation.components.RepoPagesEmpty
import zed.rainxch.repopages.presentation.components.RepoPagesError
import zed.rainxch.repopages.presentation.components.RepoPagesLoading
import zed.rainxch.repopages.presentation.components.RepoPagesTopBar
import zed.rainxch.repopages.presentation.utils.parseLabelColor

@Composable
fun IssuesRoot(
    onNavigateBack: () -> Unit,
    onOpenIssue: (issueNumber: Int) -> Unit,
    viewModel: IssuesViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is IssuesEvent.OnMessage -> {
                snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    IssuesScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            when (action) {
                IssuesAction.OnBackClick -> onNavigateBack()
                is IssuesAction.OnOpenIssue -> onOpenIssue(action.issueNumber)
                else -> viewModel.onAction(action)
            }
        }
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun IssuesScreen(
    state: IssuesUiState,
    snackbarHostState: SnackbarHostState,
    onAction: (IssuesAction) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding(),
        ) {
            RepoPagesTopBar(
                title = stringResource(Res.string.repo_pages_issues_title),
                onBack = {
                    onAction(IssuesAction.OnBackClick)
                },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter == IssueState.OPEN,
                    onClick = {
                        onAction(IssuesAction.OnFilterChange(IssueState.OPEN))
                    },
                    label = { Text(stringResource(Res.string.repo_pages_issues_filter_open)) },
                )
                FilterChip(
                    selected = state.filter == IssueState.CLOSED,
                    onClick = {
                        onAction(IssuesAction.OnFilterChange(IssueState.CLOSED))
                    },
                    label = { Text(stringResource(Res.string.repo_pages_issues_filter_closed)) },
                )
            }

            when {
                state.isLoading -> RepoPagesLoading()

                state.errorMessage != null -> RepoPagesError(
                    message = state.errorMessage,
                    onRetry = {
                        onAction(IssuesAction.OnRetry)
                    },
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
                    onLoadMore = {
                        onAction(IssuesAction.OnLoadMore)
                    },
                    onOpenIssue = {
                        onAction(IssuesAction.OnOpenIssue(it))
                    },
                )
            }
        }

        if (!state.isLoading && state.errorMessage == null) {
            ExtendedFloatingActionButton(
                onClick = {
                    onAction(IssuesAction.OnOpenNewIssue)
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .systemBarsPadding()
                    .padding(16.dp),
                text = { Text(stringResource(Res.string.repo_pages_new_issue)) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (state.showNewIssueSheet) {
            NewIssueSheet(
                state = state,
                onDismiss = {
                    onAction(IssuesAction.OnDismissNewIssue)
                },
                onTitleChange = {
                    onAction(IssuesAction.OnNewIssueTitleChange(it))
                },
                onBodyChange = {
                    onAction(IssuesAction.OnNewIssueBodyChange(it))
                },
                onSubmit = {
                    onAction(IssuesAction.OnSubmitNewIssue)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewIssueSheet(
    state: IssuesUiState,
    onDismiss: () -> Unit,
    onTitleChange: (String) -> Unit,
    onBodyChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.repo_pages_new_issue),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = state.newIssueTitle,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.repo_pages_new_issue_title_hint)) },
                singleLine = true,
                enabled = !state.isCreatingIssue,
            )
            OutlinedTextField(
                value = state.newIssueBody,
                onValueChange = onBodyChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(Res.string.repo_pages_new_issue_body_hint)) },
                minLines = 4,
                enabled = !state.isCreatingIssue,
            )
            GhsButton(
                onClick = onSubmit,
                label = stringResource(Res.string.repo_pages_new_issue_submit),
                enabled = !state.isCreatingIssue && state.newIssueTitle.isNotBlank(),
                loading = state.isCreatingIssue,
                modifier = Modifier.align(Alignment.End),
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
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = state.issues,
            key = { it.issueId }
        ) { issue ->
            IssueRow(
                issue = issue,
                onClick = {
                    onOpenIssue(issue.issueId)
                }
            )
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
            val statusColors = LocalStatusColors.current
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(10.dp)
                    .background(
                        color = if (issue.state == IssueState.OPEN) {
                            statusColors.issueOpen
                        } else {
                            statusColors.issueClosed
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
                    text = "#${issue.issueId} · " +
                            stringResource(
                                Res.string.repo_pages_issue_opened_by,
                                issue.authorLogin
                            ),
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

