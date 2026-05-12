package zed.rainxch.tweaks.presentation.hidden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.hidden_repositories_count
import zed.rainxch.githubstore.core.presentation.res.hidden_repositories_empty_description
import zed.rainxch.githubstore.core.presentation.res.hidden_repositories_empty_title
import zed.rainxch.githubstore.core.presentation.res.hidden_repositories_title
import zed.rainxch.githubstore.core.presentation.res.hidden_repositories_unhide_action
import zed.rainxch.githubstore.core.presentation.res.hidden_repositories_unhide_all
import zed.rainxch.githubstore.core.presentation.res.hidden_repositories_unhide_failure
import zed.rainxch.githubstore.core.presentation.res.hidden_repositories_unhidden_all_snackbar
import zed.rainxch.githubstore.core.presentation.res.hidden_repositories_unhidden_snackbar
import zed.rainxch.githubstore.core.presentation.res.navigate_back

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenRepositoriesRoot(
    onNavigateBack: () -> Unit,
    viewModel: HiddenRepositoriesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HiddenRepositoriesEvent.Unhidden ->
                scope.launch {
                    snackbarState.showSnackbar(
                        getString(Res.string.hidden_repositories_unhidden_snackbar, event.repoFullName),
                    )
                }
            HiddenRepositoriesEvent.UnhiddenAll ->
                scope.launch {
                    snackbarState.showSnackbar(
                        getString(Res.string.hidden_repositories_unhidden_all_snackbar),
                    )
                }
            is HiddenRepositoriesEvent.Failure ->
                scope.launch {
                    snackbarState.showSnackbar(
                        getString(Res.string.hidden_repositories_unhide_failure),
                    )
                }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(Res.string.hidden_repositories_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (state.items.isNotEmpty()) {
                            Text(
                                text = stringResource(Res.string.hidden_repositories_count, state.items.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.navigate_back),
                        )
                    }
                },
                actions = {
                    if (state.items.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.onAction(HiddenRepositoriesAction.OnUnhideAll)
                            },
                        ) {
                            Text(stringResource(Res.string.hidden_repositories_unhide_all))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarState) },
    ) { padding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.items.isEmpty() -> {
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(Res.string.hidden_repositories_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(Res.string.hidden_repositories_empty_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.items, key = { it.repoId }) { item ->
                            HiddenRepoRow(
                                item = item,
                                onUnhide = {
                                    viewModel.onAction(HiddenRepositoriesAction.OnUnhide(item.repoId))
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HiddenRepoRow(
    item: HiddenRepoUi,
    onUnhide: () -> Unit,
) {
    OutlinedCard(
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GitHubStoreImage(
                imageModel = { item.repoOwnerAvatarUrl },
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.repoName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.repoOwner,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            TextButton(onClick = onUnhide) {
                Text(stringResource(Res.string.hidden_repositories_unhide_action))
            }
        }
    }
}
