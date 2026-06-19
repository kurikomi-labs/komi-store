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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import zed.rainxch.core.presentation.components.overlays.rememberKomiToastState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import zed.rainxch.core.presentation.components.bars.KomiTopBar
import zed.rainxch.core.presentation.components.bars.KomiTopBarSize
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.locals.LocalPersonality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import org.jetbrains.compose.resources.pluralStringResource
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

@Composable
fun HiddenRepositoriesRoot(
    onNavigateBack: () -> Unit,
    viewModel: HiddenRepositoriesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val toastState = rememberKomiToastState()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is HiddenRepositoriesEvent.Unhidden ->

                toastState.info(
                    getString(Res.string.hidden_repositories_unhidden_snackbar, event.repoFullName),
                )

            HiddenRepositoriesEvent.UnhiddenAll ->
                toastState.info(
                    getString(Res.string.hidden_repositories_unhidden_all_snackbar),
                )

            is HiddenRepositoriesEvent.Failure ->
                toastState.danger(
                    getString(Res.string.hidden_repositories_unhide_failure),
                )
        }
    }

    KomiScaffold(
        topBar = {
            KomiTopBar(
                title = stringResource(Res.string.hidden_repositories_title),
                subtitle = if (state.items.isNotEmpty()) {
                    pluralStringResource(
                        Res.plurals.hidden_repositories_count,
                        state.items.size,
                        state.items.size,
                    )
                } else {
                    null
                },
                size = KomiTopBarSize.Compact,
                leading = {
                    KomiIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.navigate_back),
                        onClick = onNavigateBack,
                        variant = KomiButtonVariant.Text,
                    )
                },
                actions = {
                    if (state.items.isNotEmpty()) {
                        KomiButton(
                            onClick = {
                                viewModel.onAction(HiddenRepositoriesAction.OnUnhideAll)
                            },
                            label = stringResource(Res.string.hidden_repositories_unhide_all),
                            variant = KomiButtonVariant.Text,
                            size = KomiButtonSize.Sm,
                        )
                    }
                },
            )
        },
        toastState = toastState,
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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

            KomiButton(
                onClick = onUnhide,
                label = stringResource(Res.string.hidden_repositories_unhide_action),
                variant = KomiButtonVariant.Text,
                size = KomiButtonSize.Sm,
            )
        }
    }
}
