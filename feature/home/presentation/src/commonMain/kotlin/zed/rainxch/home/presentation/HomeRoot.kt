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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.home_finding_repositories
import zed.rainxch.githubstore.core.presentation.res.home_retry
import zed.rainxch.githubstore.core.presentation.res.home_section_from_your_stars
import zed.rainxch.githubstore.core.presentation.res.home_section_hot_releases
import zed.rainxch.githubstore.core.presentation.res.home_section_most_popular
import zed.rainxch.githubstore.core.presentation.res.home_section_trending_now
import zed.rainxch.home.domain.model.HomeCategory
import zed.rainxch.core.presentation.components.cards.DiscoveryRepoCard
import zed.rainxch.core.presentation.components.cards.KomiRepoCardFeed
import zed.rainxch.core.presentation.components.overlays.KomiToastState
import zed.rainxch.core.presentation.components.overlays.rememberKomiToastState
import zed.rainxch.githubstore.core.presentation.res.home_topbar_discover
import zed.rainxch.home.presentation.components.RepositoryActionsSheet
import zed.rainxch.home.presentation.model.toDiscoveryUi

@Composable
fun HomeRoot(
    onNavigateToDetails: (repoId: Long) -> Unit,
    onNavigateToDeveloperProfile: (username: String) -> Unit,
    onNavigateToCategoryList: (HomeCategory) -> Unit,
    onNavigateToStarredRepos: () -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val toastState = rememberKomiToastState()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HomeEvent.OnScrollToListTop -> coroutineScope.launch {
                listState.animateScrollToItem(0)
            }

            is HomeEvent.OnMessage -> coroutineScope.launch {
                toastState.show(event.message)
            }
        }
    }

    HomeScreen(
        state = state,
        toastState = toastState,
        onAction = { action ->
            when (action) {
                HomeAction.OnSeeAllHot -> onNavigateToCategoryList(HomeCategory.HOT_RELEASE)
                HomeAction.OnSeeAllTrending -> onNavigateToCategoryList(HomeCategory.TRENDING)
                HomeAction.OnSeeAllPopular -> onNavigateToCategoryList(HomeCategory.MOST_POPULAR)
                HomeAction.OnSeeAllStarred -> onNavigateToStarredRepos()
                is HomeAction.OnRepoClick -> onNavigateToDetails(action.repo.id)
                is HomeAction.OnDeveloperClick -> onNavigateToDeveloperProfile(action.username)
                else -> viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeScreen(
    state: HomeState,
    toastState: KomiToastState,
    onAction: (HomeAction) -> Unit,
) {
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    KomiScaffold(
        topBar = {
            KomiTopBar(
                title = stringResource(Res.string.home_topbar_discover),
            )
        },
        toastState = toastState,
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(modifier = Modifier.constrainedContentWidth().fillMaxSize()) {
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

                            KomiButton(
                                onClick = { onAction(HomeAction.OnRetry) },
                                label = stringResource(Res.string.home_retry),
                                variant = KomiButtonVariant.Outline,
                            )
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
                                bottom = 32.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (state.hot.isNotEmpty()) {
                                item(key = "hot_header") {
                                    KomiText(
                                        text = stringResource(Res.string.home_section_hot_releases),
                                        role = KomiTextRole.Title,
                                    )
                                }

                                item(key = "hot_row") {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp),
                                    ) {
                                        items(items = state.hot, key = { "hot_${it.id}" }) { card ->
                                            DiscoveryRepoCard(
                                                discoveryRepositoryUi = card.toDiscoveryUi(),
                                                onClick = {
                                                    onAction(HomeAction.OnRepoClick(card.rawRepository))
                                                },
                                                onShareClick = {
                                                    onAction(HomeAction.OnShareClick(card.rawRepository))
                                                },
                                                onLongPress = {
                                                    onAction(
                                                        HomeAction.OnRepoLongClick(
                                                            card.id
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.animateItem(),
                                                compact = true,
                                                feed = KomiRepoCardFeed.Release
                                            )
                                        }
                                    }
                                }
                            }

                            if (state.trending.isNotEmpty()) {
                                item(key = "trending_header") {
                                    KomiText(
                                        text = stringResource(Res.string.home_section_trending_now),
                                        role = KomiTextRole.Title,
                                    )
                                }

                                items(
                                    items = state.trending,
                                    key = { card -> "trending_${card.id}" },
                                ) { card ->
                                    DiscoveryRepoCard(
                                        discoveryRepositoryUi = card.toDiscoveryUi(),
                                        onClick = { onAction(HomeAction.OnRepoClick(card.rawRepository)) },
                                        onShareClick = { onAction(HomeAction.OnShareClick(card.rawRepository)) },
                                        onLongPress = { onAction(HomeAction.OnRepoLongClick(card.id)) },
                                        feed = KomiRepoCardFeed.Trending,
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }

                            if (state.popular.isNotEmpty()) {
                                item(key = "popular_header") {
                                    KomiText(
                                        text = stringResource(Res.string.home_section_most_popular),
                                        role = KomiTextRole.Title,
                                    )
                                }

                                itemsIndexed(
                                    items = state.popular,
                                    key = { _, card -> "popular_${card.id}" },
                                ) { index, card ->
                                    DiscoveryRepoCard(
                                        discoveryRepositoryUi = card.toDiscoveryUi(),
                                        onClick = { onAction(HomeAction.OnRepoClick(card.rawRepository)) },
                                        onShareClick = { onAction(HomeAction.OnShareClick(card.rawRepository)) },
                                        onLongPress = { onAction(HomeAction.OnRepoLongClick(card.id)) },
                                        rank = index + 1,
                                        feed = KomiRepoCardFeed.Popular,
                                        modifier = Modifier.animateItem(),
                                    )
                                }
                            }

                            if (state.isUserSignedIn && state.starred.isNotEmpty()) {
                                item(key = "starred_header") {
                                    KomiText(
                                        text = stringResource(Res.string.home_section_from_your_stars),
                                        role = KomiTextRole.Title,
                                    )
                                }

                                items(items = state.starred, key = { "starred_${it.id}" }) { card ->
                                    DiscoveryRepoCard(
                                        discoveryRepositoryUi = card.toDiscoveryUi(),
                                        onClick = { onAction(HomeAction.OnRepoClick(card.rawRepository)) },
                                        onShareClick = { onAction(HomeAction.OnShareClick(card.rawRepository)) },
                                        onLongPress = { onAction(HomeAction.OnRepoLongClick(card.id)) },
                                        modifier = Modifier.animateItem(),
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
}
