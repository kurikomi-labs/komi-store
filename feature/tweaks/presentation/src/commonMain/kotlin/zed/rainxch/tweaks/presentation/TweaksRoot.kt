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
import io.github.fletchmckee.liquid.liquefiable
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.locals.LocalBottomNavigationLiquid
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.components.ClearDownloadsDialog
import zed.rainxch.tweaks.presentation.components.sections.about
import zed.rainxch.tweaks.presentation.components.sections.othersSection
import zed.rainxch.tweaks.presentation.components.sections.settings

@Composable
fun TweaksRoot(viewModel: TweaksViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val productTelemetry: zed.rainxch.core.domain.telemetry.ProductTelemetry = org.koin.compose.koinInject()

    // versionName is hydrated asynchronously in TweaksViewModel.onStart;
    // its presence is the cheapest "first non-skeleton render" signal here.
    zed.rainxch.core.presentation.telemetry.TrackFirstPaint(isReady = state.versionName.isNotEmpty()) { ms ->
        productTelemetry.fire(
            name = zed.rainxch.core.domain.telemetry.ProductTelemetryEvents.FIRST_PAINT_MS,
            props =
                mapOf(
                    zed.rainxch.core.domain.telemetry.ProductTelemetryProps.SCREEN to "settings",
                    zed.rainxch.core.domain.telemetry.ProductTelemetryProps.BUCKET to
                        zed.rainxch.core.domain.telemetry.TelemetryBuckets.durationMs(ms),
                ),
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    viewModel.onAction(TweaksAction.OnRefreshCacheSize)
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

            TweaksEvent.OnAnalyticsIdReset -> {
                coroutineScope.launch {
                    snackbarState.showSnackbar(getString(Res.string.analytics_id_reset))
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

            TweaksEvent.OnAppLanguageChangeRequiresRestart -> {
                coroutineScope.launch {
                    val result =
                        snackbarState.showSnackbar(
                            message = getString(Res.string.language_restart_required),
                            actionLabel = getString(Res.string.language_restart_action),
                            withDismissAction = true,
                        )
                    if (result == SnackbarResult.ActionPerformed) {
                        // Best-effort relaunch on Desktop; see
                        // `RestartApp.jvm.kt`. Falls back to plain
                        // exit if a clean relaunch isn't possible
                        // (IDE runs, sandbox restrictions) — the
                        // preference is already persisted so the new
                        // locale takes effect on the next manual
                        // launch either way.
                        restartAppAfterLanguageChange()
                    }
                }
            }
        }
    }

    TweaksScreen(
        state = state,
        onAction = viewModel::onAction,
        snackbarState = snackbarState,
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
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TweaksScreen(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
    snackbarState: SnackbarHostState,
) {
    val liquidState = LocalBottomNavigationLiquid.current
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
        modifier =
            Modifier.then(
                if (state.isLiquidGlassEnabled) {
                    Modifier.liquefiable(liquidState)
                } else {
                    Modifier
                },
            ),
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
