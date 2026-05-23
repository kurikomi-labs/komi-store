@file:OptIn(ExperimentalMaterial3Api::class)

package zed.rainxch.apps.presentation.starred

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.apps.presentation.starred.components.StarredCandidateRow
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.navigate_back
import zed.rainxch.githubstore.core.presentation.res.starred_picker_empty
import zed.rainxch.githubstore.core.presentation.res.starred_picker_filter_show_all
import zed.rainxch.githubstore.core.presentation.res.starred_picker_header_counts
import zed.rainxch.githubstore.core.presentation.res.starred_picker_no_match
import zed.rainxch.githubstore.core.presentation.res.starred_picker_progress
import zed.rainxch.githubstore.core.presentation.res.starred_picker_rate_limited
import zed.rainxch.githubstore.core.presentation.res.starred_picker_resume
import zed.rainxch.githubstore.core.presentation.res.starred_picker_search_hint
import zed.rainxch.githubstore.core.presentation.res.starred_picker_sign_in_required
import zed.rainxch.githubstore.core.presentation.res.starred_picker_sort_alphabetical
import zed.rainxch.githubstore.core.presentation.res.starred_picker_sort_recent
import zed.rainxch.githubstore.core.presentation.res.starred_picker_sort_stars
import zed.rainxch.githubstore.core.presentation.res.starred_picker_star_dedup_tooltip
import zed.rainxch.githubstore.core.presentation.res.starred_picker_title

@Composable
fun StarredPickerRoot(
    viewModel: StarredPickerViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (repoId: Long, owner: String, repo: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            StarredPickerEvent.NavigateBack -> onNavigateBack()
            is StarredPickerEvent.NavigateToDetails -> onNavigateToDetails(event.repoId, event.owner, event.repo)
        }
    }

    StarredPickerScreen(state = state, onAction = viewModel::onAction)
}

@Composable
private fun StarredPickerScreen(
    state: StarredPickerState,
    onAction: (StarredPickerAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.starred_picker_title),
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(StarredPickerAction.OnNavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.navigate_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state.phase) {
                StarredPickerState.Phase.LoadingStars -> CenteredProgress()
                StarredPickerState.Phase.Empty -> EmptyState(state.isAuthenticated)
                StarredPickerState.Phase.ScanningReleases,
                StarredPickerState.Phase.Ready,
                -> ContentBody(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun ContentBody(
    state: StarredPickerState,
    onAction: (StarredPickerAction) -> Unit,
) {
    val apkCount = state.candidates.count { it.hasApkRelease }
    val trackedCount = state.candidates.count { it.isAlreadyTracked }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(
                Res.string.starred_picker_header_counts,
                state.totalStarred,
                apkCount,
                trackedCount,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.phase == StarredPickerState.Phase.ScanningReleases) {
            Spacer(Modifier.height(8.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = {
                        if (state.scanTotal > 0) state.scanProgress.toFloat() / state.scanTotal else 0f
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        Res.string.starred_picker_progress,
                        state.scanProgress,
                        state.scanTotal,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.rateLimited) {
            Spacer(Modifier.height(8.dp))
            RateLimitedBanner(onResume = { onAction(StarredPickerAction.OnResume) })
        }

        Spacer(Modifier.height(12.dp))
        GhsTextField(
            value = state.searchQuery,
            onValueChange = { onAction(StarredPickerAction.OnSearchChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.starred_picker_search_hint),
            leadingIcon = Icons.Filled.Search,
            singleLine = true,
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.sortRule == StarredPickerSortRule.RecentlyStarred,
                onClick = { onAction(StarredPickerAction.OnSortRuleSelected(StarredPickerSortRule.RecentlyStarred)) },
                label = { Text(stringResource(Res.string.starred_picker_sort_recent)) },
                shape = RoundedCornerShape(12.dp),
            )
            FilterChip(
                selected = state.sortRule == StarredPickerSortRule.Alphabetical,
                onClick = { onAction(StarredPickerAction.OnSortRuleSelected(StarredPickerSortRule.Alphabetical)) },
                label = { Text(stringResource(Res.string.starred_picker_sort_alphabetical)) },
                shape = RoundedCornerShape(12.dp),
            )
            FilterChip(
                selected = state.sortRule == StarredPickerSortRule.MostStars,
                onClick = { onAction(StarredPickerAction.OnSortRuleSelected(StarredPickerSortRule.MostStars)) },
                label = { Text(stringResource(Res.string.starred_picker_sort_stars)) },
                shape = RoundedCornerShape(12.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.starred_picker_filter_show_all),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = state.showWithoutApk,
                onCheckedChange = { onAction(StarredPickerAction.OnToggleWithoutApk(it)) },
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.starred_picker_star_dedup_tooltip),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val visible = filterAndSort(state)
        if (visible.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.starred_picker_no_match),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = visible, key = { it.repoId }) { candidate ->
                    StarredCandidateRow(
                        candidate = candidate,
                        onClick = { onAction(StarredPickerAction.OnCandidateClick(candidate)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RateLimitedBanner(onResume: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.starred_picker_rate_limited),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onResume,
            colors = ButtonDefaults.buttonColors(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(Res.string.starred_picker_resume))
        }
    }
}

@Composable
private fun EmptyState(isAuthenticated: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val message = if (!isAuthenticated) {
            stringResource(Res.string.starred_picker_sign_in_required)
        } else {
            stringResource(Res.string.starred_picker_empty)
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CenteredProgress() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

private fun filterAndSort(state: StarredPickerState): List<StarredCandidateUi> {
    val query = state.searchQuery.trim().lowercase()
    val filtered = state.candidates.filter { candidate ->
        if (!state.showWithoutApk && !candidate.hasApkRelease) return@filter false
        if (query.isBlank()) return@filter true
        candidate.owner.lowercase().contains(query) ||
            candidate.name.lowercase().contains(query) ||
            (candidate.description?.lowercase()?.contains(query) == true)
    }
    return when (state.sortRule) {
        StarredPickerSortRule.RecentlyStarred ->
            filtered.sortedByDescending { it.starredAt ?: 0L }
        StarredPickerSortRule.Alphabetical ->
            filtered.sortedBy { "${it.owner}/${it.name}".lowercase() }
        StarredPickerSortRule.MostStars ->
            filtered.sortedByDescending { it.stargazersCount }
    }
}
