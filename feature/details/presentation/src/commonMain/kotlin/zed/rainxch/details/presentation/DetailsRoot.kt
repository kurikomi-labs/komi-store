package zed.rainxch.details.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import zed.rainxch.core.domain.isDesktop
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
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.components.overlays.KomiDropdown
import zed.rainxch.core.presentation.components.overlays.KomiMenuItem
import kotlinx.collections.immutable.toImmutableList
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.domain.model.installation.InstallSource
import zed.rainxch.core.presentation.components.ScrollbarContainer
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiIconButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.scaffold.KomiScaffold
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.core.presentation.utils.contentWidthCap
import zed.rainxch.core.presentation.personality.utils.PersonalityPreview
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.core.presentation.utils.isPullToRefreshSupported
import zed.rainxch.core.domain.model.error.RefreshError
import zed.rainxch.details.presentation.components.ApkInspectSheet
import zed.rainxch.details.presentation.components.sections.about
import zed.rainxch.details.presentation.components.sections.author
import zed.rainxch.details.presentation.components.sections.header
import zed.rainxch.details.presentation.components.sections.logs
import zed.rainxch.details.presentation.components.sections.reportIssue
import zed.rainxch.details.presentation.components.sections.stats
import zed.rainxch.details.presentation.components.sections.releaseChannel
import zed.rainxch.details.presentation.components.sections.whatsNew
import zed.rainxch.details.presentation.components.states.ErrorState
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.repo_pages_details_issues_button
import zed.rainxch.githubstore.core.presentation.res.repo_pages_details_pulls_button
import zed.rainxch.githubstore.core.presentation.res.repo_pages_details_security_button
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
    onNavigateToAbout: (repoId: Long, owner: String, repo: String, sourceHost: String?, translateTo: String?) -> Unit,
    onNavigateToWhatsNew: (repoId: Long, owner: String, repo: String, sourceHost: String?) -> Unit,
    onNavigateToIssues: (owner: String, repo: String) -> Unit,
    onNavigateToSecurity: (owner: String, repo: String) -> Unit,
    onNavigateToPulls: (owner: String, repo: String) -> Unit,
    onNavigateToMarkdownViewer: (url: String) -> Unit,
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
                    null,
                )
            }
        },
        onTranslateLanguage = state.repository?.let { repo ->
            { code ->
                onNavigateToAbout(
                    repo.id,
                    repo.owner.login,
                    repo.name,
                    repo.sourceHost,
                    code,
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
        onOpenIssues = state.repository?.takeIf { it.sourceHost == null }?.let { repo ->
            { onNavigateToIssues(repo.owner.login, repo.name) }
        },
        onOpenSecurity = state.repository?.takeIf { it.sourceHost == null }?.let { repo ->
            { onNavigateToSecurity(repo.owner.login, repo.name) }
        },
        onOpenPulls = state.repository?.takeIf { it.sourceHost == null }?.let { repo ->
            { onNavigateToPulls(repo.owner.login, repo.name) }
        },
    )

    state.downgradeWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(DetailsAction.OnDismissDowngradeWarning)
            },
            shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
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
                KomiButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnDismissDowngradeWarning)
                        viewModel.onAction(DetailsAction.UninstallApp)
                    },
                    label = stringResource(Res.string.uninstall_first),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            },
            dismissButton = {
                KomiButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnDismissDowngradeWarning)
                    },
                    label = stringResource(Res.string.cancel),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            },
        )
    }

    state.signingKeyWarning?.let { warning ->
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(DetailsAction.OnDismissSigningKeyWarning)
            },
            shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
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
                KomiButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnOverrideSigningKeyWarning)
                    },
                    label = stringResource(Res.string.install_anyway),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            },
            dismissButton = {
                KomiButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnDismissSigningKeyWarning)
                    },
                    label = stringResource(Res.string.cancel),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            },
        )
    }

    if (state.showUninstallConfirmation) {
        val appName = state.installedApp?.appName ?: ""
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(DetailsAction.OnDismissUninstallConfirmation)
            },
            shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
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
                KomiButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnConfirmUninstall)
                    },
                    label = stringResource(Res.string.uninstall),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            },
            dismissButton = {
                KomiButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnDismissUninstallConfirmation)
                    },
                    label = stringResource(Res.string.cancel),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            },
        )
    }

    if (state.showUnlinkConfirmation) {
        val appName = state.installedApp?.appName ?: ""
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(DetailsAction.OnDismissUnlinkConfirmation)
            },
            shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
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
                KomiButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnConfirmUnlinkExternalApp)
                    },
                    label = stringResource(Res.string.details_unlink_external_app_dialog_confirm),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            },
            dismissButton = {
                KomiButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OnDismissUnlinkConfirmation)
                    },
                    label = stringResource(Res.string.cancel),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            },
        )
    }

    if (state.showExternalInstallerPrompt) {
        AlertDialog(
            onDismissRequest = {
                viewModel.onAction(DetailsAction.DismissExternalInstallerPrompt)
            },
            shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
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
                KomiButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.OpenWithExternalInstaller)
                    },
                    label = stringResource(Res.string.open_with_external_installer),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            },
            dismissButton = {
                KomiButton(
                    onClick = {
                        viewModel.onAction(DetailsAction.DismissExternalInstallerPrompt)
                    },
                    label = stringResource(Res.string.dismiss),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
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
    onTranslateLanguage: ((String) -> Unit)? = null,
    onReadMoreWhatsNew: (() -> Unit)? = null,
    onOpenIssues: (() -> Unit)? = null,
    onOpenSecurity: (() -> Unit)? = null,
    onOpenPulls: (() -> Unit)? = null,
) {
    KomiScaffold(
        topBar = {
            DetailsTopbar(
                state = state,
                onAction = onAction,
            )
        },
        overlay = {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        },
    ) { innerPadding ->

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularWavyProgressIndicator()
                }

                return@KomiScaffold
            }

            if (state.errorMessage != null) {
                ErrorState(state.errorMessage, onAction)

                return@KomiScaffold
            }

            val density = LocalDensity.current
            var containerHeightDp by remember { mutableStateOf(0.dp) }
            val collapsedSectionHeight = containerHeightDp * 0.4f
            val listState = rememberLazyListState()
            val isScrollbarEnabled = LocalScrollbarEnabled.current
            val contentWidthDp = contentWidthCap()
            val pullEnabled = remember { isPullToRefreshSupported() }

            val isDesktop = remember { isDesktop() }
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

                    if (onOpenIssues != null || onOpenSecurity != null || onOpenPulls != null) {
                        item(key = "repo_pages_actions") {
                            RepoPagesActionRow(
                                onOpenIssues = onOpenIssues,
                                onOpenSecurity = onOpenSecurity,
                                onOpenPulls = onOpenPulls,
                            )
                        }
                    }

                    if (state.isComingFromUpdate) {
                        state.selectedRelease?.let { release ->
                            whatsNew(
                                release = release,
                                isExpanded = state.isWhatsNewExpanded,
                                onToggleExpanded = { onAction(DetailsAction.ToggleWhatsNewExpanded) },
                                collapsedHeight = collapsedSectionHeight,
                                measuredHeightPx = state.whatsNewMeasuredHeightPx,
                                onMeasured = { onAction(DetailsAction.OnWhatsNewMeasured(it)) },
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
                                onTranslateLanguage = onTranslateLanguage,
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
                                onTranslateLanguage = onTranslateLanguage,
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
        KomiIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(Res.string.navigate_back),
            onClick = { onAction(DetailsAction.OnNavigateBackClick) },
        )
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

    val entries = buildList {
        add(
            KomiMenuItem(
                id = "star",
                label = stringResource(
                    if (state.isStarred) Res.string.repository_starred
                    else Res.string.repository_not_starred,
                ),
                icon = if (state.isStarred) Icons.Default.Star else Icons.Default.StarBorder,
            ),
        )
        add(
            KomiMenuItem(
                id = "favourite",
                label = stringResource(
                    if (state.isFavourite) Res.string.remove_from_favourites
                    else Res.string.add_to_favourites,
                ),
                icon = if (state.isFavourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
            ),
        )
        if (state.repository?.htmlUrl != null) {
            add(
                KomiMenuItem(
                    id = "share",
                    label = stringResource(Res.string.share_repository),
                    icon = Icons.Default.Share,
                ),
            )
        }
        add(
            KomiMenuItem(
                id = "refresh",
                label = if (cooldownActive) {
                    stringResource(Res.string.details_refresh_cooldown, cooldownSeconds)
                } else {
                    stringResource(Res.string.details_refresh)
                },
                icon = Icons.Default.Refresh,
                enabled = !refreshDisabled,
            ),
        )
        if (state.installedApp?.installSource == InstallSource.MANUAL) {
            add(
                KomiMenuItem(
                    id = "unlink",
                    label = stringResource(Res.string.details_unlink_external_app_menu),
                    icon = Icons.Default.LinkOff,
                ),
            )
        }
    }.toImmutableList()

    val moreOptionsCd = stringResource(Res.string.details_refresh_more_options)
    KomiDropdown(
        entries = entries,
        onSelect = { item ->
            when (item.id) {
                "star" -> onAction(DetailsAction.OnToggleStar)
                "favourite" -> onAction(DetailsAction.OnToggleFavorite)
                "share" -> onAction(DetailsAction.OnShareClick)
                "refresh" -> onAction(DetailsAction.Refresh)
                "unlink" -> onAction(DetailsAction.OnUnlinkExternalApp)
            }
        },
        trigger = { onClick ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = moreOptionsCd,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
    )
}

@Preview
@Composable
private fun Preview() {
    PersonalityPreview {
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

@Composable
private fun RepoPagesActionRow(
    onOpenIssues: (() -> Unit)?,
    onOpenSecurity: (() -> Unit)?,
    onOpenPulls: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        onOpenIssues?.let { open ->
            KomiButton(
                onClick = open,
                label = stringResource(Res.string.repo_pages_details_issues_button),
                variant = KomiButtonVariant.Tonal,
                size = KomiButtonSize.Sm,
                modifier = Modifier.weight(1f),
            )
        }
        onOpenPulls?.let { open ->
            KomiButton(
                onClick = open,
                label = stringResource(Res.string.repo_pages_details_pulls_button),
                variant = KomiButtonVariant.Tonal,
                size = KomiButtonSize.Sm,
                modifier = Modifier.weight(1f),
            )
        }
        onOpenSecurity?.let { open ->
            KomiButton(
                onClick = open,
                label = stringResource(Res.string.repo_pages_details_security_button),
                variant = KomiButtonVariant.Tonal,
                size = KomiButtonSize.Sm,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
