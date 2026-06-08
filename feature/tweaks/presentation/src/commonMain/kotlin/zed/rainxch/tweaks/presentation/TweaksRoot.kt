package zed.rainxch.tweaks.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.GTranslate
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import zed.rainxch.core.presentation.utils.constrainedContentWidth
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.components.RestartBanner
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.system.Platform
import zed.rainxch.core.presentation.components.hub.GhsEntryRow
import zed.rainxch.core.presentation.components.hub.GhsSectionHeader
import zed.rainxch.core.presentation.theme.tokens.GhsAccents
import zed.rainxch.tweaks.presentation.components.TweaksSearchField
import zed.rainxch.tweaks.presentation.feedback.components.FeedbackBottomSheet
import zed.rainxch.tweaks.presentation.feedback.model.FeedbackChannel

@Composable
fun TweaksRoot(
    onNavigateBack: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToConnection: () -> Unit,
    onNavigateToSources: () -> Unit,
    onNavigateToTranslation: () -> Unit,
    onNavigateToInstallMethod: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToHostTokens: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var feedbackSheetOpen by rememberSaveable { mutableStateOf(false) }

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
            TweaksEvent.OnAppLanguageChangeRequiresRestart -> {
                coroutineScope.launch {
                    val result = snackbarState.showSnackbar(
                        message = getString(Res.string.language_restart_required),
                        actionLabel = getString(Res.string.language_restart_action),
                        withDismissAction = true,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        restartAppAfterLanguageChange()
                    }
                }
            }
            else -> Unit
        }
    }

    TweaksHubScreen(
        state = state,
        onNavigateBack = onNavigateBack,
        onNavigateToAppearance = onNavigateToAppearance,
        onNavigateToLanguage = onNavigateToLanguage,
        onNavigateToConnection = onNavigateToConnection,
        onNavigateToSources = onNavigateToSources,
        onNavigateToTranslation = onNavigateToTranslation,
        onNavigateToInstallMethod = onNavigateToInstallMethod,
        onNavigateToUpdates = onNavigateToUpdates,
        onNavigateToStorage = onNavigateToStorage,
        onNavigateToPrivacy = onNavigateToPrivacy,
        onNavigateToHostTokens = onNavigateToHostTokens,
        onSendFeedbackClick = { feedbackSheetOpen = true },
        onRestartNow = { viewModel.onAction(TweaksAction.OnRestartNowClick) },
        onRestartLater = { viewModel.onAction(TweaksAction.OnRestartLaterClick) },
        snackbarState = snackbarState,
    )

    if (feedbackSheetOpen) {
        FeedbackBottomSheet(
            onDismiss = { feedbackSheetOpen = false },
            onSent = { channel ->
                feedbackSheetOpen = false
                coroutineScope.launch {
                    val msg = when (channel) {
                        FeedbackChannel.EMAIL -> getString(Res.string.feedback_send_success_email)
                        FeedbackChannel.GITHUB -> getString(Res.string.feedback_send_success_github)
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

private data class TweaksHubEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val accent: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
)

private data class TweaksHubBlock(
    val title: String,
    val entries: List<TweaksHubEntry>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TweaksHubScreen(
    state: TweaksState,
    onNavigateBack: () -> Unit,
    onNavigateToAppearance: () -> Unit,
    onNavigateToLanguage: () -> Unit,
    onNavigateToConnection: () -> Unit,
    onNavigateToSources: () -> Unit,
    onNavigateToTranslation: () -> Unit,
    onNavigateToInstallMethod: () -> Unit,
    onNavigateToUpdates: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToHostTokens: () -> Unit,
    onSendFeedbackClick: () -> Unit,
    onRestartNow: () -> Unit,
    onRestartLater: () -> Unit,
    snackbarState: SnackbarHostState,
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current
    var query by rememberSaveable { mutableStateOf("") }

    val tapToManage = stringResource(Res.string.tweaks_entry_subtitle_tap)
    val isAndroid = getPlatform() == Platform.ANDROID

    val blocks = listOfNotNull(
        TweaksHubBlock(
            title = stringResource(Res.string.section_look_and_feel),
            entries = listOf(
                TweaksHubEntry(
                    title = stringResource(Res.string.tweaks_entry_appearance),
                    subtitle = tapToManage,
                    icon = Icons.Outlined.Palette,
                    onClick = onNavigateToAppearance,
                    accent = GhsAccents.Lavender,
                ),
                TweaksHubEntry(
                    title = stringResource(Res.string.tweaks_entry_language),
                    subtitle = tapToManage,
                    icon = Icons.Outlined.Translate,
                    onClick = onNavigateToLanguage,
                    accent = GhsAccents.Mint,
                ),
            ),
        ),
        TweaksHubBlock(
            title = stringResource(Res.string.section_connectivity),
            entries = listOf(
                TweaksHubEntry(
                    title = stringResource(Res.string.tweaks_entry_connection),
                    subtitle = tapToManage,
                    icon = Icons.Outlined.Wifi,
                    onClick = onNavigateToConnection,
                    accent = GhsAccents.Sky,
                ),
                TweaksHubEntry(
                    title = stringResource(Res.string.tweaks_entry_sources),
                    subtitle = tapToManage,
                    icon = Icons.Outlined.Hub,
                    onClick = onNavigateToSources,
                    accent = GhsAccents.Blush,
                ),
                TweaksHubEntry(
                    title = stringResource(Res.string.tweaks_entry_translation),
                    subtitle = tapToManage,
                    icon = Icons.Outlined.GTranslate,
                    onClick = onNavigateToTranslation,
                    accent = GhsAccents.Peach,
                ),
            ),
        ),
        if (isAndroid) {
            TweaksHubBlock(
                title = stringResource(Res.string.section_installs_and_updates),
                entries = listOf(
                    TweaksHubEntry(
                        title = stringResource(Res.string.tweaks_entry_install_method),
                        subtitle = tapToManage,
                        icon = Icons.Outlined.InstallMobile,
                        onClick = onNavigateToInstallMethod,
                        accent = GhsAccents.Sage,
                    ),
                    TweaksHubEntry(
                        title = stringResource(Res.string.tweaks_entry_updates),
                        subtitle = tapToManage,
                        icon = Icons.Outlined.Update,
                        onClick = onNavigateToUpdates,
                        accent = GhsAccents.Amber,
                    ),
                ),
            )
        } else {
            null
        },
        TweaksHubBlock(
            title = stringResource(Res.string.section_privacy_and_data),
            entries = listOf(
                TweaksHubEntry(
                    title = stringResource(Res.string.tweaks_entry_storage),
                    subtitle = state.cacheSize.ifBlank { tapToManage },
                    icon = Icons.Outlined.Inventory2,
                    onClick = onNavigateToStorage,
                    accent = GhsAccents.Periwinkle,
                ),
                TweaksHubEntry(
                    title = stringResource(Res.string.tweaks_entry_privacy),
                    subtitle = tapToManage,
                    icon = Icons.Outlined.PrivacyTip,
                    onClick = onNavigateToPrivacy,
                    accent = GhsAccents.Rose,
                ),
                TweaksHubEntry(
                    title = stringResource(Res.string.tweaks_entry_access_tokens),
                    subtitle = tapToManage,
                    icon = Icons.Outlined.VpnKey,
                    onClick = onNavigateToHostTokens,
                    accent = GhsAccents.Gold,
                ),
            ),
        ),
        TweaksHubBlock(
            title = stringResource(Res.string.section_app_block),
            entries = listOf(
                TweaksHubEntry(
                    title = stringResource(Res.string.tweaks_entry_feedback),
                    subtitle = stringResource(Res.string.feedback_hub_subtitle),
                    icon = Icons.Outlined.Feedback,
                    onClick = onSendFeedbackClick,
                    accent = GhsAccents.Tan,
                ),
            ),
        ),
    )

    val filteredBlocks = remember(query, blocks) {
        if (query.isBlank()) {
            blocks
        } else {
            val q = query.trim()
            blocks.map { block ->
                block.copy(
                    entries = block.entries.filter { entry ->
                        entry.title.contains(q, ignoreCase = true) ||
                            entry.subtitle.contains(q, ignoreCase = true)
                    },
                )
            }.filter { it.entries.isNotEmpty() }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarState,
                modifier = Modifier.padding(bottom = bottomNavHeight + 16.dp),
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.tweaks_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back_cd),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val listState = rememberLazyListState()
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .constrainedContentWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
                .arrowKeyScroll(listState, autoFocus = true),
        ) {
            if (state.restartBannerVisible) {
                item(key = "restart_banner") {
                    Spacer(Modifier.height(8.dp))
                    RestartBanner(
                        reasons = state.needsRestartReasons,
                        onRestartNow = onRestartNow,
                        onLater = onRestartLater,
                    )
                }
            }

            item(key = "search_field") {
                Spacer(Modifier.height(12.dp))
                TweaksSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onClear = { query = "" },
                )
                Spacer(Modifier.height(16.dp))
            }

            if (filteredBlocks.isEmpty()) {
                item(key = "search_empty") {
                    EmptySearchResult(query = query)
                    Spacer(Modifier.height(32.dp))
                }
            } else if (query.isBlank()) {
                blocks.forEachIndexed { idx, block ->
                    item(key = "block_header_${block.title}") {
                        if (idx > 0) Spacer(Modifier.height(24.dp))
                        GhsSectionHeader(text = block.title)
                        Spacer(Modifier.height(8.dp))
                    }
                    block.entries.forEach { entry ->
                        item(key = "entry_${block.title}_${entry.title}") {
                            GhsEntryRow(
                                title = entry.title,
                                subtitle = entry.subtitle,
                                icon = entry.icon,
                                onClick = entry.onClick,
                                accentColor = entry.accent,
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            } else {
                filteredBlocks.flatMap { it.entries }.forEach { entry ->
                    item(key = "search_entry_${entry.title}") {
                        GhsEntryRow(
                            title = entry.title,
                            subtitle = entry.subtitle,
                            icon = entry.icon,
                            onClick = entry.onClick,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(Modifier.height(bottomNavHeight + 32.dp))
            }
        }
        }
    }
}

@Composable
private fun EmptySearchResult(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.SearchOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.tweaks_search_empty, query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        TweaksHubScreen(
            state = TweaksState(),
            onNavigateBack = {},
            onNavigateToAppearance = {},
            onNavigateToLanguage = {},
            onNavigateToConnection = {},
            onNavigateToSources = {},
            onNavigateToTranslation = {},
            onNavigateToInstallMethod = {},
            onNavigateToUpdates = {},
            onNavigateToStorage = {},
            onNavigateToPrivacy = {},
            onNavigateToHostTokens = {},
            onSendFeedbackClick = {},
            onRestartNow = {},
            onRestartLater = {},
            snackbarState = SnackbarHostState(),
        )
    }
}
