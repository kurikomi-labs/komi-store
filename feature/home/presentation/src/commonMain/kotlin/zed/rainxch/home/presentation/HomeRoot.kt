package zed.rainxch.home.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.presentation.components.GithubStoreButton
import zed.rainxch.core.presentation.components.RepositoryCard
import zed.rainxch.core.presentation.components.ScrollbarContainer
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.core.presentation.utils.toIcons
import zed.rainxch.core.presentation.utils.toLabel
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.home.domain.model.HomeCategory
import zed.rainxch.home.domain.model.TopicCategory
import zed.rainxch.home.presentation.components.LiquidGlassCategoryChips
import zed.rainxch.home.presentation.utils.displayText
import zed.rainxch.home.presentation.utils.icon

@Composable
fun HomeRoot(
    onNavigateToSettings: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToApps: () -> Unit,
    onNavigateToDetails: (repoId: Long) -> Unit,
    onNavigateToDeveloperProfile: (username: String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyStaggeredGridState()
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            HomeEvent.OnScrollToListTop -> {
                scope.launch {
                    listState.animateScrollToItem(0)
                }
            }

            is HomeEvent.OnMessage -> {
                scope.launch {
                    snackbarHost.showSnackbar(event.message)
                }
            }
        }
    }

    HomeScreen(
        state = state,
        snackbarHost = snackbarHost,
        onAction = { action ->
            when (action) {
                HomeAction.OnSearchClick -> {
                    onNavigateToSearch()
                }

                HomeAction.OnSettingsClick -> {
                    onNavigateToSettings()
                }

                HomeAction.OnAppsClick -> {
                    onNavigateToApps()
                }

                is HomeAction.OnRepositoryClick -> {
                    onNavigateToDetails(action.repo.id)
                }

                is HomeAction.OnRepositoryDeveloperClick -> {
                    onNavigateToDeveloperProfile(action.username)
                }

                else -> {
                    viewModel.onAction(action)
                }
            }
        },
        listState = listState,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    state: HomeState,
    snackbarHost: SnackbarHostState,
    onAction: (HomeAction) -> Unit,
    listState: LazyStaggeredGridState,
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()

            totalItems > 0 &&
                lastVisibleItem != null &&
                lastVisibleItem.index >= (totalItems - 5) &&
                !state.isLoadingMore &&
                !state.isLoading &&
                state.hasMorePages
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onAction(HomeAction.LoadMore)
        }
    }

    // Material 3's enter-always behavior: heightOffset ticks 1:1 with scroll
    // delta (finger-speed tracking), snaps/flings settle naturally, and any
    // upward scroll re-reveals the header instantly. See #440.
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHost,
                modifier = Modifier.padding(bottom = bottomNavHeight + 16.dp),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 8.dp),
        ) {
            CollapsibleHeader(scrollBehavior = scrollBehavior) {
                HomeTopAppBar(
                    selectedPlatforms = state.selectedPlatforms,
                    onTogglePlatformPopup = {
                        onAction(HomeAction.OnTogglePlatformPopup)
                    },
                )

                FilterChips(state, onAction)

                TopicChips(
                    selectedTopics = state.selectedTopics,
                    onTopicSelected = { topic ->
                        onAction(HomeAction.SwitchTopic(topic))
                    },
                )

                Spacer(modifier = Modifier.height(4.dp))
            }

            Box(Modifier.fillMaxSize()) {
                LoadingState(state)

                ErrorState(state, onAction)

                MainState(
                    state = state,
                    listState = listState,
                    onAction = onAction,
                )
            }
        }

        // Popup is hoisted out of the TopAppBar so its parent's
        // `LayoutCoordinates` are the (stable) Scaffold root rather
        // than the (resizing) icons row. Without this, every icon
        // count change during multi-select toggles the parent layout
        // pass and the Popup window briefly tears down and re-creates.
        if (state.isPlatformPopupVisible) {
            PlatformsPopup(
                onTogglePlatformPopup = {
                    onAction(HomeAction.OnTogglePlatformPopup)
                },
                onTogglePlatform = {
                    onAction(HomeAction.TogglePlatform(it))
                },
                onSelectAllPlatforms = {
                    onAction(HomeAction.OnSelectAllPlatforms)
                },
                selectedPlatforms = state.selectedPlatforms,
            )
        }
    }
}

// Custom-layout analogue of Material 3's `TopAppBarLayout`. Measures children
// at their natural size, reports a shrunk height (natural + heightOffset) so
// siblings in the outer Column reflow upward, and translates the children by
// the same offset so the header slides rather than collapsing contents onto
// each other. `heightOffsetLimit` is synced to the measured natural height so
// `scrollBehavior` knows the full collapsible range.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsibleHeader(
    scrollBehavior: TopAppBarScrollBehavior,
    content: @Composable () -> Unit,
) {
    var naturalHeightPx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(naturalHeightPx) {
        if (naturalHeightPx > 0f) {
            scrollBehavior.state.heightOffsetLimit = -naturalHeightPx
        }
    }

    Layout(
        content = content,
        modifier = Modifier.clipToBounds(),
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0, maxHeight = Int.MAX_VALUE)
        val placeables = measurables.map { it.measure(loose) }
        val width =
            placeables.maxOfOrNull { it.width }
                ?.coerceAtLeast(constraints.minWidth)
                ?: constraints.minWidth
        val natural = placeables.sumOf { it.height }
        naturalHeightPx = natural.toFloat()

        val offset = scrollBehavior.state.heightOffset.toInt().coerceIn(-natural, 0)
        val rendered = (natural + offset).coerceAtLeast(0)

        layout(width, rendered) {
            var y = offset
            placeables.forEach { p ->
                p.place(0, y)
                y += p.height
            }
        }
    }
}

@Composable
private fun TopicChips(
    selectedTopics: Set<TopicCategory>,
    onTopicSelected: (TopicCategory) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TopicCategory.entries.forEach { topic ->
            val isSelected = topic in selectedTopics

            val containerColor by animateColorAsState(
                targetValue =
                    if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                animationSpec = tween(250),
                label = "topicChipContainer",
            )

            val labelColor by animateColorAsState(
                targetValue =
                    if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                animationSpec = tween(250),
                label = "topicChipLabel",
            )

            FilterChip(
                selected = isSelected,
                onClick = { onTopicSelected(topic) },
                label = {
                    Text(
                        text = topic.displayText(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = topic.icon(),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                colors =
                    FilterChipDefaults.filterChipColors(
                        containerColor = containerColor,
                        labelColor = labelColor,
                        iconColor = labelColor,
                        selectedContainerColor = containerColor,
                        selectedLabelColor = labelColor,
                        selectedLeadingIconColor = labelColor,
                    ),
                border =
                    FilterChipDefaults.filterChipBorder(
                        borderColor = Color.Transparent,
                        selectedBorderColor = Color.Transparent,
                        enabled = true,
                        selected = isSelected,
                    ),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

@Composable
private fun MainState(
    state: HomeState,
    listState: LazyStaggeredGridState,
    onAction: (HomeAction) -> Unit,
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current
    val visibleRepos by remember(
        state.repos,
        state.isHideSeenEnabled,
        state.seenRepoIds,
        state.hiddenRepoIds,
    ) {
        derivedStateOf {
            val hidden = state.hiddenRepoIds
            val needsHideSeen = state.isHideSeenEnabled && state.seenRepoIds.isNotEmpty()
            if (hidden.isEmpty() && !needsHideSeen) {
                state.repos
            } else {
                state.repos.filter { repo ->
                    repo.repository.id !in hidden &&
                        (!needsHideSeen || repo.repository.id !in state.seenRepoIds)
                }
            }
        }
    }

    val isHideSeenFilteringAll =
        state.repos.isNotEmpty() &&
            visibleRepos.isEmpty() &&
            state.isHideSeenEnabled

    LaunchedEffect(
        state.repos.size,
        visibleRepos.size,
        state.isHideSeenEnabled,
        state.hasMorePages,
        state.isLoadingMore,
        state.isLoading,
    ) {
        if (isHideSeenFilteringAll &&
            state.hasMorePages &&
            !state.isLoadingMore &&
            !state.isLoading
        ) {
            onAction(HomeAction.LoadMore)
        }
    }

    if (visibleRepos.isNotEmpty()) {
        val isScrollbarEnabled = LocalScrollbarEnabled.current
        ScrollbarContainer(
            gridState = listState,
            enabled = isScrollbarEnabled,
            modifier = Modifier.fillMaxSize(),
        ) {
        LazyVerticalStaggeredGrid(
            state = listState,
            columns = StaggeredGridCells.Adaptive(350.dp),
            verticalItemSpacing = 12.dp,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding =
                PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 12.dp,
                    bottom = bottomNavHeight + 32.dp,
                ),
            modifier = Modifier.fillMaxSize().arrowKeyScroll(listState, autoFocus = true),
        ) {
            items(
                items = visibleRepos,
                key = { it.repository.id },
                contentType = { "repo" },
            ) { discoveryRepository ->
                RepositoryCard(
                    discoveryRepositoryUi = discoveryRepository,
                    onClick = {
                        onAction(HomeAction.OnRepositoryClick(discoveryRepository.repository))
                    },
                    onDeveloperClick = { username ->
                        onAction(HomeAction.OnRepositoryDeveloperClick(username))
                    },
                    onShareClick = {
                        onAction(HomeAction.OnShareClick(discoveryRepository.repository))
                    },
                    onHideClick = {
                        onAction(HomeAction.OnHideRepository(discoveryRepository.repository))
                    },
                    onToggleSeen = {
                        if (discoveryRepository.isSeen) {
                            onAction(HomeAction.OnMarkAsUnseen(discoveryRepository.repository.id))
                        } else {
                            onAction(HomeAction.OnMarkAsSeen(discoveryRepository.repository))
                        }
                    },
                    modifier = Modifier.animateItem(),
                )
            }

            if (state.isLoadingMore || state.isLoadingTopicSupplement) {
                item(key = "loading_indicator") {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = stringResource(Res.string.home_loading_more),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            if (!state.hasMorePages && !state.isLoadingMore && !state.isLoadingTopicSupplement) {
                item(key = "end_message") {
                    Text(
                        text = stringResource(Res.string.home_no_more_repositories),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        } // ScrollbarContainer
    } else if (isHideSeenFilteringAll && state.hasMorePages) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.searching_for_unseen_repos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    } else if (isHideSeenFilteringAll && !state.hasMorePages) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.search_results_hidden_by_seen_filter),
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(8.dp))

                GithubStoreButton(
                    text = stringResource(Res.string.show_all_results),
                    onClick = {
                        onAction(HomeAction.OnDisableHideSeenForResults)
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LoadingState(state: HomeState) {
    if (state.isLoading && state.repos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularWavyProgressIndicator()

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.home_finding_repositories),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ErrorState(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    if (state.errorMessage != null && state.repos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(modifier = Modifier.height(8.dp))

                GithubStoreButton(
                    text = stringResource(Res.string.home_retry),
                    onClick = {
                        onAction(HomeAction.Retry)
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterChips(
    state: HomeState,
    onAction: (HomeAction) -> Unit,
) {
    LiquidGlassCategoryChips(
        categories = HomeCategory.entries.toList(),
        selectedCategory = state.currentCategory,
        onCategorySelected = { category ->
            onAction(HomeAction.SwitchCategory(category))
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun HomeTopAppBar(
    selectedPlatforms: Set<DiscoveryPlatform>,
    onTogglePlatformPopup: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            Image(
                painter = painterResource(Res.drawable.app_icon),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Crop,
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.app_name),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(start = 4.dp),
                maxLines = 2,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        },
        actions = {
            val icons = selectedPlatformsIcons(selectedPlatforms)

            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onTogglePlatformPopup)
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                icons.forEach { icon ->
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        modifier = Modifier.padding(12.dp),
        contentPadding = PaddingValues(),
        windowInsets = WindowInsets(),
    )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlatformsPopup(
    onTogglePlatformPopup: () -> Unit,
    onTogglePlatform: (DiscoveryPlatform) -> Unit,
    onSelectAllPlatforms: () -> Unit,
    selectedPlatforms: Set<DiscoveryPlatform>,
) {
    val density = LocalDensity.current
    val positionProvider = remember(density) {
        WindowAnchoredTopEndPopupPositionProvider(
            topPaddingPx = with(density) { 80.dp.roundToPx() },
            endPaddingPx = with(density) { 16.dp.roundToPx() },
        )
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onTogglePlatformPopup,
    ) {
        Column(
            modifier =
                Modifier
                    .width(250.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            PlatformPopupRow(
                label = DiscoveryPlatform.All.toLabel(),
                isSelected = selectedPlatforms.isEmpty(),
                onClick = onSelectAllPlatforms,
            )

            DiscoveryPlatform.selectablePlatforms.forEach { platform ->
                PlatformPopupRow(
                    label = platform.toLabel(),
                    isSelected = platform in selectedPlatforms,
                    onClick = { onTogglePlatform(platform) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlatformPopupRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(
                    6.dp,
                    Alignment.Start,
                ),
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }

            Text(
                text = label,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

// Pins the popup to the window's top-end corner instead of anchoring to its
// parent's `LayoutCoordinates`. The default Compose anchor jitters when the
// parent (here, the platform-icons Row inside the TopAppBar action slot)
// resizes during multi-select toggles, causing the popup to flicker.
private class WindowAnchoredTopEndPopupPositionProvider(
    private val topPaddingPx: Int,
    private val endPaddingPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x =
            if (layoutDirection == LayoutDirection.Ltr) {
                windowSize.width - popupContentSize.width - endPaddingPx
            } else {
                endPaddingPx
            }
        return IntOffset(x.coerceAtLeast(0), topPaddingPx)
    }
}

@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        HomeScreen(
            state = HomeState(),
            onAction = {},
            snackbarHost = SnackbarHostState(),
            listState = rememberLazyStaggeredGridState(),
        )
    }
}
