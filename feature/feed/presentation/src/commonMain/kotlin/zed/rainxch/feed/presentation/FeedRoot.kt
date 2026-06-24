package zed.rainxch.feed.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.isAndroid
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.cards.DiscoveryRepoCard
import zed.rainxch.core.presentation.components.cards.KomiRepoCardFeed
import zed.rainxch.core.presentation.components.chips.KomiChip
import zed.rainxch.core.presentation.components.chips.KomiChipKind
import zed.rainxch.core.presentation.components.dividers.KomiHorizontalDivider
import zed.rainxch.core.presentation.components.icon.KomiIcon
import zed.rainxch.core.presentation.components.overlays.KomiSheet
import zed.rainxch.core.presentation.components.overlays.KomiSheetPlacement
import zed.rainxch.core.presentation.components.overlays.KomiToastState
import zed.rainxch.core.presentation.components.overlays.rememberKomiToastState
import zed.rainxch.core.presentation.components.progress.KomiCircularProgress
import zed.rainxch.core.presentation.components.refresh.KomiPullToRefresh
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.components.surfaces.KomiSurface
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.usesDecor
import zed.rainxch.core.presentation.personality.MangaPersonality
import zed.rainxch.core.presentation.personality.model.PersonalityColors
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.core.presentation.utils.toIcon
import zed.rainxch.core.presentation.utils.toLabel
import zed.rainxch.core.domain.isDesktop
import zed.rainxch.core.domain.model.repository.FeedCategory
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.category_all
import zed.rainxch.githubstore.core.presentation.res.feed_category_ai
import zed.rainxch.githubstore.core.presentation.res.feed_category_media
import zed.rainxch.githubstore.core.presentation.res.feed_category_networking
import zed.rainxch.githubstore.core.presentation.res.feed_category_privacy
import zed.rainxch.githubstore.core.presentation.res.feed_category_reading
import zed.rainxch.githubstore.core.presentation.res.feed_category_social
import zed.rainxch.githubstore.core.presentation.res.feed_category_tools
import zed.rainxch.githubstore.core.presentation.res.feed_cd_profile
import zed.rainxch.githubstore.core.presentation.res.feed_cd_search
import zed.rainxch.githubstore.core.presentation.res.feed_empty_reset
import zed.rainxch.githubstore.core.presentation.res.feed_empty_subtitle
import zed.rainxch.githubstore.core.presentation.res.feed_empty_title
import zed.rainxch.githubstore.core.presentation.res.feed_end_cap
import zed.rainxch.githubstore.core.presentation.res.feed_failed_to_load
import zed.rainxch.githubstore.core.presentation.res.feed_fresh_for
import zed.rainxch.githubstore.core.presentation.res.feed_loading
import zed.rainxch.githubstore.core.presentation.res.feed_masthead_subtitle
import zed.rainxch.githubstore.core.presentation.res.feed_masthead_title
import zed.rainxch.githubstore.core.presentation.res.feed_masthead_title_accent
import zed.rainxch.githubstore.core.presentation.res.feed_offline
import zed.rainxch.githubstore.core.presentation.res.feed_platform_all
import zed.rainxch.githubstore.core.presentation.res.feed_platform_picker_title
import zed.rainxch.githubstore.core.presentation.res.feed_platform_picker_title_jp
import zed.rainxch.githubstore.core.presentation.res.home_retry

@Composable
fun FeedRoot(
    onNavigateToDetails: (repoId: Long) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: FeedViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastState = rememberKomiToastState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is FeedEvent.OnMessage -> toastState.show(event.message)
            FeedEvent.OnScrollToTop -> coroutineScope.launch { listState.animateScrollToItem(0) }
        }
    }

    FeedScreen(
        state = state,
        toastState = toastState,
        listState = listState,
        onAction = { action ->
            when (action) {
                FeedAction.OnSearchClick -> onNavigateToSearch()
                FeedAction.OnProfileClick -> onNavigateToProfile()
                is FeedAction.OnRepoClick -> onNavigateToDetails(action.repo.id)
                else -> viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedScreen(
    state: FeedState,
    toastState: KomiToastState,
    listState: LazyListState,
    onAction: (FeedAction) -> Unit,
) {
    val reachedEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastIndex = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastIndex >= info.totalItemsCount - 4
        }
    }

    LaunchedEffect(reachedEnd, state.hasMore, state.isLoadingMore) {
        if (reachedEnd && state.hasMore && !state.isLoadingMore) {
            onAction(FeedAction.OnLoadMore)
        }
    }

    val colors = LocalPersonality.current.colors
    val isManga = LocalPersonality.current is MangaPersonality

    KomiScaffold(
        topBar = {
            KomiTopBar(
                title = stringResource(Res.string.feed_masthead_title),
                titleAccent = stringResource(Res.string.feed_masthead_title_accent),
                subtitle = if (LocalPersonality.current.usesDecor) stringResource(Res.string.feed_masthead_subtitle) else null,
            )
        },
        toastState = toastState
    ) { innerPadding ->
        if (isAndroid()) {
            KomiPullToRefresh(
                isRefreshing = state.isRefreshing,
                onRefresh = { onAction(FeedAction.OnRefresh) },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                FeedContent(
                    listState = listState,
                    onAction = onAction,
                    colors = colors,
                    state = state,
                    isManga = isManga
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                FeedContent(
                    listState = listState,
                    onAction = onAction,
                    colors = colors,
                    state = state,
                    isManga = isManga
                )
            }
        }
    }

    if (state.isPlatformPickerVisible) {
        FeedPlatformPicker(
            selected = state.selectedPlatform,
            onSelect = { onAction(FeedAction.OnPlatformSelected(it)) },
            onDismiss = { onAction(FeedAction.OnPlatformPickerDismiss) },
        )
    }
}

@Composable
private fun BoxScope.FeedContent(
    listState: LazyListState,
    onAction: (FeedAction) -> Unit,
    colors: PersonalityColors,
    state: FeedState,
    isManga: Boolean
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.constrainedContentWidth().fillMaxSize().align(Alignment.TopCenter),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!isDesktop()) {
            stickyHeader(key = "feed_controls", contentType = "controls") {
                Column(modifier = Modifier.fillMaxWidth().background(colors.background)) {
                    FeedPlatformBar(
                        platform = state.selectedPlatform,
                        onOpenPicker = { onAction(FeedAction.OnPlatformPickerOpen) },
                    )

                    FeedCategoryStrip(
                        categories = state.categories,
                        selected = state.selectedCategory,
                        onSelect = { onAction(FeedAction.OnCategorySelected(it)) },
                    )

                    if (isManga) {
                        KomiHorizontalDivider(thickness = 3.dp, color = colors.outline)
                    }
                }
            }
        }

        when {
            state.isLoading && state.repos.isEmpty() -> {
                item(key = "feed_loading") { FeedLoading() }
            }

            state.errorMessage != null && state.repos.isEmpty() -> {
                item(key = "feed_error") {
                    FeedError(
                        message = state.errorMessage,
                        onRetry = { onAction(FeedAction.OnRetry) },
                    )
                }
            }

            else -> {
                if (state.isOffline) {
                    item(key = "feed_offline") {
                        KomiText(
                            text = stringResource(Res.string.feed_offline),
                            role = KomiTextRole.Label,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 2.dp),
                        )
                    }
                }

                if (state.repos.isEmpty()) {
                    item(key = "feed_empty") {
                        FeedEmpty(
                            category = state.selectedCategory,
                            platform = state.selectedPlatform,
                            onReset = { onAction(FeedAction.OnResetFilters) },
                        )
                    }
                } else {
                    items(state.repos, key = { "feed_${it.repository.id}" }) { card ->
                        DiscoveryRepoCard(
                            discoveryRepositoryUi = card,
                            onClick = { onAction(FeedAction.OnRepoClick(card.repository)) },
                            onShareClick = { onAction(FeedAction.OnShareClick(card.repository)) },
                            onHideClick = { onAction(FeedAction.OnHideRepository(card.repository)) },
                            onToggleSeen = {
                                if (card.isSeen) {
                                    onAction(FeedAction.OnMarkAsUnseen(card.repository.id))
                                } else {
                                    onAction(FeedAction.OnMarkAsSeen(card.repository))
                                }
                            },
                            feed = KomiRepoCardFeed.Release,
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 12.dp)
                                .animateItem(),
                        )
                    }

                    if (state.isLoadingMore) {
                        item(key = "feed_loading_more") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                KomiCircularProgress(modifier = Modifier.size(28.dp))
                            }
                        }
                    } else if (!state.hasMore) {
                        item(key = "feed_end_cap") { FeedEndCap() }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedPlatformBar(
    platform: DiscoveryPlatform,
    onOpenPicker: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        KomiText(
            text = stringResource(Res.string.feed_fresh_for),
            role = KomiTextRole.Title,
        )

        KomiButton(
            onClick = onOpenPicker,
            label = if (platform == DiscoveryPlatform.All) stringResource(Res.string.feed_platform_all) else platform.toLabel(),
            variant = KomiButtonVariant.Primary,
            size = KomiButtonSize.Sm,
            leadingIcon = if (platform == DiscoveryPlatform.All) null else platform.toIcon(),
            trailingIcon = Icons.Rounded.KeyboardArrowDown,
        )
    }
}

@Composable
private fun FeedCategoryStrip(
    categories: ImmutableList<FeedCategory>,
    selected: FeedCategory,
    onSelect: (FeedCategory) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(categories, key = { _, category -> category.name }) { index, category ->
            KomiChip(
                label = category.label(),
                kind = KomiChipKind.Filter,
                selected = category == selected,
                index = index,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun FeedEndCap() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KomiHorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 2.dp,
            color = LocalPersonality.current.colors.outline.copy(alpha = 0.4f),
        )

        if (LocalPersonality.current.usesDecor) {
            KomiText(
                text = stringResource(Res.string.feed_end_cap),
                role = KomiTextRole.Label,
                color = LocalPersonality.current.colors.onSurfaceVariant,
            )
        }

        KomiHorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 2.dp,
            color = LocalPersonality.current.colors.outline.copy(alpha = 0.4f),
        )
    }
}

@Composable
private fun FeedLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            KomiCircularProgress()

            KomiText(
                text = stringResource(Res.string.feed_loading),
                role = KomiTextRole.Body,
                color = LocalPersonality.current.colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FeedError(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            KomiText(
                text = stringResource(Res.string.feed_failed_to_load),
                role = KomiTextRole.Title,
                textAlign = TextAlign.Center,
            )

            KomiText(
                text = message,
                role = KomiTextRole.Body,
                color = LocalPersonality.current.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            KomiButton(
                onClick = onRetry,
                label = stringResource(Res.string.home_retry),
                variant = KomiButtonVariant.Outline,
            )
        }
    }
}

@Composable
private fun FeedEmpty(
    category: FeedCategory,
    platform: DiscoveryPlatform,
    onReset: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            KomiText(
                text = stringResource(Res.string.feed_empty_title),
                role = KomiTextRole.Title,
                textAlign = TextAlign.Center,
            )

            KomiText(
                text = stringResource(
                    Res.string.feed_empty_subtitle,
                    category.label(),
                    platform.toLabel()
                ),
                role = KomiTextRole.Body,
                color = LocalPersonality.current.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            KomiButton(
                onClick = onReset,
                label = stringResource(Res.string.feed_empty_reset),
                variant = KomiButtonVariant.Outline,
            )
        }
    }
}

@Composable
private fun FeedPlatformPicker(
    selected: DiscoveryPlatform,
    onSelect: (DiscoveryPlatform) -> Unit,
    onDismiss: () -> Unit,
) {
    KomiSheet(
        onDismiss = onDismiss,
        placement = KomiSheetPlacement.Bottom,
        title = stringResource(Res.string.feed_platform_picker_title),
        titleJp = stringResource(Res.string.feed_platform_picker_title_jp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DiscoveryPlatform.entries.forEach { platform ->
                FeedPlatformRow(
                    platform = platform,
                    isSelected = platform == selected,
                    onClick = { onSelect(platform) },
                )
            }
        }
    }
}

@Composable
private fun FeedPlatformRow(
    platform: DiscoveryPlatform,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val content =
        if (isSelected) LocalPersonality.current.colors.onPrimary else LocalPersonality.current.colors.onSurface

    KomiSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isSelected) LocalPersonality.current.colors.primary else Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            platform.toIcon()?.let { icon ->
                KomiIcon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = content,
                )
            }

            KomiText(
                text = if (platform == DiscoveryPlatform.All) stringResource(Res.string.feed_platform_all) else platform.toLabel(),
                role = KomiTextRole.Title,
                color = content,
                modifier = Modifier.weight(1f),
            )

            if (isSelected) {
                KomiIcon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = content,
                )
            }
        }
    }
}

@Composable
private fun FeedCategory.label(): String = stringResource(
    when (this) {
        FeedCategory.All -> Res.string.category_all
        FeedCategory.Ai -> Res.string.feed_category_ai
        FeedCategory.Privacy -> Res.string.feed_category_privacy
        FeedCategory.Networking -> Res.string.feed_category_networking
        FeedCategory.Media -> Res.string.feed_category_media
        FeedCategory.Social -> Res.string.feed_category_social
        FeedCategory.Reading -> Res.string.feed_category_reading
        FeedCategory.Tools -> Res.string.feed_category_tools
    },
)
