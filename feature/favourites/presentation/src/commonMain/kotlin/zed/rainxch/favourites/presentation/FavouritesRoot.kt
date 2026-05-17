package zed.rainxch.favourites.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.components.ScrollbarContainer
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.favourites.presentation.components.FavouriteRepositoryItem
import zed.rainxch.favourites.presentation.model.FavouritesSortRule
import zed.rainxch.githubstore.core.presentation.res.*

@Composable
fun FavouritesRoot(
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (repoId: Long) -> Unit,
    onNavigateToDeveloperProfile: (username: String) -> Unit,
    viewModel: FavouritesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    FavouritesScreen(
        state = state,
        onAction = { action ->
            when (action) {
                FavouritesAction.OnNavigateBackClick -> {
                    onNavigateBack()
                }

                is FavouritesAction.OnRepositoryClick -> {
                    onNavigateToDetails(action.favouriteRepository.repoId)
                }

                is FavouritesAction.OnDeveloperProfileClick -> {
                    onNavigateToDeveloperProfile(action.username)
                }

                else -> {
                    viewModel.onAction(action)
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FavouritesScreen(
    state: FavouritesState,
    onAction: (FavouritesAction) -> Unit,
) {
    Scaffold(
        topBar = {
            FavouritesTopbar(
                sortRule = state.sortRule,
                hasRepos = state.favouriteRepositories.isNotEmpty(),
                onAction = onAction,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            if (state.favouriteRepositories.isNotEmpty()) {
                FavouritesSearchBar(
                    query = state.searchQuery,
                    onQueryChange = { onAction(FavouritesAction.OnSearchChange(it)) },
                )
            }

            val filteredRepositories =
                remember(state.favouriteRepositories, state.searchQuery) {
                    val q = state.searchQuery.trim().lowercase()
                    if (q.isBlank()) {
                        state.favouriteRepositories
                    } else {
                        state.favouriteRepositories
                            .filter { repo ->
                                repo.repoName.lowercase().contains(q) ||
                                    repo.repoOwner.lowercase().contains(q) ||
                                    (repo.repoDescription?.lowercase()?.contains(q) == true) ||
                                    (repo.primaryLanguage?.lowercase()?.contains(q) == true)
                            }
                            .toImmutableList()
                    }
                }

            Box(modifier = Modifier.fillMaxSize()) {
                val gridState = rememberLazyStaggeredGridState()
                val isScrollbarEnabled = LocalScrollbarEnabled.current
                ScrollbarContainer(
                    gridState = gridState,
                    enabled = isScrollbarEnabled,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyVerticalStaggeredGrid(
                        state = gridState,
                        columns =
                            StaggeredGridCells.Adaptive(
                                350.dp,
                            ),
                        verticalItemSpacing = 12.dp,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
                        modifier = Modifier.fillMaxSize().arrowKeyScroll(gridState, autoFocus = true),
                    ) {
                        items(
                            items = filteredRepositories,
                            key = { it.repoId },
                        ) { repo ->
                            FavouriteRepositoryItem(
                                favouriteRepository = repo,
                                onToggleFavouriteClick = {
                                    onAction(FavouritesAction.OnToggleFavorite(repo))
                                },
                                onItemClick = {
                                    onAction(FavouritesAction.OnRepositoryClick(repo))
                                },
                                onDevProfileClick = {
                                    onAction(FavouritesAction.OnDeveloperProfileClick(repo.repoOwner))
                                },
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }
                }

                if (state.isLoading) {
                    CircularWavyProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FavouritesTopbar(
    sortRule: FavouritesSortRule,
    hasRepos: Boolean,
    onAction: (FavouritesAction) -> Unit,
) {
    var showSortMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.favourites),
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        navigationIcon = {
            IconButton(
                shapes = IconButtonDefaults.shapes(),
                onClick = {
                    onAction(FavouritesAction.OnNavigateBackClick)
                },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(Res.string.navigate_back),
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        actions = {
            if (hasRepos) {
                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(Res.string.sort_label),
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                    ) {
                        FavouritesSortRule.entries.forEach { rule ->
                            DropdownMenuItem(
                                text = { Text(stringResource(rule.labelRes())) },
                                onClick = {
                                    showSortMenu = false
                                    onAction(FavouritesAction.OnSortRuleSelected(rule))
                                },
                            )
                        }
                    }
                }
            }
        },
    )
}

private fun FavouritesSortRule.labelRes() =
    when (this) {
        FavouritesSortRule.RecentlyAdded -> Res.string.sort_recently_added
        FavouritesSortRule.NameAsc -> Res.string.sort_name
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavouritesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        placeholder = { Text(stringResource(Res.string.search_repositories_hint)) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(Res.string.clear_search),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors =
            TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
    )
}

@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        FavouritesScreen(
            state = FavouritesState(),
            onAction = {},
        )
    }
}
