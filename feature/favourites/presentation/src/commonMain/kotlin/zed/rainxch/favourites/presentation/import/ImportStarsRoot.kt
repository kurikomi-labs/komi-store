@file:OptIn(ExperimentalMaterial3Api::class)

package zed.rainxch.favourites.presentation.import

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zed.rainxch.core.presentation.components.GitHubStoreImage
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.clear_search
import zed.rainxch.githubstore.core.presentation.res.import_stars_add
import zed.rainxch.githubstore.core.presentation.res.import_stars_add_all
import zed.rainxch.githubstore.core.presentation.res.import_stars_added
import zed.rainxch.githubstore.core.presentation.res.import_stars_button
import zed.rainxch.githubstore.core.presentation.res.import_stars_empty
import zed.rainxch.githubstore.core.presentation.res.import_stars_header_count
import zed.rainxch.githubstore.core.presentation.res.import_stars_hint
import zed.rainxch.githubstore.core.presentation.res.import_stars_no_match
import zed.rainxch.githubstore.core.presentation.res.import_stars_search_hint
import zed.rainxch.githubstore.core.presentation.res.import_stars_subtitle
import zed.rainxch.githubstore.core.presentation.res.import_stars_title
import zed.rainxch.githubstore.core.presentation.res.import_stars_try_another
import zed.rainxch.githubstore.core.presentation.res.navigate_back

@Composable
fun ImportStarsRoot(
    viewModel: ImportStarsViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (repoId: Long, owner: String, repo: String) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ImportStarsEvent.NavigateBack -> onNavigateBack()
            is ImportStarsEvent.NavigateToDetails -> {
                onNavigateToDetails(event.repoId, event.owner, event.repo)
            }
        }
    }

    ImportStarsScreen(state = state, onAction = viewModel::onAction)
}

@Composable
private fun ImportStarsScreen(
    state: ImportStarsState,
    onAction: (ImportStarsAction) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.import_stars_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onAction(ImportStarsAction.OnNavigateBack)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.navigate_back),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (state.phase) {
                ImportStarsState.Phase.UsernameInput -> UsernameInputPanel(
                    state = state,
                    onAction = onAction,
                )

                ImportStarsState.Phase.Results -> ResultsPanel(
                    state = state,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun UsernameInputPanel(
    state: ImportStarsState,
    onAction: (ImportStarsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(Res.string.import_stars_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        GhsTextField(
            value = state.usernameQuery,
            onValueChange = { onAction(ImportStarsAction.OnUsernameQueryChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.import_stars_hint),
            singleLine = true,
        )

        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        GhsButton(
            onClick = { onAction(ImportStarsAction.OnImportClick) },
            label = stringResource(Res.string.import_stars_button),
            variant = GhsButtonVariant.Primary,
            size = GhsButtonSize.Md,
            modifier = Modifier.fillMaxWidth(),
            loading = state.isImporting,
            enabled = state.usernameQuery.trim().isNotEmpty() && !state.isImporting,
        )
    }
}

@Composable
private fun ResultsPanel(
    state: ImportStarsState,
    onAction: (ImportStarsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    resource = Res.string.import_stars_header_count,
                    state.importedUsername ?: "",
                    state.candidates.size,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            GhsButton(
                onClick = { onAction(ImportStarsAction.OnResetImport) },
                label = stringResource(Res.string.import_stars_try_another),
                variant = GhsButtonVariant.Text,
                size = GhsButtonSize.Sm,
            )
        }

        Spacer(Modifier.height(8.dp))
        GhsTextField(
            value = state.searchQuery,
            onValueChange = { onAction(ImportStarsAction.OnSearchChange(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.import_stars_search_hint),
            leadingIcon = Icons.Filled.Search,
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onAction(ImportStarsAction.OnClearSearch) },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(Res.string.clear_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            singleLine = true,
        )

        if (state.pendingCount > 0) {
            Spacer(Modifier.height(12.dp))
            GhsButton(
                onClick = { onAction(ImportStarsAction.OnAddAll) },
                label = stringResource(Res.string.import_stars_add_all, state.pendingCount),
                variant = GhsButtonVariant.Tonal,
                size = GhsButtonSize.Md,
                modifier = Modifier.fillMaxWidth(),
                loading = state.isBulkAdding,
                enabled = !state.isBulkAdding,
            )
        }

        Spacer(Modifier.height(8.dp))
        if (state.filteredCandidates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (state.candidates.isEmpty()) {
                        stringResource(
                            resource = Res.string.import_stars_empty,
                            state.importedUsername ?: ""
                        )
                    } else {
                        stringResource(Res.string.import_stars_no_match)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = state.filteredCandidates, key = { it.repoId }) { candidate ->
                    CandidateRow(
                        candidate = candidate,
                        onClick = { onAction(ImportStarsAction.OnCandidateClick(candidate)) },
                        onToggle = { onAction(ImportStarsAction.OnToggleFavourite(candidate)) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(
    candidate: ImportCandidateUi,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        GitHubStoreImage(
            imageModel = { candidate.ownerAvatarUrl },
            modifier = Modifier
                .size(40.dp)
                .clip(MaterialTheme.shapes.small),
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${candidate.owner}/${candidate.name}",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )

            if (!candidate.description.isNullOrBlank()) {
                Text(
                    text = candidate.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }

        IconButton(onClick = onToggle) {
            Icon(
                imageVector = if (candidate.isAlreadyFavourited) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
                contentDescription = stringResource(
                    if (candidate.isAlreadyFavourited) {
                        Res.string.import_stars_added
                    } else {
                        Res.string.import_stars_add
                    },
                ),
                tint = if (candidate.isAlreadyFavourited) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}
