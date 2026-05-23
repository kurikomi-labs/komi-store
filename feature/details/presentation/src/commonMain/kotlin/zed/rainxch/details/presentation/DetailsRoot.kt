package zed.rainxch.details.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.Platform
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import zed.rainxch.core.presentation.components.overlays.GhsDropdownMenu
import zed.rainxch.core.presentation.components.overlays.GhsDropdownMenuItem
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.domain.model.InstallSource
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.presentation.components.FloatingPill
import zed.rainxch.core.presentation.components.ScrollbarContainer
import zed.rainxch.core.presentation.locals.LocalContentWidth
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.core.presentation.utils.isPullToRefreshSupported
import zed.rainxch.core.domain.model.RefreshError
import zed.rainxch.details.presentation.components.ApkInspectSheet
import zed.rainxch.details.presentation.components.LanguagePicker
import zed.rainxch.details.presentation.components.sections.about
import zed.rainxch.details.presentation.components.sections.author
import zed.rainxch.details.presentation.components.sections.header
import zed.rainxch.details.presentation.components.sections.logs
import zed.rainxch.details.presentation.components.sections.reportIssue
import zed.rainxch.details.presentation.components.sections.stats
import zed.rainxch.details.presentation.components.sections.releaseChannel
import zed.rainxch.details.presentation.components.sections.whatsNew
import zed.rainxch.details.presentation.components.states.ErrorState
import zed.rainxch.details.presentation.model.TranslationTarget
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.add_to_favourites
import zed.rainxch.githubstore.core.presentation.res.cancel
import zed.rainxch.githubstore.core.presentation.res.confirm_uninstall_message
import zed.rainxch.githubstore.core.presentation.res.confirm_uninstall_title
import zed.rainxch.githubstore.core.presentation.res.details_refresh
import zed.rainxch.githubstore.core.presentation.res.details_refresh_cooldown
import zed.rainxch.githubstore.core.presentation.res.details_refresh_more_options
import zed.rainxch.githubstore.core.presentation.res.details_refresh_snackbar_archived
import zed.rainxch.githubstore.core.presentation.res.details_refresh_snackbar_budget_exhausted
import zed.rainxch.githubstore.core.presentation.res.details_refresh_snackbar_cooldown
import zed.rainxch.githubstore.core.presentation.res.details_refresh_snackbar_generic
import zed.rainxch.githubstore.core.presentation.res.details_refresh_snackbar_not_found
import zed.rainxch.githubstore.core.presentation.res.details_refresh_snackbar_upstream
import zed.rainxch.githubstore.core.presentation.res.details_unlink_external_app_dialog_body
import zed.rainxch.githubstore.core.presentation.res.details_unlink_external_app_dialog_confirm
import zed.rainxch.githubstore.core.presentation.res.details_unlink_external_app_dialog_title
import zed.rainxch.githubstore.core.presentation.res.details_unlink_external_app_menu
import zed.rainxch.githubstore.core.presentation.res.dismiss
import zed.rainxch.githubstore.core.presentation.res.downgrade_requires_uninstall
import zed.rainxch.githubstore.core.presentation.res.downgrade_warning_message
import zed.rainxch.githubstore.core.presentation.res.install_anyway
import zed.rainxch.githubstore.core.presentation.res.install_permission_blocked_message
import zed.rainxch.githubstore.core.presentation.res.install_permission_unavailable
import zed.rainxch.githubstore.core.presentation.res.navigate_back
import zed.rainxch.githubstore.core.presentation.res.open_repository
import zed.rainxch.githubstore.core.presentation.res.open_with_external_installer
import zed.rainxch.githubstore.core.presentation.res.remove_from_favourites
import zed.rainxch.githubstore.core.presentation.res.repository_not_starred
import zed.rainxch.githubstore.core.presentation.res.repository_starred
import zed.rainxch.githubstore.core.presentation.res.share_repository
import zed.rainxch.githubstore.core.presentation.res.signing_key_changed_message
import zed.rainxch.githubstore.core.presentation.res.signing_key_changed_title
import zed.rainxch.githubstore.core.presentation.res.star_from_github
import zed.rainxch.githubstore.core.presentation.res.uninstall
import zed.rainxch.githubstore.core.presentation.res.uninstall_first
import zed.rainxch.githubstore.core.presentation.res.unstar_from_github

@Composable
fun DetailsRoot(
    onNavigateBack: () -> Unit,
    onNavigateToDeveloperProfile: (username: String) -> Unit,
    onOpenRepositoryInApp: (repoId: Long) -> Unit,
    onNavigateToSearchByPlatform: (DiscoveryPlatform) -> Unit,
    onNavigateToAbout: (repoId: Long, owner: String, repo: String, sourceHost: String?) -> Unit,
    onNavigateToWhatsNew: (repoId: Long, owner: String, repo: String, sourceHost: String?) -> Unit,
    viewModel: DetailsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is DetailsEvent.OnOpenRepositoryInApp -> {
                onOpenRepositoryInApp(event.repositoryId)
            }

            is DetailsEvent.OnMessage -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }

            is DetailsEvent.OnRefreshError -> {
                coroutineScope.launch {
                    val seconds = event.retryAfterSeconds?.toInt() ?: 0
                    val text = when (event.kind) {
                        RefreshError.COOLDOWN -> getString(
                            Res.string.details_refresh_snackbar_cooldown,
                            seconds.coerceAtLeast(1),
                        )
                        RefreshError.BUDGET_EXHAUSTED -> getString(
                            Res.string.details_refresh_snackbar_budget_exhausted,
                            seconds.coerceAtLeast(1),
                        )
                        RefreshError.ARCHIVED -> getString(Res.string.details_refresh_snackbar_archived)
                        RefreshError.NOT_FOUND -> getString(Res.string.details_refresh_snackbar_not_found)
                        RefreshError.UPSTREAM -> getString(Res.string.details_refresh_snackbar_upstream)
                        RefreshError.GENERIC -> getString(Res.string.details_refresh_snackbar_generic)
                    }
                    snackbarHostState.showSnackbar(text)
                }
            }
        }
    }

    val onAction: (DetailsAction) -> Unit = remember(
        viewModel,
        coroutineScope,
        snackbarHostState,
        onNavigateBack,
        onNavigateToDeveloperProfile,
        onNavigateToSearchByPlatform,
    ) {
        { action ->
            when (action) {
                DetailsAction.OnNavigateBackClick -> {
                    onNavigateBack()
                }

                is DetailsAction.OpenDeveloperProfile -> {
                    onNavigateToDeveloperProfile(action.username)
                }

                is DetailsAction.OnPlatformChipClick -> {
                    onNavigateToSearchByPlatform(action.platform)
                }

                is DetailsAction.OnMessage -> {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(getString(action.messageText))
                    }
                }

                else -> {
                    viewModel.onAction(action)
                }
            }
            Unit
        }
    }

    DetailsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = onAction,
        onReadMoreAbout = state.repository?.let { repo ->
            {
                onNavigateToAbout(
                    repo.id,
                    repo.owner.login,
                    repo.name,
                    repo.sourceHost,
                )
            }
        },
        onReadMoreWhatsNew = state.repository?.let { repo ->
            {
                onNavigateToWhatsNew(
                    repo.id,
                    repo.owner.login,
                    repo.name,
                    repo.sourceHost,
                )
            }
        },
    )

    state.downgradeWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(DetailsAction.OnDismissDowngradeWarning)
            },
            shape = WonkySquircleShape.Dialog,
            title = {
                Text(
                    text = stringResource(Res.string.downgrade_requires_uninstall),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            },
            text = {
                Text(
                    text =
                        stringResource(
                            Res.string.downgrade_warning_message,
                            warning.targetVersion,
                            warning.currentVersion,
                        ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnDismissDowngradeWarning)
                        viewModel.onAction(DetailsAction.UninstallApp)
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.uninstall_first),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnDismissDowngradeWarning)
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.cancel),
                    )
                }
            },
        )
    }

    state.signingKeyWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(DetailsAction.OnDismissSigningKeyWarning)
            },
            shape = WonkySquircleShape.Dialog,
            title = {
                Text(
                    text = stringResource(Res.string.signing_key_changed_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            },
            text = {
                Text(
                    text =
                        stringResource(
                            Res.string.signing_key_changed_message,
                            warning.expectedFingerprint.take(19),
                            warning.actualFingerprint.take(19),
                        ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnOverrideSigningKeyWarning)
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.install_anyway),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnDismissSigningKeyWarning)
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.cancel),
                    )
                }
            },
        )
    }

    if (state.showUninstallConfirmation) {
        val appName = state.installedApp?.appName ?: ""
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(DetailsAction.OnDismissUninstallConfirmation)
            },
            shape = WonkySquircleShape.Dialog,
            title = {
                Text(
                    text = stringResource(Res.string.confirm_uninstall_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.confirm_uninstall_message, appName),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnConfirmUninstall)
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.uninstall),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnDismissUninstallConfirmation)
                    },
                ) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (state.showUnlinkConfirmation) {
        val appName = state.installedApp?.appName ?: ""
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(DetailsAction.OnDismissUnlinkConfirmation)
            },
            shape = WonkySquircleShape.Dialog,
            title = {
                Text(
                    text = stringResource(Res.string.details_unlink_external_app_dialog_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            },
            text = {
                Text(
                    text = stringResource(Res.string.details_unlink_external_app_dialog_body, appName),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnConfirmUnlinkExternalApp)
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.details_unlink_external_app_dialog_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnDismissUnlinkConfirmation)
                    },
                ) {
                    Text(text = stringResource(Res.string.cancel))
                }
            },
        )
    }

    if (state.showExternalInstallerPrompt) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(DetailsAction.DismissExternalInstallerPrompt)
            },
            shape = WonkySquircleShape.Dialog,
            title = {
                Text(
                    text = stringResource(Res.string.install_permission_unavailable),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            },
            text = {
                Text(text = stringResource(Res.string.install_permission_blocked_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OpenWithExternalInstaller)
                    },
                ) {
                    Text(text = stringResource(Res.string.open_with_external_installer))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.DismissExternalInstallerPrompt)
                    },
                ) {
                    Text(text = stringResource(Res.string.dismiss))
                }
            },
        )
    }

    if (state.isApkInspectSheetVisible) {
        ApkInspectSheet(
            inspection = state.apkInspection,
            isLoading = state.isApkInspectLoading,
            onDismiss = { viewModel.onAction(DetailsAction.OnDismissApkInspect) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailsScreen(
    state: DetailsState,
    onAction: (DetailsAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    onReadMoreAbout: (() -> Unit)? = null,
    onReadMoreWhatsNew: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            DetailsTopbar(
                state = state,
                onAction = onAction,
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->

            LanguagePicker(
                isVisible = state.isLanguagePickerVisible,
                selectedLanguageCode =
                    when (state.languagePickerTarget) {
                        TranslationTarget.About -> state.aboutTranslation.targetLanguageCode
                        TranslationTarget.WhatsNew -> state.whatsNewTranslation.targetLanguageCode
                        null -> null
                    },
                deviceLanguageCode = state.deviceLanguageCode,
                onLanguageSelected = { language ->
                    when (state.languagePickerTarget) {
                        TranslationTarget.About -> {
                            onAction(DetailsAction.TranslateAbout(language.code))
                        }

                        TranslationTarget.WhatsNew -> {
                            onAction(
                                DetailsAction.TranslateWhatsNew(
                                    language.code,
                                ),
                            )
                        }

                        null -> {}
                    }
                    onAction(DetailsAction.DismissLanguagePicker)
                },
                onDismiss = { onAction(DetailsAction.DismissLanguagePicker) },
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularWavyProgressIndicator()
                }

                return@Scaffold
            }

            if (state.errorMessage != null) {
                ErrorState(state.errorMessage, onAction)

                return@Scaffold
            }

            val density = LocalDensity.current
            var containerHeightDp by remember { mutableStateOf(0.dp) }
            val collapsedSectionHeight = containerHeightDp * 0.4f
            val listState = rememberLazyListState()
            val isScrollbarEnabled = LocalScrollbarEnabled.current
            val contentWidthDp = when (LocalContentWidth.current) {
                ContentWidth.COMPACT -> 680.dp
                ContentWidth.WIDE -> 960.dp
                ContentWidth.EXTRA_WIDE -> androidx.compose.ui.unit.Dp.Unspecified
            }
            val pullEnabled = remember { isPullToRefreshSupported() }

            val isDesktop = remember { getPlatform() != Platform.ANDROID }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scrollable(
                        state = listState,
                        orientation = Orientation.Vertical,

                        reverseDirection = true,
                        enabled = isDesktop,
                    )
                    .onSizeChanged { size ->

                        val newHeight = with(density) { size.height.toDp() }
                        if (newHeight != containerHeightDp) containerHeightDp = newHeight
                    },
                contentAlignment = Alignment.Center,
            ) {
                ScrollbarContainer(
                    listState = listState,
                    enabled = isScrollbarEnabled,
                    modifier =
                        Modifier
                            .fillMaxHeight()
                            .widthIn(max = contentWidthDp)
                            .fillMaxWidth(),
                ) {
                    val listModifier =
                        Modifier
                            .fillMaxHeight()
                            .widthIn(max = contentWidthDp)
                            .fillMaxWidth()
                            .arrowKeyScroll(listState, autoFocus = true)
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown &&
                                    (event.isMetaPressed || event.isCtrlPressed) &&
                                    event.key == Key.R
                                ) {
                                    onAction(DetailsAction.Refresh)
                                    true
                                } else {
                                    false
                                }
                            }.padding(innerPadding)

                    PullToRefreshHost(
                        enabled = pullEnabled,
                        isRefreshing = state.isRefreshing,
                        onRefresh = { onAction(DetailsAction.Refresh) },
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = listModifier,
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                    header(
                        state = state,
                        onAction = onAction,
                    )

                    state.stats?.let { stats ->
                        stats(
                            repoStats = stats,
                        )
                    }

                    releaseChannel(
                        state = state,
                        onAction = onAction,
                    )

                    if (state.isComingFromUpdate) {
                        state.selectedRelease?.let { release ->
                            whatsNew(
                                release = release,
                                isExpanded = state.isWhatsNewExpanded,
                                onToggleExpanded = { onAction(DetailsAction.ToggleWhatsNewExpanded) },
                                collapsedHeight = collapsedSectionHeight,
                                measuredHeightPx = state.whatsNewMeasuredHeightPx,
                                onMeasured = { onAction(DetailsAction.OnWhatsNewMeasured(it)) },
                                translationState = state.whatsNewTranslation,
                                onTranslateClick = {
                                    onAction(DetailsAction.TranslateWhatsNew(state.deviceLanguageCode))
                                },
                                onLanguagePickerClick = {
                                    onAction(DetailsAction.ShowLanguagePicker(TranslationTarget.WhatsNew))
                                },
                                onToggleTranslation = {
                                    onAction(DetailsAction.ToggleWhatsNewTranslation)
                                },
                                onReadMore = onReadMoreWhatsNew,
                            )
                        }

                        state.readmeMarkdown?.let {
                            about(
                                readmeMarkdown = state.readmeMarkdown,
                                readmeLanguage = state.readmeLanguage,
                                isExpanded = state.isAboutExpanded,
                                onToggleExpanded = { onAction(DetailsAction.ToggleAboutExpanded) },
                                collapsedHeight = collapsedSectionHeight,
                                measuredHeightPx = state.aboutMeasuredHeightPx,
                                onMeasured = { onAction(DetailsAction.OnAboutMeasured(it)) },
                                translationState = state.aboutTranslation,
                                onTranslateClick = {
                                    onAction(DetailsAction.TranslateAbout(state.deviceLanguageCode))
                                },
                                onLanguagePickerClick = {
                                    onAction(DetailsAction.ShowLanguagePicker(TranslationTarget.About))
                                },
                                onToggleTranslation = {
                                    onAction(DetailsAction.ToggleAboutTranslation)
                                },
                                onReadMore = onReadMoreAbout,
                            )
                        }
                    } else {
                        state.readmeMarkdown?.let {
                            about(
                                readmeMarkdown = state.readmeMarkdown,
                                readmeLanguage = state.readmeLanguage,
                                isExpanded = state.isAboutExpanded,
                                onToggleExpanded = { onAction(DetailsAction.ToggleAboutExpanded) },
                                collapsedHeight = collapsedSectionHeight,
                                measuredHeightPx = state.aboutMeasuredHeightPx,
                                onMeasured = { onAction(DetailsAction.OnAboutMeasured(it)) },
                                translationState = state.aboutTranslation,
                                onTranslateClick = {
                                    onAction(DetailsAction.TranslateAbout(state.deviceLanguageCode))
                                },
                                onLanguagePickerClick = {
                                    onAction(DetailsAction.ShowLanguagePicker(TranslationTarget.About))
                                },
                                onToggleTranslation = {
                                    onAction(DetailsAction.ToggleAboutTranslation)
                                },
                                onReadMore = onReadMoreAbout,
                            )
                        }

                        state.selectedRelease?.let { release ->
                            whatsNew(
                                release = release,
                                isExpanded = state.isWhatsNewExpanded,
                                onToggleExpanded = { onAction(DetailsAction.ToggleWhatsNewExpanded) },
                                collapsedHeight = collapsedSectionHeight,
                                measuredHeightPx = state.whatsNewMeasuredHeightPx,
                                onMeasured = { onAction(DetailsAction.OnWhatsNewMeasured(it)) },
                                translationState = state.whatsNewTranslation,
                                onTranslateClick = {
                                    onAction(DetailsAction.TranslateWhatsNew(state.deviceLanguageCode))
                                },
                                onLanguagePickerClick = {
                                    onAction(DetailsAction.ShowLanguagePicker(TranslationTarget.WhatsNew))
                                },
                                onToggleTranslation = {
                                    onAction(DetailsAction.ToggleWhatsNewTranslation)
                                },
                                onReadMore = onReadMoreWhatsNew,
                            )
                        }
                    }

                    state.repository?.let { repository ->
                        reportIssue(
                            repoUrl = repository.htmlUrl,
                        )
                    }

                    state.userProfile?.let { userProfile ->
                        author(
                            author = userProfile,
                            onAction = onAction,
                        )
                    }

                    if (state.installLogs.isNotEmpty()) {
                        logs(state)
                    }
                        }
                    }
                }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PullToRefreshHost(
    enabled: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (enabled) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            content()
        }
    } else {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DetailsTopbar(
    state: DetailsState,
    onAction: (DetailsAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        FloatingPill(
            onClick = { onAction(DetailsAction.OnNavigateBackClick) },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.navigate_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        if (state.repository != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(50),
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onAction(DetailsAction.OpenRepoInBrowser) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInBrowser,
                        contentDescription = stringResource(Res.string.open_repository),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(1.dp, 20.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                )
                DetailsOverflowMenu(state = state, onAction = onAction)
            }
        }
    }
}


@OptIn(
    ExperimentalTime::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun DetailsOverflowMenu(
    state: DetailsState,
    onAction: (DetailsAction) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val cooldownUntilMs = state.refreshCooldownUntilEpochMs
    var nowMs by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }

    LaunchedEffect(cooldownUntilMs) {
        if (cooldownUntilMs == null) return@LaunchedEffect
        while (Clock.System.now().toEpochMilliseconds() < cooldownUntilMs) {
            nowMs = Clock.System.now().toEpochMilliseconds()
            delay(500L)
        }
        nowMs = Clock.System.now().toEpochMilliseconds()
    }

    val cooldownSeconds = cooldownUntilMs?.let { until ->
        ((until - nowMs + 999) / 1000).coerceAtLeast(0L).toInt()
    } ?: 0
    val cooldownActive = cooldownSeconds > 0
    val refreshDisabled = cooldownActive || state.isRefreshing

    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable { menuOpen = true }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(Res.string.details_refresh_more_options),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
        GhsDropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
        ) {
            GhsDropdownMenuItem(
                text = stringResource(
                    if (state.isStarred) Res.string.repository_starred
                    else Res.string.repository_not_starred,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = if (state.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    menuOpen = false
                    onAction(
                        DetailsAction.OnMessage(
                            messageText = if (state.isStarred) {
                                Res.string.unstar_from_github
                            } else {
                                Res.string.star_from_github
                            },
                        ),
                    )
                },
            )
            GhsDropdownMenuItem(
                text = stringResource(
                    if (state.isFavourite) Res.string.remove_from_favourites
                    else Res.string.add_to_favourites,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = if (state.isFavourite) {
                            Icons.Default.Favorite
                        } else {
                            Icons.Default.FavoriteBorder
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    menuOpen = false
                    onAction(DetailsAction.OnToggleFavorite)
                },
            )
            state.repository?.htmlUrl?.let {
                GhsDropdownMenuItem(
                    text = stringResource(Res.string.share_repository),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onAction(DetailsAction.OnShareClick)
                    },
                )
            }
            GhsDropdownMenuItem(
                enabled = !refreshDisabled,
                text = if (cooldownActive) {
                    stringResource(Res.string.details_refresh_cooldown, cooldownSeconds)
                } else {
                    stringResource(Res.string.details_refresh)
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                onClick = {
                    menuOpen = false
                    onAction(DetailsAction.Refresh)
                },
            )
            if (state.installedApp?.installSource == InstallSource.MANUAL) {
                GhsDropdownMenuItem(
                    text = stringResource(Res.string.details_unlink_external_app_menu),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LinkOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onAction(DetailsAction.OnUnlinkExternalApp)
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        DetailsScreen(
            state =
                DetailsState(
                    isLoading = false,
                ),
            onAction = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
