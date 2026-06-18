package zed.rainxch.home.presentation.categorylist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.cd_back
import zed.rainxch.githubstore.core.presentation.res.home_section_hot_releases
import zed.rainxch.githubstore.core.presentation.res.home_section_most_popular
import zed.rainxch.githubstore.core.presentation.res.home_section_trending_now
import androidx.compose.material3.IconButton
import zed.rainxch.core.presentation.components.cards.DiscoveryRepoCard
import zed.rainxch.core.presentation.components.cards.KomiRepoCardFeed
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.home.domain.model.HomeCategory
import zed.rainxch.home.presentation.model.toDiscoveryUi

@Composable
fun CategoryListRoot(
    category: HomeCategory,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (Long) -> Unit,
    viewModel: CategoryListViewModel = koinViewModel { parametersOf(category) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is CategoryListEvent.NavigateToDetails -> onNavigateToDetails(event.repoId)
        }
    }

    CategoryListScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = onNavigateBack,
    )
}

@Composable
fun CategoryListScreen(
    state: CategoryListState,
    onAction: (CategoryListAction) -> Unit,
    onBack: () -> Unit,
) {
    val listState = rememberLazyListState()

    val shouldLoadMore by remember {
        derivedStateOf {
            val total = listState.layoutInfo.totalItemsCount
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 4
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !state.isLoadingMore && state.hasMorePages) {
            onAction(CategoryListAction.OnLoadMore)
        }
    }

    Scaffold(
        topBar = { CategoryListTopBar(state.category, onBack) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.isLoading && state.cards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(
                        items = state.cards,
                        key = { _, card -> card.id },
                    ) { index, card ->
                        DiscoveryRepoCard(
                            discoveryRepositoryUi = card.toDiscoveryUi(),
                            onClick = { onAction(CategoryListAction.OnRepoClick(card.id)) },
                            onShareClick = { },
                            onDeveloperClick = { },
                            rank = index + 1,
                            feed = if (state.category == HomeCategory.MOST_POPULAR) {
                                KomiRepoCardFeed.Popular
                            } else {
                                KomiRepoCardFeed.Plain
                            },
                        )
                    }

                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryListTopBar(category: HomeCategory, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.cd_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = stringResource(
                    when (category) {
                        HomeCategory.HOT_RELEASE -> Res.string.home_section_hot_releases
                        HomeCategory.TRENDING -> Res.string.home_section_trending_now
                        HomeCategory.MOST_POPULAR -> Res.string.home_section_most_popular
                    },
                ),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 26.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}