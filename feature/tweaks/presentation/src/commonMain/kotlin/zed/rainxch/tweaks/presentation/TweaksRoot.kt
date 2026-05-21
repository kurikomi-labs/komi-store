package zed.rainxch.tweaks.presentation

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.components.ClearDownloadsDialog
import zed.rainxch.tweaks.presentation.components.sections.about
import zed.rainxch.tweaks.presentation.components.sections.othersSection
import zed.rainxch.tweaks.presentation.components.sections.settings
import zed.rainxch.tweaks.presentation.feedback.components.FeedbackBottomSheet
import zed.rainxch.tweaks.presentation.feedback.model.FeedbackChannel

@Composable
fun TweaksRoot(
    onNavigateToMirrorPicker: () -> Unit,
    onNavigateToSkippedUpdates: () -> Unit,
    onNavigateToHiddenRepositories: () -> Unit,
    onNavigateToHostTokens: () -> Unit = {},
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    viewModel.onAction(TweaksAction.OnRefreshCacheSize)

                    viewModel.onAction(TweaksAction.OnReevaluateBatteryOptimizationCard)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            TweaksEvent.OnProxySaved -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.proxy_saved))
                }
            }

            is TweaksEvent.OnProxySaveError -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(event.message)
                }
            }

            is TweaksEvent.OnProxyTestSuccess -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(
                        getString(Res.string.proxy_test_success, event.latencyMs),
                    )
                }
            }

            is TweaksEvent.OnProxyTestError -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(event.message)
                }
            }

            TweaksEvent.OnCacheCleared -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.downloads_cleared))
                }
            }

            is TweaksEvent.OnCacheClearError -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(event.message)
                }
            }

            TweaksEvent.OnSeenHistoryCleared -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.seen_history_cleared))
                }
            }

            TweaksEvent.OnTranslationProviderSaved -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.translation_provider_saved))
                }
            }

            TweaksEvent.OnYoudaoCredentialsSaved -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.translation_youdao_saved))
                }
            }

            TweaksEvent.OnLibreTranslateCredentialsSaved -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.translation_libre_saved))
                }
            }

            TweaksEvent.OnDeeplCredentialsSaved -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.translation_deepl_saved))
                }
            }

            TweaksEvent.OnMicrosoftTranslatorCredentialsSaved -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.translation_microsoft_saved))
                }
            }

            TweaksEvent.OnAppLanguageChangeRequiresRestart -> {
                coroutineScope.launch {
                    val result =
                        snackbarState.showSnackbar(
                            message = getString(Res.string.language_restart_required),
                            actionLabel = getString(Res.string.language_restart_action),
                            withDismissAction = true,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        restartAppAfterLanguageChange()
                    }
                }
            }
        }
    }

    TweaksScreen(
        state = state,
        onAction = { action ->
            when (action) {
                TweaksAction.OnMirrorPickerClick -> onNavigateToMirrorPicker()
                TweaksAction.OnSkippedUpdatesClick -> onNavigateToSkippedUpdates()

                TweaksAction.OnHiddenRepositoriesClick -> onNavigateToHiddenRepositories()
                else -> viewModel.onAction(action)
            }
        },
        snackbarState = snackbarState,
        onNavigateToHostTokens = onNavigateToHostTokens,
    )

    if (state.isClearDownloadsDialogVisible) {
        ClearDownloadsDialog(
            cacheSize = state.cacheSize,
            onDismissRequest = {
                viewModel.onAction(TweaksAction.OnClearDownloadsDismiss)
            },
            onConfirm = {
                viewModel.onAction(TweaksAction.OnClearDownloadsConfirm)
            },
        )
    }

    if (state.showCustomForgesDialog) {
        zed.rainxch.tweaks.presentation.components.CustomForgesDialog(
            state = state,
            onAction = { viewModel.onAction(it) },
        )
    }

    if (state.isFeedbackSheetVisible) {
        FeedbackBottomSheet(
            onDismiss = {
                viewModel.onAction(TweaksAction.OnFeedbackDismiss)
            },
            onSent = { channel ->
                viewModel.onAction(TweaksAction.OnFeedbackDismiss)
                coroutineScope.launch {
                    val msg =
                        when (channel) {
                            FeedbackChannel.EMAIL ->
                                getString(Res.string.feedback_send_success_email)
                            FeedbackChannel.GITHUB ->
                                getString(Res.string.feedback_send_success_github)
                        }
                    snackbarState.showSnackbar(msg)
                }
            },
            onError = { error ->
                coroutineScope.launch {
                    snackbarState.showSnackbar(
                        getString(Res.string.feedback_send_error, error),
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TweaksScreen(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
    snackbarState: SnackbarHostState,
    onNavigateToHostTokens: () -> Unit = {},
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current
    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarState,
                modifier = Modifier.padding(bottom = bottomNavHeight + 16.dp),
            )
        },
        topBar = {
            TopAppBar()
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val listState = rememberLazyListState()
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .arrowKeyScroll(listState, autoFocus = true),
        ) {
            settings(
                state = state,
                onAction = onAction,
            )

            item {
                Spacer(Modifier.height(16.dp))
                zed.rainxch.tweaks.presentation.hosttokens.HostTokensEntryCard(
                    onClick = onNavigateToHostTokens,
                )
                Spacer(Modifier.height(16.dp))
            }

            othersSection(
                state = state,
                onAction = onAction,
            )

            item {
                Spacer(Modifier.height(32.dp))
            }

            about(
                versionName = state.versionName,
                onAction = onAction,
            )

            item {
                Spacer(Modifier.height(bottomNavHeight + 32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopAppBar() {
    TopAppBar(
        title = {
            Text(
                text = stringResource(Res.string.tweaks_title),
                style = MaterialTheme.typography.titleMediumEmphasized,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
    )
}

@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        TweaksScreen(
            state = TweaksState(),
            onAction = {},
            snackbarState = SnackbarHostState(),
        )
    }
}
