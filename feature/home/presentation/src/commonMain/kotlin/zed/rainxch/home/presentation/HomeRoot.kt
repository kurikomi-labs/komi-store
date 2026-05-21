package zed.rainxch.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.presentation.components.GithubStoreButton
import zed.rainxch.core.presentation.components.ScrollbarContainer
import zed.rainxch.core.presentation.components.section.SectionHeader
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.toIcons
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.home.presentation.components.HomeTopBar
import zed.rainxch.home.presentation.components.HotCardItem
import zed.rainxch.home.presentation.components.LeadCard
import zed.rainxch.home.presentation.components.PlatformFilterMenu
import zed.rainxch.home.presentation.components.PopularRowItem
import zed.rainxch.home.presentation.components.RepositoryActionsSheet
import zed.rainxch.home.presentation.components.StarredRowItem
import zed.rainxch.home.presentation.components.TrendingRowItem

@Composable
fun HomeRoot(
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: (category: String?) -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToDetails: (repoId: Long) -> Unit,
    onNavigateToDeveloperProfile: (username: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HomeEvent.OnScrollToListTop -> {
                scope.launch { listState.animateScrollToItem(0) }
            }
            is HomeEvent.OnMessage -> {
                scope.launch { snackbarHost.showSnackbar(event.message) }
            }
        }
    }

    HomeScreen(
        state = state,
        snackbarHost = snackbarHost,
        listState = listState,
        onAction = { action ->
            when (action) {
                HomeAction.OnSearchClick -> onNavigateToSearch(null)
                HomeAction.OnSettingsClick -> onNavigateToSettings()
                HomeAction.OnAppsClick -> onNavigateToApps()
                HomeAction.OnSeeAllHot -> onNavigateToSearch("hot")
                HomeAction.OnSeeAllTrending -> onNavigateToSearch("trending")
                HomeAction.OnSeeAllPopular -> onNavigateToSearch("popular")
                HomeAction.OnSeeAllStarred -> onNavigateToSearch("starred")
                is HomeAction.OnRepoClick -> onNavigateToDetails(action.repo.id)
                is HomeAction.OnDeveloperClick -> onNavigateToDeveloperProfile(action.username)
                else -> viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    state: HomeState,
    snackbarHost: SnackbarHostState,
    listState: LazyListState,
    onAction: (HomeAction) -> Unit,
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.padding(bottom = bottomNavHeight + 16.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val uriHandler = LocalUriHandler.current
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val isAnyLoading = state.isHotLoading || state.isTrendingLoading ||
                state.isPopularLoading || state.isStarredLoading
            val sectionsAreEmpty = state.hot.isEmpty() && state.trending.isEmpty() &&
                state.popular.isEmpty() && state.starred.isEmpty()

            when {
                isAnyLoading && sectionsAreEmpty -> LoadingState()
                state.errorMessage != null && sectionsAreEmpty -> ErrorState(
                    message = state.errorMessage,
                    onRetry = { onAction(HomeAction.OnRetry) },
                )
                else -> FeedContent(
                    state = state,
                    listState = listState,
                    onAction = onAction,
                )
            }

            val actionRepo = state.actionSheetRepoId
                ?.let { findRepo(state, it) }
            if (actionRepo != null) {
                RepositoryActionsSheet(
                    repository = actionRepo.repository,
                    isSeen = actionRepo.isSeen,
                    onDismiss = { onAction(HomeAction.OnActionSheetDismiss) },
                    onShare = {
                        onAction(HomeAction.OnActionSheetDismiss)
                        onAction(HomeAction.OnShareClick(actionRepo.repository))
                    },
                    onOpenOnGithub = {
                        onAction(HomeAction.OnActionSheetDismiss)
                        uriHandler.openUri(actionRepo.repository.htmlUrl)
                    },
                    onToggleSeen = {
                        onAction(HomeAction.OnActionSheetDismiss)
                        if (actionRepo.isSeen) {
                            onAction(HomeAction.OnMarkAsUnseen(actionRepo.repository.id))
                        } else {
                            onAction(HomeAction.OnMarkAsSeen(actionRepo.repository))
                        }
                    },
                    onHide = {
                        onAction(HomeAction.OnActionSheetDismiss)
                        onAction(HomeAction.OnHideRepository(actionRepo.repository))
                    },
                )
            }
        }
    }
}

@Composable
private fun FeedContent(
    state: HomeState,
    listState: LazyListState,
    onAction: (HomeAction) -> Unit,
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current
    val visibleHot by visibleReposState(state, state.hot)
    val visibleTrending by visibleReposState(state, state.trending)
    val visiblePopular by visibleReposState(state, state.popular)
    val visibleStarred by visibleReposState(state, state.starred)

    val lead = visibleHot.firstOrNull()
    val hotTail = visibleHot.drop(1)

    val isScrollbarEnabled = LocalScrollbarEnabled.current
    ScrollbarContainer(
        listState = listState,
        enabled = isScrollbarEnabled,
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 0.dp,
                bottom = bottomNavHeight + 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "top_bar") {
                HomeTopBar(
                    onSearchClick = { onAction(HomeAction.OnSearchClick) },
                    onSettingsClick = { onAction(HomeAction.OnSettingsClick) },
                    actions = {
                        PlatformFilterAction(
                            selectedPlatforms = state.selectedPlatforms,
                            isPlatformPopupVisible = state.isPlatformPopupVisible,
                            onAction = onAction,
                        )
                    },
                )
            }

            if (lead != null) {
                item(key = "lead_${lead.repository.id}") {
                    LeadCard(
                        repo = lead,
                        onClick = { onAction(HomeAction.OnRepoClick(lead.repository)) },
                        onLongClick = { onAction(HomeAction.OnRepoLongClick(lead.repository.id)) },
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }

            if (hotTail.isNotEmpty()) {
                item(key = "hot_header") {
                    SectionHeader(
                        title = "Hot releases",
                        subCount = hotTail.size.toString(),
                        onSeeAll = { onAction(HomeAction.OnSeeAllHot) },
                    )
                }
                item(key = "hot_row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        items(items = hotTail, key = { "hot_${it.repository.id}" }) { repo ->
                            HotCardItem(
                                repo = repo,
                                onClick = { onAction(HomeAction.OnRepoClick(repo.repository)) },
                                onLongClick = { onAction(HomeAction.OnRepoLongClick(repo.repository.id)) },
                            )
                        }
                    }
                }
            }

            if (visibleTrending.isNotEmpty()) {
                item(key = "trending_header") {
                    SectionHeader(
                        title = "Trending now",
                        subCount = visibleTrending.size.toString(),
                        onSeeAll = { onAction(HomeAction.OnSeeAllTrending) },
                    )
                }
                itemsIndexed(
                    items = visibleTrending,
                    key = { _, repo -> "trending_${repo.repository.id}" },
                ) { idx, repo ->
                    TrendingRowItem(
                        repo = repo,
                        rank = idx + 1,
                        onClick = { onAction(HomeAction.OnRepoClick(repo.repository)) },
                        onLongClick = { onAction(HomeAction.OnRepoLongClick(repo.repository.id)) },
                    )
                }
            }

            if (visiblePopular.isNotEmpty()) {
                item(key = "popular_header") {
                    SectionHeader(
                        title = "Most popular",
                        subCount = visiblePopular.size.toString(),
                        onSeeAll = { onAction(HomeAction.OnSeeAllPopular) },
                    )
                }
                itemsIndexed(
                    items = visiblePopular,
                    key = { _, repo -> "popular_${repo.repository.id}" },
                ) { idx, repo ->
                    PopularRowItem(
                        repo = repo,
                        rank = idx + 1,
                        onClick = { onAction(HomeAction.OnRepoClick(repo.repository)) },
                        onLongClick = { onAction(HomeAction.OnRepoLongClick(repo.repository.id)) },
                    )
                }
            }

            if (state.isUserSignedIn && visibleStarred.isNotEmpty()) {
                item(key = "starred_header") {
                    SectionHeader(
                        title = "From your stars",
                        subCount = visibleStarred.size.toString(),
                        onSeeAll = { onAction(HomeAction.OnSeeAllStarred) },
                    )
                }
                items(items = visibleStarred, key = { "starred_${it.repository.id}" }) { repo ->
                    StarredRowItem(
                        repo = repo,
                        onClick = { onAction(HomeAction.OnRepoClick(repo.repository)) },
                        onLongClick = { onAction(HomeAction.OnRepoLongClick(repo.repository.id)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformFilterAction(
    selectedPlatforms: Set<DiscoveryPlatform>,
    isPlatformPopupVisible: Boolean,
    onAction: (HomeAction) -> Unit,
) {
    Box {
        val icons = selectedPlatformsIcons(selectedPlatforms)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onAction(HomeAction.OnPlatformPopupOpen) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            icons.forEach { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        PlatformFilterMenu(
            expanded = isPlatformPopupVisible,
            selectedPlatforms = selectedPlatforms,
            onDismiss = { onAction(HomeAction.OnPlatformPopupDismiss) },
            onSelectAll = { onAction(HomeAction.OnSelectAllPlatforms) },
            onToggle = { onAction(HomeAction.OnPlatformToggle(it)) },
        )
    }
}

@Composable
private fun selectedPlatformsIcons(
    selectedPlatforms: Set<DiscoveryPlatform>,
) = if (selectedPlatforms.isEmpty()) {
    DiscoveryPlatform.All.toIcons()
} else {
    DiscoveryPlatform.selectablePlatforms
        .filter { it in selectedPlatforms }
        .flatMap { it.toIcons() }
}

@Composable
private fun visibleReposState(
    state: HomeState,
    source: kotlinx.collections.immutable.ImmutableList<zed.rainxch.core.presentation.model.DiscoveryRepositoryUi>,
) = remember(source, state.hiddenRepoIds, state.seenRepoIds, state.isHideSeenEnabled) {
    derivedStateOf {
        val hidden = state.hiddenRepoIds
        val needsHideSeen = state.isHideSeenEnabled && state.seenRepoIds.isNotEmpty()
        if (hidden.isEmpty() && !needsHideSeen) {
            source
        } else {
            source.filter { repo ->
                repo.repository.id !in hidden &&
                    (!needsHideSeen || repo.repository.id !in state.seenRepoIds)
            }
        }
    }
}

private fun findRepo(
    state: HomeState,
    repoId: Long,
): zed.rainxch.core.presentation.model.DiscoveryRepositoryUi? {
    fun seq() = sequence {
        yieldAll(state.hot)
        yieldAll(state.trending)
        yieldAll(state.popular)
        yieldAll(state.starred)
    }
    return seq().firstOrNull { it.repository.id == repoId }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.home_finding_repositories),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            GithubStoreButton(
                text = stringResource(Res.string.home_retry),
                onClick = onRetry,
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        HomeScreen(
            state = HomeState(),
            snackbarHost = SnackbarHostState(),
            listState = rememberLazyListState(),
            onAction = {},
        )
    }
}
