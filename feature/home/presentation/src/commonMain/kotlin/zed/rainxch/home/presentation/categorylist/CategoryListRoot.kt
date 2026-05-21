package zed.rainxch.home.presentation.categorylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import zed.rainxch.core.presentation.components.buttons.IconButton
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.vocabulary.Squiggle
import zed.rainxch.home.domain.model.HomeCategory
import zed.rainxch.home.presentation.components.PopularRowItem
import zed.rainxch.home.presentation.components.TrendingRowItem

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding(),
    ) {
        CategoryListTopBar(state.category, onBack)
        if (state.isLoading && state.repos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items = state.repos, key = { it.repository.id }) { repo ->
                val rank = state.repos.indexOf(repo) + 1
                when (state.category) {
                    HomeCategory.MOST_POPULAR -> PopularRowItem(
                        rank = rank,
                        repo = repo,
                        onClick = { onAction(CategoryListAction.OnRepoClick(repo.repository.id)) },
                        onLongClick = { },
                    )
                    else -> TrendingRowItem(
                        rank = rank,
                        repo = repo,
                        onClick = { onAction(CategoryListAction.OnRepoClick(repo.repository.id)) },
                        onLongClick = { },
                    )
                }
            }
            if (state.isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
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
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(
                text = categoryTitle(category),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 26.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.size(4.dp))
            Squiggle()
        }
    }
}

private fun categoryTitle(category: HomeCategory): String = when (category) {
    HomeCategory.HOT_RELEASE -> "Hot releases"
    HomeCategory.TRENDING -> "Trending now"
    HomeCategory.MOST_POPULAR -> "Most popular"
}
