package zed.rainxch.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.components.ScrollbarContainer
import zed.rainxch.core.presentation.components.buttons.OutlineButton
import zed.rainxch.core.presentation.components.section.SectionHeader
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.home_finding_repositories
import zed.rainxch.githubstore.core.presentation.res.home_retry
import zed.rainxch.home.domain.model.HomeCategory
import zed.rainxch.home.presentation.components.HomeTopBar
import zed.rainxch.home.presentation.components.HotCardItem
import zed.rainxch.home.presentation.components.LeadCard
import zed.rainxch.home.presentation.components.PopularRowItem
import zed.rainxch.home.presentation.components.RepositoryActionsSheet
import zed.rainxch.home.presentation.components.StarredRowItem
import zed.rainxch.home.presentation.components.TrendingRowItem

@Composable
fun HomeRoot(
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToDetails: (repoId: Long) -> Unit,
    onNavigateToDeveloperProfile: (username: String) -> Unit,
    onNavigateToCategoryList: (HomeCategory) -> Unit,
    onNavigateToStarredRepos: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HomeEvent.OnScrollToListTop -> coroutineScope.launch { listState.animateScrollToItem(0) }
            is HomeEvent.OnMessage -> coroutineScope.launch { snackbarHost.showSnackbar(event.message) }
        }
    }

    HomeScreen(
        state = state,
        snackbarHost = snackbarHost,
        onAction = { action ->
            when (action) {
                HomeAction.OnSearchClick -> onNavigateToSearch()
                HomeAction.OnSettingsClick -> onNavigateToSettings()
                HomeAction.OnAppsClick -> onNavigateToApps()
                HomeAction.OnSeeAllHot -> onNavigateToCategoryList(HomeCategory.HOT_RELEASE)
                HomeAction.OnSeeAllTrending -> onNavigateToCategoryList(HomeCategory.TRENDING)
                HomeAction.OnSeeAllPopular -> onNavigateToCategoryList(HomeCategory.MOST_POPULAR)
                HomeAction.OnSeeAllStarred -> onNavigateToStarredRepos()
                is HomeAction.OnRepoClick -> onNavigateToDetails(action.repo.id)
                is HomeAction.OnDeveloperClick -> onNavigateToDeveloperProfile(action.username)
                else -> viewModel.onAction(action)
            }
        },
        viewModel = viewModel,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeScreen(
    state: HomeState,
    snackbarHost: SnackbarHostState,
    onAction: (HomeAction) -> Unit,
    viewModel: HomeViewModel,
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.padding(bottom = bottomNavHeight + 16.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val sectionsAreEmpty = state.lead == null && state.hot.isEmpty() &&
                state.trending.isEmpty() && state.popular.isEmpty() && state.starred.isEmpty()
            val isAnyLoading = state.isHotLoading || state.isTrendingLoading ||
                state.isPopularLoading || state.isStarredLoading

            when {
                isAnyLoading && sectionsAreEmpty -> Box(
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

                state.errorMessage != null && sectionsAreEmpty -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        OutlineButton(onClick = { onAction(HomeAction.OnRetry) }) {
                            Text(text = stringResource(Res.string.home_retry))
                        }
                    }
                }

                else -> ScrollbarContainer(
                    listState = listState,
                    enabled = LocalScrollbarEnabled.current,
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
                        item(key = "top_bar") { HomeTopBar() }

                        state.lead?.let { lead ->
                            item(key = "lead_${lead.id}") {
                                LeadCard(
                                    card = lead,
                                    onClick = { onAction(HomeAction.OnRepoClick(lead.rawRepository)) },
                                    onLongClick = { onAction(HomeAction.OnRepoLongClick(lead.id)) },
                                    modifier = Modifier.padding(vertical = 4.dp),
                                )
                            }
                        }

                        if (state.hot.isNotEmpty()) {
                            item(key = "hot_header") {
                                SectionHeader(
                                    title = "Hot releases",
                                    subCount = state.hot.size.toString(),
                                    onSeeAll = { onAction(HomeAction.OnSeeAllHot) },
                                )
                            }
                            item(key = "hot_row") {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp),
                                ) {
                                    items(items = state.hot, key = { "hot_${it.id}" }) { card ->
                                        HotCardItem(
                                            card = card,
                                            onClick = { onAction(HomeAction.OnRepoClick(card.rawRepository)) },
                                            onLongClick = { onAction(HomeAction.OnRepoLongClick(card.id)) },
                                        )
                                    }
                                }
                            }
                        }

                        if (state.trending.isNotEmpty()) {
                            item(key = "trending_header") {
                                SectionHeader(
                                    title = "Trending now",
                                    subCount = state.trending.size.toString(),
                                    onSeeAll = { onAction(HomeAction.OnSeeAllTrending) },
                                )
                            }
                            itemsIndexed(
                                items = state.trending,
                                key = { _, card -> "trending_${card.id}" },
                            ) { index, card ->
                                TrendingRowItem(
                                    card = card,
                                    rank = index + 1,
                                    onClick = { onAction(HomeAction.OnRepoClick(card.rawRepository)) },
                                    onLongClick = { onAction(HomeAction.OnRepoLongClick(card.id)) },
                                )
                            }
                        }

                        if (state.popular.isNotEmpty()) {
                            item(key = "popular_header") {
                                SectionHeader(
                                    title = "Most popular",
                                    subCount = state.popular.size.toString(),
                                    onSeeAll = { onAction(HomeAction.OnSeeAllPopular) },
                                )
                            }
                            itemsIndexed(
                                items = state.popular,
                                key = { _, card -> "popular_${card.id}" },
                            ) { index, card ->
                                PopularRowItem(
                                    card = card,
                                    rank = index + 1,
                                    onClick = { onAction(HomeAction.OnRepoClick(card.rawRepository)) },
                                    onLongClick = { onAction(HomeAction.OnRepoLongClick(card.id)) },
                                )
                            }
                        }

                        if (state.isUserSignedIn && state.starred.isNotEmpty()) {
                            item(key = "starred_header") {
                                SectionHeader(
                                    title = "From your stars",
                                    subCount = state.starred.size.toString(),
                                    onSeeAll = { onAction(HomeAction.OnSeeAllStarred) },
                                )
                            }
                            items(items = state.starred, key = { "starred_${it.id}" }) { card ->
                                StarredRowItem(
                                    card = card,
                                    onClick = { onAction(HomeAction.OnRepoClick(card.rawRepository)) },
                                    onLongClick = { onAction(HomeAction.OnRepoLongClick(card.id)) },
                                )
                            }
                        }
                    }
                }
            }

            state.actionSheetCard?.let { card ->
                RepositoryActionsSheet(
                    repository = card.rawRepository,
                    isSeen = card.isSeen,
                    onDismiss = { onAction(HomeAction.OnActionSheetDismiss) },
                    onShare = {
                        onAction(HomeAction.OnActionSheetDismiss)
                        onAction(HomeAction.OnShareClick(card.rawRepository))
                    },
                    onOpenOnGithub = {
                        onAction(HomeAction.OnActionSheetDismiss)
                        uriHandler.openUri(card.rawRepository.htmlUrl)
                    },
                    onToggleSeen = {
                        onAction(HomeAction.OnActionSheetDismiss)
                        if (card.isSeen) {
                            onAction(HomeAction.OnMarkAsUnseen(card.id))
                        } else {
                            onAction(HomeAction.OnMarkAsSeen(card.rawRepository))
                        }
                    },
                    onHide = {
                        onAction(HomeAction.OnActionSheetDismiss)
                        onAction(HomeAction.OnHideRepository(card.rawRepository))
                    },
                )
            }
        }
    }
}
