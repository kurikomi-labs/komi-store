package zed.rainxch.search.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.components.cards.DiscoveryRepoCard
import zed.rainxch.core.presentation.components.ScrollbarContainer
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.core.presentation.personality.utils.PersonalityPreview
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.search.presentation.components.LanguageFilterBottomSheet
import zed.rainxch.search.presentation.components.SearchHistorySection
import zed.rainxch.search.presentation.components.SortByBottomSheet
import zed.rainxch.search.presentation.model.ParsedGithubLink
import zed.rainxch.search.presentation.model.ProgrammingLanguageUi
import zed.rainxch.search.presentation.model.SearchPlatformUi
import zed.rainxch.search.presentation.model.SearchSourceUi
import zed.rainxch.search.presentation.model.SortByUi
import zed.rainxch.search.presentation.utils.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchRoot(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (repoId: Long, sourceHost: String?) -> Unit,
    onNavigateToDetailsFromLink: (owner: String, repo: String) -> Unit,
    onNavigateToDeveloperProfile: (username: String) -> Unit,
    viewModel: SearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SearchEvent.OnMessage -> {
                scope.launch {
                    snackbarHost.showSnackbar(event.message)
                }
            }

            is SearchEvent.NavigateToRepo -> {
                onNavigateToDetailsFromLink(event.owner, event.repo)
            }
        }
    }

    SearchScreen(
        state = state,
        snackbarHost = snackbarHost,
        onAction = { action ->
            when (action) {
                is SearchAction.OnRepositoryClick -> {
                    onNavigateToDetails(action.repository.id, action.repository.sourceHost)
                }

                SearchAction.OnNavigateBackClick -> {
                    onNavigateBack()
                }

                is SearchAction.OnRepositoryDeveloperClick -> {
                    onNavigateToDeveloperProfile(action.username)
                }

                else -> {
                    viewModel.onAction(action)
                }
            }
        },
    )

    if (state.isFiltersSheetVisible) {
        zed.rainxch.search.presentation.components.SearchFiltersSheet(
            selectedSource = state.selectedSource,
            availableSources = state.availableSources,
            selectedPlatform = state.selectedSearchPlatform,
            selectedLanguage = state.selectedLanguage,
            selectedSortBy = state.selectedSortBy,
            onSourceSelected = { viewModel.onAction(SearchAction.OnSourceSelected(it)) },
            onPlatformSelected = { viewModel.onAction(SearchAction.OnPlatformTypeSelected(it)) },
            onOpenLanguagePicker = {
                viewModel.onAction(SearchAction.OnToggleFiltersSheet)
                viewModel.onAction(SearchAction.OnToggleLanguageSheetVisibility)
            },
            onOpenSortPicker = {
                viewModel.onAction(SearchAction.OnToggleFiltersSheet)
                viewModel.onAction(SearchAction.OnToggleSortByDialogVisibility)
            },
            onReset = {
                viewModel.onAction(SearchAction.OnLanguageSelected(ProgrammingLanguageUi.All))
                viewModel.onAction(SearchAction.OnPlatformTypeSelected(SearchPlatformUi.All))
                viewModel.onAction(SearchAction.OnSortBySelected(SortByUi.BestMatch))
            },
            onDismiss = {
                viewModel.onAction(SearchAction.OnToggleFiltersSheet)
            },
        )
    }

    if (state.isLanguageSheetVisible) {
        LanguageFilterBottomSheet(
            selectedLanguage = state.selectedLanguage,
            onLanguageSelected = { language ->
                viewModel.onAction(SearchAction.OnLanguageSelected(language))
            },
            onDismissRequest = {
                viewModel.onAction(SearchAction.OnToggleLanguageSheetVisibility)
            },
        )
    }

    if (state.isSortByDialogVisible) {
        SortByBottomSheet(
            selectedSortBy = state.selectedSortBy,
            selectedSortOrder = state.selectedSortOrder,
            onSortBySelected = { sortBy ->
                viewModel.onAction(SearchAction.OnSortBySelected(sortBy))
            },
            onSortOrderSelected = { sortOrder ->
                viewModel.onAction(SearchAction.OnSortOrderSelected(sortOrder))
            },
            onDismissRequest = {
                viewModel.onAction(SearchAction.OnToggleSortByDialogVisibility)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    state: SearchState,
    snackbarHost: SnackbarHostState,
    onAction: (SearchAction) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val listState = rememberLazyStaggeredGridState()
    val bottomNavHeight = LocalBottomNavigationHeight.current

    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItems = layoutInfo.visibleItemsInfo

            if (totalItems == 0 ||
                state.isLoadingMore ||
                state.isLoading ||
                !state.hasMorePages
            ) {
                return@derivedStateOf false
            }

            val lastVisibleItem = visibleItems.lastOrNull() ?: return@derivedStateOf false
            val viewportEndOffset = layoutInfo.viewportEndOffset

            val hasEmptySpaceAtBottom =
                lastVisibleItem.index == totalItems - 1 &&
                        lastVisibleItem.offset.y + lastVisibleItem.size.height < viewportEndOffset

            val threshold = (totalItems * 0.8f).toInt()
            val isNearEnd = lastVisibleItem.index >= threshold

            isNearEnd || hasEmptySpaceAtBottom
        }
    }

    val currentOnAction by rememberUpdatedState(onAction)

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            currentOnAction(SearchAction.LoadMore)
        }
    }

    LaunchedEffect(
        state.repositories.size,
        state.visibleRepos.size,
        state.isHideSeenEnabled,
        state.hasMorePages,
        state.isLoadingMore,
        state.isLoading,
    ) {
        if (state.repositories.isNotEmpty() &&
            state.visibleRepos.isEmpty() &&
            state.isHideSeenEnabled &&
            state.hasMorePages &&
            !state.isLoadingMore &&
            !state.isLoading
        ) {
            currentOnAction(SearchAction.LoadMore)
        }
    }

    LaunchedEffect(listState.layoutInfo.totalItemsCount, listState.layoutInfo.viewportEndOffset) {
        val layoutInfo = listState.layoutInfo
        val visibleItems = layoutInfo.visibleItemsInfo
        val lastVisible = visibleItems.lastOrNull()

        if (lastVisible != null &&
            layoutInfo.totalItemsCount > 0 &&
            !state.isLoadingMore &&
            !state.isLoading &&
            state.hasMorePages
        ) {
            val hasEmptySpace =
                lastVisible.index == layoutInfo.totalItemsCount - 1 &&
                        lastVisible.offset.y + lastVisible.size.height < layoutInfo.viewportEndOffset

            if (hasEmptySpace) {
                delay(100)
                currentOnAction(SearchAction.LoadMore)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (state.query.isEmpty()) {
            focusRequester.requestFocus()
        }
    }

    KomiScaffold(
        topBar = {
            SearchTopbar(
                onAction = onAction,
                state = state,
                focusRequester = focusRequester,
            )
        },
        overlay = {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                SnackbarHost(
                    hostState = snackbarHost,
                    modifier = Modifier.padding(bottom = bottomNavHeight + 16.dp),
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    onAction(SearchAction.OnFabClick)
                },
                modifier = Modifier.padding(bottom = bottomNavHeight + 16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )

                    Text(
                        text = stringResource(Res.string.open_github_link),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
        Column(
            modifier =
                Modifier
                    .constrainedContentWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
        ) {

            AnimatedVisibility(
                visible = state.isClipboardBannerVisible && state.clipboardLinks.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                ClipboardBanner(
                    links = state.clipboardLinks,
                    onOpenLink = { link ->
                        onAction(SearchAction.OpenGithubLink(link.owner, link.repo))
                    },
                    onDismiss = {
                        onAction(SearchAction.DismissClipboardBanner)
                    },
                )
            }

            AnimatedVisibility(
                visible = state.detectedLinks.isNotEmpty(),
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
            ) {
                DetectedLinksSection(
                    links = state.detectedLinks,
                    onOpenLink = { link ->
                        onAction(SearchAction.OpenGithubLink(link.owner, link.repo))
                    },
                )
            }

            ActiveFiltersStrip(state = state, onAction = onAction)

            Spacer(Modifier.height(6.dp))

            if (state.totalCount != null) {
                Text(
                    text =
                        stringResource(
                            Res.string.results_found,
                            state.totalCount,
                        ),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                )
            }

            if (state.query.isBlank() &&
                state.repositories.isEmpty() &&
                state.recentSearches.isNotEmpty() &&
                !state.isLoading
            ) {
                SearchHistorySection(
                    recentSearches = state.recentSearches,
                    onHistoryItemClick = { query ->
                        onAction(SearchAction.OnHistoryItemClick(query))
                    },
                    onRemoveItem = { query ->
                        onAction(SearchAction.OnRemoveHistoryItem(query))
                    },
                    onClearAll = {
                        onAction(SearchAction.OnClearAllHistory)
                    },
                )
            }

            Box(Modifier.fillMaxSize()) {
                if (state.isLoading && state.repositories.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().imePadding(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularWavyProgressIndicator()
                    }
                }

                if (state.errorMessage != null && state.repositories.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.errorMessage,
                            )

                            Spacer(Modifier.height(8.dp))

                            KomiButton(
                                label = stringResource(Res.string.retry),
                                onClick = {
                                    onAction(SearchAction.Retry)
                                },
                            )
                        }
                    }
                }

                if (!state.isLoading &&
                    !state.isLoadingMore &&
                    state.errorMessage == null &&
                    state.repositories.isEmpty() &&
                    state.query.isNotBlank() &&
                    !state.hasMorePages
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(Res.string.no_repositories_found))

                            if (state.passthroughAttempted != true) {
                                Spacer(Modifier.height(8.dp))
                                ExploreFromGithubButton(
                                    status = state.exploreStatus,
                                    onExplore = { onAction(SearchAction.ExploreFromGithub) },
                                )
                            }
                        }
                    }
                }

                if (state.repositories.isNotEmpty() &&
                    state.visibleRepos.isEmpty() &&
                    state.isHideSeenEnabled &&
                    state.hasMorePages
                ) {
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
                }

                if (state.repositories.isNotEmpty() &&
                    state.visibleRepos.isEmpty() &&
                    state.isHideSeenEnabled &&
                    !state.hasMorePages
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(Res.string.search_results_hidden_by_seen_filter),
                            )

                            Spacer(Modifier.height(8.dp))

                            KomiButton(
                                label = stringResource(Res.string.show_all_results),
                                onClick = {
                                    onAction(SearchAction.OnDisableHideSeenForResults)
                                },
                            )
                        }
                    }
                }

                if (state.visibleRepos.isNotEmpty()) {
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
                                    bottom = bottomNavHeight + 88.dp,
                                ),
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .arrowKeyScroll(listState, autoFocus = false),
                        ) {
                            items(
                                items = state.visibleRepos,
                                key = { it.repository.id },
                            ) { discoveryRepository ->
                                DiscoveryRepoCard(
                                    discoveryRepositoryUi = discoveryRepository,
                                    onClick = {
                                        onAction(SearchAction.OnRepositoryClick(discoveryRepository.repository))
                                    },
                                    onDeveloperClick = { username ->
                                        onAction(SearchAction.OnRepositoryDeveloperClick(username))
                                    },
                                    onShareClick = {
                                        onAction(SearchAction.OnShareClick(discoveryRepository.repository))
                                    },
                                    onHideClick = {
                                        onAction(SearchAction.OnHideRepository(discoveryRepository.repository))
                                    },
                                    onToggleSeen = {
                                        if (discoveryRepository.isSeen) {
                                            onAction(SearchAction.OnMarkAsUnseen(discoveryRepository.repository.id))
                                        } else {
                                            onAction(SearchAction.OnMarkAsSeen(discoveryRepository.repository))
                                        }
                                    },
                                    modifier = Modifier.animateItem(),
                                )
                            }

                            item {
                                if (state.isLoadingMore) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            }

                            if (!state.isLoading && !state.isLoadingMore && state.query.isNotBlank()) {
                                item {
                                    ExploreFromGithubButton(
                                        status = state.exploreStatus,
                                        onExplore = { onAction(SearchAction.ExploreFromGithub) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun ClipboardBanner(
    links: ImmutableList<ParsedGithubLink>,
    onOpenLink: (ParsedGithubLink) -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.clipboard_link_detected),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp).clip(CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.dismiss),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            links.forEach { link ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenLink(link) }
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${link.owner}/${link.repo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(Res.string.open_in_app),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetectedLinksSection(
    links: ImmutableList<ParsedGithubLink>,
    onOpenLink: (ParsedGithubLink) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
    ) {
        Text(
            text = stringResource(Res.string.detected_links),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        links.forEach { link ->
            androidx.compose.material3.Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                onClick = { onOpenLink(link) },
                shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${link.owner}/${link.repo}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(Res.string.open_in_app),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchTopbar(
    onAction: (SearchAction) -> Unit,
    state: SearchState,
    focusRequester: FocusRequester,
) {
    val activeFilterCount = activeFilterCount(state)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TextField(
            value = state.query,
            onValueChange = { value ->
                onAction(SearchAction.OnSearchChange(value))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            trailingIcon = if (state.query.isNotEmpty()) {
                {
                    IconButton(
                        onClick = { onAction(SearchAction.OnClearClick) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(Res.string.dismiss),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else null,
            placeholder = {
                Text(
                    text = stringResource(Res.string.search_repositories_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    softWrap = false,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            textStyle =
                MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
            keyboardOptions =
                KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search,
                ),
            keyboardActions =
                KeyboardActions(
                    onSearch = { onAction(SearchAction.OnSearchImeClick) },
                    onDone = { onAction(SearchAction.OnSearchImeClick) },
                    onGo = { onAction(SearchAction.OnSearchImeClick) },
                    onNext = { onAction(SearchAction.OnSearchImeClick) },
                    onSend = { onAction(SearchAction.OnSearchImeClick) },
                ),
            singleLine = true,
            colors =
                TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            shape = RoundedCornerShape(50),
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
        )

        FiltersPillButton(
            activeCount = activeFilterCount,
            onClick = { onAction(SearchAction.OnToggleFiltersSheet) },
        )
    }
}

private fun activeFilterCount(state: SearchState): Int {
    var count = 0
    if (state.selectedSource != SearchSourceUi.GitHub) count++
    if (state.selectedSearchPlatform != SearchPlatformUi.All) count++
    if (state.selectedLanguage != ProgrammingLanguageUi.All) count++
    if (state.selectedSortBy != SortByUi.BestMatch) count++
    return count
}

@Composable
private fun FiltersPillButton(
    activeCount: Int,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val container =
        if (activeCount > 0) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceContainerLow
    val content =
        if (activeCount > 0) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(shape)
            .background(container, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Default.FilterList,
            contentDescription = stringResource(Res.string.search_filters_button),
            modifier = Modifier.size(18.dp),
            tint = content,
        )
        if (activeCount > 0) {
            Text(
                text = activeCount.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = content,
            )
        }
    }
}

@Composable
private fun ActiveFiltersStrip(
    state: SearchState,
    onAction: (SearchAction) -> Unit,
) {
    val items = buildList<Triple<String, () -> Unit, androidx.compose.ui.graphics.vector.ImageVector?>> {
        if (state.selectedSource != SearchSourceUi.GitHub) {
            add(Triple(state.selectedSource.label, { onAction(SearchAction.OnSourceSelected(SearchSourceUi.GitHub)) }, null))
        }
        if (state.selectedSearchPlatform != SearchPlatformUi.All) {
            add(
                Triple(
                    state.selectedSearchPlatform.name.lowercase().replaceFirstChar { it.uppercase() },
                    { onAction(SearchAction.OnPlatformTypeSelected(SearchPlatformUi.All)) },
                    null,
                ),
            )
        }
        if (state.selectedLanguage != ProgrammingLanguageUi.All) {
            add(
                Triple(
                    "${state.selectedLanguage}",
                    { onAction(SearchAction.OnLanguageSelected(ProgrammingLanguageUi.All)) },
                    Icons.Outlined.KeyboardArrowDown,
                ),
            )
        }
        if (state.selectedSortBy != SortByUi.BestMatch) {
            add(
                Triple(
                    "${state.selectedSortBy}",
                    { onAction(SearchAction.OnSortBySelected(SortByUi.BestMatch)) },
                    Icons.AutoMirrored.Filled.Sort,
                ),
            )
        }
    }
    if (items.isEmpty()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { (label, onRemove, leading) ->
            ActiveFilterChip(label = label, leadingIcon = leading, onRemove = onRemove)
        }
    }
}

@Composable
private fun ActiveFilterChip(
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector?,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), shape)
            .clickable(onClick = onRemove)
            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(Res.string.search_clear_filter_cd),
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun ExploreFromGithubButton(
    status: SearchState.ExploreStatus,
    onExplore: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (status) {
            SearchState.ExploreStatus.IDLE -> {
                KomiButton(
                    onClick = onExplore,
                    label = stringResource(Res.string.fetch_more_from_github),
                    variant = KomiButtonVariant.Outline,
                    leadingIcon = Icons.Outlined.TravelExplore,
                )
            }

            SearchState.ExploreStatus.LOADING -> {
                KomiButton(
                    onClick = {},
                    label = stringResource(Res.string.fetching_from_github),
                    variant = KomiButtonVariant.Outline,
                    enabled = false,
                    loading = true,
                )
            }

            SearchState.ExploreStatus.EXHAUSTED -> {
                Text(
                    text = stringResource(Res.string.no_more_github_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    PersonalityPreview {
        SearchScreen(
            state = SearchState(),
            snackbarHost = SnackbarHostState(),
            onAction = {},
        )
    }
}
