package zed.rainxch.feed.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.presentation.components.RepositoryCard
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.for_you_empty_subtitle
import zed.rainxch.githubstore.core.presentation.res.for_you_empty_title
import zed.rainxch.githubstore.core.presentation.res.for_you_failed_to_load
import zed.rainxch.githubstore.core.presentation.res.for_you_filter_all
import zed.rainxch.githubstore.core.presentation.res.for_you_offline
import zed.rainxch.githubstore.core.presentation.res.for_you_title
import zed.rainxch.githubstore.core.presentation.res.home_finding_repositories
import zed.rainxch.githubstore.core.presentation.res.home_retry

@Composable
fun FeedRoot(
    onNavigateToDetails: (repoId: Long) -> Unit,
    onNavigateToDeveloperProfile: (username: String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: FeedViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is FeedEvent.OnMessage -> coroutineScope.launch { snackbarHost.showSnackbar(event.message) }
            FeedEvent.OnScrollToTop -> coroutineScope.launch { listState.animateScrollToItem(0) }
        }
    }

    FeedScreen(
        state = state,
        snackbarHost = snackbarHost,
        listState = listState,
        onAction = { action ->
            when (action) {
                FeedAction.OnSearchClick -> onNavigateToSearch()
                FeedAction.OnSettingsClick -> onNavigateToSettings()
                is FeedAction.OnRepoClick -> onNavigateToDetails(action.repo.id)
                is FeedAction.OnDeveloperClick -> onNavigateToDeveloperProfile(action.username)
                else -> viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedScreen(
    state: FeedState,
    snackbarHost: SnackbarHostState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onAction: (FeedAction) -> Unit,
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current

    val reachedEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(reachedEnd, state.hasMore, state.isLoadingMore) {
        if (reachedEnd && state.hasMore && !state.isLoadingMore) {
            onAction(FeedAction.OnLoadMore)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.padding(bottom = bottomNavHeight + 16.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(modifier = Modifier.constrainedContentWidth().fillMaxSize()) {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onAction(FeedAction.OnRefresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when {
                        state.isLoading && state.repos.isEmpty() -> LoadingState()

                        state.errorMessage != null && state.repos.isEmpty() ->
                            ErrorState(
                                message = state.errorMessage,
                                onRetry = { onAction(FeedAction.OnRetry) },
                            )

                        else -> FeedList(
                            state = state,
                            listState = listState,
                            bottomNavHeight = bottomNavHeight,
                            onAction = onAction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedList(
    state: FeedState,
    listState: androidx.compose.foundation.lazy.LazyListState,
    bottomNavHeight: androidx.compose.ui.unit.Dp,
    onAction: (FeedAction) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomNavHeight + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "for_you_header") {
            FeedHeader(
                onSearchClick = { onAction(FeedAction.OnSearchClick) },
                onSettingsClick = { onAction(FeedAction.OnSettingsClick) },
            )
        }

        item(key = "for_you_filters") {
            PlatformFilterRow(
                selected = state.selectedPlatform,
                onSelect = { onAction(FeedAction.OnPlatformSelected(it)) },
            )
        }

        if (state.isOffline) {
            item(key = "for_you_offline") {
                Text(
                    text = stringResource(Res.string.for_you_offline),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                )
            }
        }

        if (state.repos.isEmpty()) {
            item(key = "for_you_empty") { EmptyState() }
        } else {
            items(
                items = state.repos,
                key = { "feed_${it.repository.id}" },
            ) { card ->
                RepositoryCard(
                    discoveryRepositoryUi = card,
                    onClick = { onAction(FeedAction.OnRepoClick(card.repository)) },
                    onShareClick = { onAction(FeedAction.OnShareClick(card.repository)) },
                    onDeveloperClick = { onAction(FeedAction.OnDeveloperClick(it)) },
                    onHideClick = { onAction(FeedAction.OnHideRepository(card.repository)) },
                    onToggleSeen = {
                        if (card.isSeen) {
                            onAction(FeedAction.OnMarkAsUnseen(card.repository.id))
                        } else {
                            onAction(FeedAction.OnMarkAsSeen(card.repository))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (state.isLoadingMore) {
            item(key = "for_you_loading_more") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.for_you_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearchClick) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = stringResource(Res.string.for_you_title),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlatformFilterRow(
    selected: DiscoveryPlatform,
    onSelect: (DiscoveryPlatform) -> Unit,
) {
    val options = listOf(
        DiscoveryPlatform.All,
        DiscoveryPlatform.Android,
        DiscoveryPlatform.Windows,
        DiscoveryPlatform.Macos,
        DiscoveryPlatform.Linux,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { platform ->
            FilterChip(
                selected = platform == selected,
                onClick = { onSelect(platform) },
                label = { Text(platform.label()) },
            )
        }
    }
}

@Composable
private fun DiscoveryPlatform.label(): String = when (this) {
    DiscoveryPlatform.All -> stringResource(Res.string.for_you_filter_all)
    DiscoveryPlatform.Android -> "Android"
    DiscoveryPlatform.Windows -> "Windows"
    DiscoveryPlatform.Macos -> "macOS"
    DiscoveryPlatform.Linux -> "Linux"
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(Res.string.home_finding_repositories),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.for_you_failed_to_load),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            GhsButton(
                onClick = onRetry,
                variant = GhsButtonVariant.Primary,
            ) {
                Text(stringResource(Res.string.home_retry))
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.for_you_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(Res.string.for_you_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
