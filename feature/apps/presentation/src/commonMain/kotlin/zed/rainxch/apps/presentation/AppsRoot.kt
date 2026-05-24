@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalTime::class)

package zed.rainxch.apps.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import zed.rainxch.core.presentation.components.overlays.GhsDropdownMenu
import zed.rainxch.core.presentation.components.overlays.GhsDropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.coil3.CoilImage
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.apps.presentation.components.AdvancedAppSettingsBottomSheet
import zed.rainxch.apps.presentation.components.AppsSectionHeader
import zed.rainxch.apps.presentation.components.CompactAppRow
import zed.rainxch.apps.presentation.components.InstalledAppIcon
import zed.rainxch.apps.presentation.components.LinkAppBottomSheet
import zed.rainxch.apps.presentation.components.VariantPickerDialog
import zed.rainxch.apps.presentation.components.KaoBanner
import zed.rainxch.apps.presentation.components.UpdatesBanner
import zed.rainxch.apps.presentation.import.components.ImportProposalBanner
import zed.rainxch.apps.presentation.model.AppItem
import zed.rainxch.apps.presentation.model.AppSortRule
import zed.rainxch.apps.presentation.model.UpdateAllProgress
import zed.rainxch.apps.presentation.model.UpdateState
import zed.rainxch.core.presentation.components.ExpressiveCard
import zed.rainxch.core.presentation.components.ScrollbarContainer
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.add_by_link
import zed.rainxch.githubstore.core.presentation.res.add_from_starred_title
import zed.rainxch.githubstore.core.presentation.res.advanced_settings_open
import zed.rainxch.githubstore.core.presentation.res.apps_compact_more_actions
import zed.rainxch.githubstore.core.presentation.res.apps_ignore_updates
import zed.rainxch.githubstore.core.presentation.res.apps_skip_version
import zed.rainxch.githubstore.core.presentation.res.apps_skip_version_unskip
import zed.rainxch.githubstore.core.presentation.res.apps_section_pending_installs
import zed.rainxch.githubstore.core.presentation.res.apps_section_up_to_date
import zed.rainxch.githubstore.core.presentation.res.install
import zed.rainxch.githubstore.core.presentation.res.ready_to_install
import zed.rainxch.githubstore.core.presentation.res.variant_label_inline
import zed.rainxch.githubstore.core.presentation.res.variant_picker_open
import zed.rainxch.githubstore.core.presentation.res.variant_stale_hint
import zed.rainxch.githubstore.core.presentation.res.cancel
import zed.rainxch.githubstore.core.presentation.res.check_for_updates
import zed.rainxch.githubstore.core.presentation.res.checking
import zed.rainxch.githubstore.core.presentation.res.checking_for_updates
import zed.rainxch.githubstore.core.presentation.res.confirm_uninstall_message
import zed.rainxch.githubstore.core.presentation.res.confirm_uninstall_title
import zed.rainxch.githubstore.core.presentation.res.downloading
import zed.rainxch.githubstore.core.presentation.res.error_with_message
import zed.rainxch.githubstore.core.presentation.res.export_apps
import zed.rainxch.githubstore.core.presentation.res.export_apps_obtainium
import zed.rainxch.githubstore.core.presentation.res.external_import_rescan_menu
import zed.rainxch.githubstore.core.presentation.res.import_apps
import zed.rainxch.githubstore.core.presentation.res.bottom_nav_apps_title
import zed.rainxch.githubstore.core.presentation.res.installing
import zed.rainxch.githubstore.core.presentation.res.last_checked
import zed.rainxch.githubstore.core.presentation.res.last_checked_hours_ago
import zed.rainxch.githubstore.core.presentation.res.last_checked_just_now
import zed.rainxch.githubstore.core.presentation.res.last_checked_minutes_ago
import zed.rainxch.githubstore.core.presentation.res.no_apps_found
import zed.rainxch.githubstore.core.presentation.res.open
import zed.rainxch.githubstore.core.presentation.res.pending_install
import zed.rainxch.githubstore.core.presentation.res.pre_release_badge
import zed.rainxch.githubstore.core.presentation.res.search_your_apps
import zed.rainxch.githubstore.core.presentation.res.sort_apps
import zed.rainxch.githubstore.core.presentation.res.sort_name
import zed.rainxch.githubstore.core.presentation.res.sort_recently_updated
import zed.rainxch.githubstore.core.presentation.res.sort_updates_first
import zed.rainxch.githubstore.core.presentation.res.uninstall
import zed.rainxch.githubstore.core.presentation.res.confirm_discard_pending_message
import zed.rainxch.githubstore.core.presentation.res.confirm_discard_pending_title
import zed.rainxch.githubstore.core.presentation.res.discard_pending_install
import zed.rainxch.githubstore.core.presentation.res.update
import zed.rainxch.githubstore.core.presentation.res.updated_successfully

@Composable
fun AppsRoot(
    onNavigateBack: () -> Unit,
    onNavigateToRepo: (repoId: Long, sourceHost: String?, owner: String?, repo: String?) -> Unit,
    onNavigateToExternalImport: () -> Unit,
    onNavigateToStarredPicker: () -> Unit,
    viewModel: AppsViewModel = koinViewModel(),
    state: AppsState,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.onAction(AppsAction.OnLifecycleResume)
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AppsEvent.NavigateToRepo -> {
                onNavigateToRepo(event.repoId, event.sourceHost, event.owner, event.repo)
            }

            is AppsEvent.ShowError -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }

            is AppsEvent.ShowSuccess -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(event.message)
                }
            }

            is AppsEvent.AppLinkedSuccessfully -> {
            }

            is AppsEvent.ImportComplete -> {
            }

            AppsEvent.NavigateToExternalImport -> {
                onNavigateToExternalImport()
            }
        }
    }

    AppsScreen(
        state = state,
        onAction = { action ->
            when (action) {
                AppsAction.OnNavigateBackClick -> {
                    onNavigateBack()
                }

                AppsAction.OnAddFromStarredClick -> {
                    onNavigateToStarredPicker()
                }

                else -> {
                    viewModel.onAction(action)
                }
            }
        },
        snackbarHostState = snackbarHostState,
    )
}

@Composable
fun AppsScreen(
    state: AppsState,
    onAction: (AppsAction) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val bottomNavHeight = LocalBottomNavigationHeight.current
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(Res.string.bottom_nav_apps_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 4.dp),
                )
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
                    Box {
                        Box(
                            modifier = Modifier
                                .clickable { showSortMenu = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(Res.string.sort_apps),
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        GhsDropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            GhsDropdownMenuItem(
                                text = stringResource(Res.string.sort_updates_first),
                                onClick = {
                                    showSortMenu = false
                                    onAction(AppsAction.OnSortRuleSelected(AppSortRule.UpdatesFirst))
                                },
                            )
                            GhsDropdownMenuItem(
                                text = stringResource(Res.string.sort_recently_updated),
                                onClick = {
                                    showSortMenu = false
                                    onAction(AppsAction.OnSortRuleSelected(AppSortRule.RecentlyUpdated))
                                },
                            )
                            GhsDropdownMenuItem(
                                text = stringResource(Res.string.sort_name),
                                onClick = {
                                    showSortMenu = false
                                    onAction(AppsAction.OnSortRuleSelected(AppSortRule.Name))
                                },
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(1.dp, 20.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    )
                    Box(
                        modifier = Modifier
                            .clickable { onAction(AppsAction.OnCheckAllForUpdates) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.check_for_updates),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(1.dp, 20.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    )
                    Box {
                        Box(
                            modifier = Modifier
                                .clickable { showOverflowMenu = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        GhsDropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            GhsDropdownMenuItem(
                                text = stringResource(Res.string.export_apps),
                                onClick = {
                                    showOverflowMenu = false
                                    onAction(AppsAction.OnExportApps)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                },
                            )
                            GhsDropdownMenuItem(
                                text = stringResource(Res.string.export_apps_obtainium),
                                onClick = {
                                    showOverflowMenu = false
                                    onAction(AppsAction.OnExportObtainium)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                },
                            )
                            GhsDropdownMenuItem(
                                text = stringResource(Res.string.import_apps),
                                onClick = {
                                    showOverflowMenu = false
                                    onAction(AppsAction.OnImportApps)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                                },
                            )
                            GhsDropdownMenuItem(
                                text = stringResource(Res.string.external_import_rescan_menu),
                                onClick = {
                                    showOverflowMenu = false
                                    onAction(AppsAction.OnRescanForGithubApps)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Search, contentDescription = null)
                                },
                            )
                            GhsDropdownMenuItem(
                                text = stringResource(Res.string.add_from_starred_title),
                                onClick = {
                                    showOverflowMenu = false
                                    onAction(AppsAction.OnAddFromStarredClick)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAction(AppsAction.OnAddByLinkClick) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                    )
                },
                text = { Text(stringResource(Res.string.add_by_link)) },
                modifier =
                    Modifier
                        .navigationBarsPadding()
                        .padding(bottom = bottomNavHeight),
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottomNavHeight + 16.dp),
            )
        },
    ) { innerPadding ->

        if (state.showLinkSheet) {
            LinkAppBottomSheet(
                state = state,
                onAction = onAction,
            )
        }

        if (state.advancedSettingsApp != null) {
            AdvancedAppSettingsBottomSheet(
                state = state,
                onAction = onAction,
            )
        }

        if (state.variantPickerApp != null) {
            VariantPickerDialog(
                state = state,
                onAction = onAction,
            )
        }

        state.importSummary?.let { summary ->
            zed.rainxch.apps.presentation.components.ImportSummarySheet(
                summary = summary,
                onDismiss = { onAction(AppsAction.OnDismissImportSummary) },
            )
        }

        state.appPendingUninstall?.let { app ->
            AlertDialog(
                onDismissRequest = { onAction(AppsAction.OnDismissUninstallDialog) },
                title = {
                    Text(
                        text = stringResource(Res.string.confirm_uninstall_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Text(
                        text = stringResource(Res.string.confirm_uninstall_message, app.appName),
                    )
                },
                confirmButton = {
                    GhsButton(
                        onClick = { onAction(AppsAction.OnUninstallConfirmed(app)) },
                        label = stringResource(Res.string.uninstall),
                        variant = GhsButtonVariant.Text,
                        size = GhsButtonSize.Sm,
                    )
                },
                dismissButton = {
                    GhsButton(
                        onClick = { onAction(AppsAction.OnDismissUninstallDialog) },
                        label = stringResource(Res.string.cancel),
                        variant = GhsButtonVariant.Text,
                        size = GhsButtonSize.Sm,
                    )
                },
            )
        }

        state.appPendingDiscard?.let { app ->
            AlertDialog(
                onDismissRequest = { onAction(AppsAction.OnDismissDiscardPendingDialog) },
                title = {
                    Text(
                        text = stringResource(Res.string.confirm_discard_pending_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            Res.string.confirm_discard_pending_message,
                            app.appName,
                        ),
                    )
                },
                confirmButton = {
                    GhsButton(
                        onClick = { onAction(AppsAction.OnConfirmDiscardPendingInstall(app)) },
                        label = stringResource(Res.string.discard_pending_install),
                        variant = GhsButtonVariant.Text,
                        size = GhsButtonSize.Sm,
                    )
                },
                dismissButton = {
                    GhsButton(
                        onClick = { onAction(AppsAction.OnDismissDiscardPendingDialog) },
                        label = stringResource(Res.string.cancel),
                        variant = GhsButtonVariant.Text,
                        size = GhsButtonSize.Sm,
                    )
                },
            )
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onAction(AppsAction.OnRefresh) },
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                GhsTextField(
                    value = state.searchQuery,
                    onValueChange = { onAction(AppsAction.OnSearchChange(it)) },
                    leadingIcon = Icons.Default.Search,
                    placeholder = stringResource(Res.string.search_your_apps),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                if (state.isCheckingForUpdates) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(Res.string.checking_for_updates),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (state.lastCheckedTimestamp != null) {
                    Text(
                        text =
                            stringResource(
                                Res.string.last_checked,
                                formatLastChecked(state.lastCheckedTimestamp),
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.filteredApps.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(Res.string.no_apps_found),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                    }

                    else -> {
                        val listState = rememberLazyListState()
                        val isScrollbarEnabled = LocalScrollbarEnabled.current

                        val pendingGroup =
                            state.filteredApps.filter {
                                it.installedApp.isPendingInstall &&
                                    it.installedApp.pendingInstallFilePath != null
                            }
                        val updatesGroup =
                            state.filteredApps.filter {
                                it.installedApp.isUpdateAvailable &&
                                    it.installedApp.updateCheckEnabled &&
                                    !it.installedApp.isPendingInstall
                            }
                        val idleGroup =
                            state.filteredApps.filter {
                                (!it.installedApp.isUpdateAvailable || !it.installedApp.updateCheckEnabled) &&
                                    !it.installedApp.isPendingInstall
                            }

                        val onRowSelect: (zed.rainxch.apps.presentation.model.InstalledAppUi) -> Unit =
                            { app ->
                                onAction(
                                    AppsAction.OnNavigateToRepo(
                                        repoId = app.repoId,
                                        sourceHost = app.sourceHost,
                                        owner = app.repoOwner,
                                        repo = app.repoName,
                                    ),
                                )
                            }

                        ScrollbarContainer(
                            listState = listState,
                            enabled = isScrollbarEnabled,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().arrowKeyScroll(listState),

                                contentPadding = PaddingValues(
                                    start = 0.dp,
                                    end = 0.dp,
                                    top = 8.dp,
                                    bottom = 88.dp,
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (state.showImportProposalBanner) {
                                    item(key = "external-import-banner") {
                                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            ImportProposalBanner(
                                                pendingCount = state.pendingExternalImportCount,
                                                onReview = { onAction(AppsAction.OnImportProposalReview) },
                                                onDismiss = { onAction(AppsAction.OnImportProposalDismiss) },
                                            )
                                        }
                                    }
                                }

                                if (state.showKaoBanner) {
                                    item(key = "kao-banner") {
                                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            KaoBanner(
                                                onLearnMore = { onAction(AppsAction.OnKaoLearnMore) },
                                                onDismiss = { onAction(AppsAction.OnDismissKaoBanner) },
                                            )
                                        }
                                    }
                                }

                                if (pendingGroup.isNotEmpty()) {
                                    item(key = "header-pending-installs") {
                                        AppsSectionHeader(
                                            title = stringResource(Res.string.apps_section_pending_installs),
                                            count = pendingGroup.size,
                                            isExpanded = true,
                                            collapsible = false,
                                            onToggle = {},
                                        )
                                    }
                                    items(
                                        items = pendingGroup,
                                        key = { "pending-${it.installedApp.packageName}" },
                                    ) { appItem ->
                                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            AppItemCard(
                                                appItem = appItem,
                                                onOpenClick = { onAction(AppsAction.OnOpenApp(appItem.installedApp)) },
                                                onUpdateClick = { onAction(AppsAction.OnUpdateApp(appItem.installedApp)) },
                                                onCancelClick = { onAction(AppsAction.OnCancelUpdate(appItem.installedApp.packageName)) },
                                                onUninstallClick = { onAction(AppsAction.OnUninstallApp(appItem.installedApp)) },
                                                onRepoClick = { onRowSelect(appItem.installedApp) },
                                                onTogglePreReleases = { enabled ->
                                                    onAction(AppsAction.OnTogglePreReleases(appItem.installedApp.packageName, enabled))
                                                },
                                                onToggleUpdateCheck = { enabled ->
                                                    onAction(AppsAction.OnToggleUpdateCheck(appItem.installedApp.packageName, enabled))
                                                },
                                                onAdvancedSettingsClick = {
                                                    onAction(AppsAction.OnOpenAdvancedSettings(appItem.installedApp))
                                                },
                                                onPickVariantClick = {
                                                    onAction(
                                                        AppsAction.OnOpenVariantPicker(
                                                            app = appItem.installedApp,
                                                            resumeUpdateAfterPick = false,
                                                        ),
                                                    )
                                                },
                                                onInstallPendingClick = {
                                                    onAction(AppsAction.OnInstallPendingApp(appItem.installedApp))
                                                },
                                                onDiscardPendingClick = {
                                                    onAction(AppsAction.OnDiscardPendingInstall(appItem.installedApp))
                                                },
                                                onSkipVersionClick = {
                                                    val tag =
                                                        appItem.installedApp.latestVersion
                                                            ?: appItem.installedApp.latestVersionName
                                                    if (!tag.isNullOrBlank()) {
                                                        onAction(
                                                            AppsAction.OnSkipReleaseTag(
                                                                appItem.installedApp.packageName,
                                                                tag,
                                                            ),
                                                        )
                                                    }
                                                },
                                                onUnskipVersionClick = {
                                                    onAction(AppsAction.OnUnskipReleaseTag(appItem.installedApp.packageName))
                                                },
                                            )
                                        }
                                    }
                                }

                                if (updatesGroup.isNotEmpty() || state.isUpdatingAll) {
                                    item(key = "updates-banner") {
                                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                            UpdatesBanner(
                                                count = updatesGroup.size,
                                                isExpanded = state.isUpdatesSectionExpanded,
                                                isUpdatingAll = state.isUpdatingAll,
                                                updateAllProgress = state.updateAllProgress,
                                                updateAllEnabled = state.updateAllButtonEnabled,
                                                onUpdateAll = { onAction(AppsAction.OnUpdateAll) },
                                                onCancelUpdateAll = { onAction(AppsAction.OnCancelUpdateAll) },
                                                onToggleExpanded = { onAction(AppsAction.OnToggleUpdatesSection) },
                                            )
                                        }
                                    }
                                }

                                if (updatesGroup.isNotEmpty() && state.isUpdatesSectionExpanded) {
                                    items(
                                        items = updatesGroup,
                                        key = { "rich-${it.installedApp.packageName}" },
                                    ) { appItem ->
                                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            AppItemCard(
                                                appItem = appItem,
                                                onOpenClick = { onAction(AppsAction.OnOpenApp(appItem.installedApp)) },
                                                onUpdateClick = { onAction(AppsAction.OnUpdateApp(appItem.installedApp)) },
                                                onCancelClick = { onAction(AppsAction.OnCancelUpdate(appItem.installedApp.packageName)) },
                                                onUninstallClick = { onAction(AppsAction.OnUninstallApp(appItem.installedApp)) },
                                                onRepoClick = { onRowSelect(appItem.installedApp) },
                                                onTogglePreReleases = { enabled ->
                                                    onAction(AppsAction.OnTogglePreReleases(appItem.installedApp.packageName, enabled))
                                                },
                                                onToggleUpdateCheck = { enabled ->
                                                    onAction(AppsAction.OnToggleUpdateCheck(appItem.installedApp.packageName, enabled))
                                                },
                                                onAdvancedSettingsClick = {
                                                    onAction(AppsAction.OnOpenAdvancedSettings(appItem.installedApp))
                                                },
                                                onPickVariantClick = {
                                                    onAction(
                                                        AppsAction.OnOpenVariantPicker(
                                                            app = appItem.installedApp,
                                                            resumeUpdateAfterPick = false,
                                                        ),
                                                    )
                                                },
                                                onInstallPendingClick = {
                                                    onAction(AppsAction.OnInstallPendingApp(appItem.installedApp))
                                                },
                                                onDiscardPendingClick = {
                                                    onAction(AppsAction.OnDiscardPendingInstall(appItem.installedApp))
                                                },
                                                onSkipVersionClick = {
                                                    val tag =
                                                        appItem.installedApp.latestVersion
                                                            ?: appItem.installedApp.latestVersionName
                                                    if (!tag.isNullOrBlank()) {
                                                        onAction(
                                                            AppsAction.OnSkipReleaseTag(
                                                                appItem.installedApp.packageName,
                                                                tag,
                                                            ),
                                                        )
                                                    }
                                                },
                                                onUnskipVersionClick = {
                                                    onAction(AppsAction.OnUnskipReleaseTag(appItem.installedApp.packageName))
                                                },
                                            )
                                        }
                                    }
                                }

                                if (idleGroup.isNotEmpty()) {
                                    item(key = "header-up-to-date") {
                                        AppsSectionHeader(
                                            title = stringResource(Res.string.apps_section_up_to_date),
                                            count = idleGroup.size,
                                            isExpanded = state.isUpToDateSectionExpanded,
                                            collapsible = true,
                                            onToggle = {
                                                onAction(AppsAction.OnToggleUpToDateSection)
                                            },
                                        )
                                    }
                                    if (state.isUpToDateSectionExpanded) {
                                        items(
                                            items = idleGroup,
                                            key = { "compact-${it.installedApp.packageName}" },
                                        ) { appItem ->
                                            Box(modifier = Modifier.padding(horizontal = 8.dp)) {
                                                CompactAppRow(
                                                    appItem = appItem,
                                                    onOpenClick = { onAction(AppsAction.OnOpenApp(appItem.installedApp)) },
                                                    onInstallPendingClick = {
                                                        onAction(AppsAction.OnInstallPendingApp(appItem.installedApp))
                                                    },
                                                    onDiscardPendingClick = {
                                                        onAction(AppsAction.OnDiscardPendingInstall(appItem.installedApp))
                                                    },
                                                    onAdvancedSettingsClick = {
                                                        onAction(AppsAction.OnOpenAdvancedSettings(appItem.installedApp))
                                                    },
                                                    onPickVariantClick = {
                                                        onAction(
                                                            AppsAction.OnOpenVariantPicker(
                                                                app = appItem.installedApp,
                                                                resumeUpdateAfterPick = false,
                                                            ),
                                                        )
                                                    },
                                                    onUninstallClick = {
                                                        onAction(AppsAction.OnUninstallApp(appItem.installedApp))
                                                    },
                                                    onTogglePreReleases = { enabled ->
                                                        onAction(
                                                            AppsAction.OnTogglePreReleases(
                                                                appItem.installedApp.packageName,
                                                                enabled,
                                                            ),
                                                        )
                                                    },
                                                    onToggleUpdateCheck = { enabled ->
                                                        onAction(
                                                            AppsAction.OnToggleUpdateCheck(
                                                                appItem.installedApp.packageName,
                                                                enabled,
                                                            ),
                                                        )
                                                    },
                                                    onUnskipVersionClick = {
                                                        onAction(
                                                            AppsAction.OnUnskipReleaseTag(
                                                                appItem.installedApp.packageName,
                                                            ),
                                                        )
                                                    },
                                                    onRowClick = { onRowSelect(appItem.installedApp) },
                                                )
                                            }
                                        }
                                    }
                                }

                                item {
                                    Spacer(Modifier.height(bottomNavHeight + 32.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppItemCard(
    appItem: AppItem,
    onOpenClick: () -> Unit,
    onUpdateClick: () -> Unit,
    onCancelClick: () -> Unit,
    onUninstallClick: () -> Unit,
    onRepoClick: () -> Unit,
    onTogglePreReleases: (Boolean) -> Unit,
    onToggleUpdateCheck: (Boolean) -> Unit,
    onAdvancedSettingsClick: () -> Unit,
    onPickVariantClick: () -> Unit,
    onInstallPendingClick: () -> Unit,
    onDiscardPendingClick: () -> Unit,
    onSkipVersionClick: () -> Unit,
    onUnskipVersionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = appItem.installedApp
    val isBusy =
        app.isPendingInstall ||
            appItem.updateState is UpdateState.Downloading ||
            appItem.updateState is UpdateState.Installing ||
            appItem.updateState is UpdateState.CheckingUpdate

    ExpressiveCard(
        onClick = onRepoClick,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(32.dp))
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                InstalledAppIcon(
                    packageName = app.packageName,
                    appName = app.appName,
                    modifier =
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp)),
                    apkFilePath = app.pendingInstallFilePath,
                )

                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        CoilImage(
                            imageModel = { app.repoOwnerAvatarUrl },
                            modifier =
                                Modifier
                                    .size(18.dp)
                                    .clip(CircleShape),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularWavyProgressIndicator()
                                }
                            },
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = app.repoOwner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        app.sourceHost?.let {
                            Spacer(Modifier.width(6.dp))
                            zed.rainxch.apps.presentation.components.SourceChip(host = it)
                        }
                    }

                    when {

                        app.pendingInstallFilePath != null -> {
                            Text(
                                text = stringResource(Res.string.ready_to_install),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        app.isPendingInstall -> {
                            Text(
                                text = stringResource(Res.string.pending_install),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }

                        app.preferredVariantStale -> {

                            Text(
                                text = stringResource(Res.string.variant_stale_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier =
                                    Modifier.clickable(
                                        enabled = !isBusy,
                                        onClick = onPickVariantClick,
                                    ),
                            )
                        }

                        app.isUpdateAvailable -> {
                            Text(
                                text =
                                    buildVersionLabel(
                                        installedVersion = app.installedVersion,
                                        latestVersion = app.latestVersion,
                                        latestReleasePublishedAt = app.latestReleasePublishedAt,
                                        lastUpdatedAt = app.lastUpdatedAt,
                                    ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            if (!app.preferredAssetVariant.isNullOrBlank()) {
                                Text(
                                    text =
                                        stringResource(
                                            Res.string.variant_label_inline,
                                            app.preferredAssetVariant,
                                        ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        else -> {
                            Text(
                                text =
                                    buildVersionLabel(
                                        installedVersion = app.installedVersion,
                                        latestVersion = null,
                                        latestReleasePublishedAt = null,
                                        lastUpdatedAt = app.lastUpdatedAt,
                                    ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }

            if (app.repoDescription != null) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = app.repoDescription,
                    style = MaterialTheme.typography.bodyMediumEmphasized,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val preReleaseString = stringResource(Res.string.pre_release_badge)
                Text(
                    text = preReleaseString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {

                    val advancedFilterDescription =
                        stringResource(Res.string.advanced_settings_open)
                    val hasFilter =
                        !app.assetFilterRegex.isNullOrBlank() || app.fallbackToOlderReleases
                    IconButton(
                        onClick = onAdvancedSettingsClick,
                        enabled = !isBusy,
                        modifier = Modifier.semantics {
                            contentDescription = advancedFilterDescription
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterAlt,
                            contentDescription = null,
                            tint =
                                if (hasFilter) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }

                    val pickVariantDescription =
                        stringResource(Res.string.variant_picker_open)
                    val hasPin = !app.preferredAssetVariant.isNullOrBlank()
                    IconButton(
                        onClick = onPickVariantClick,
                        enabled = !isBusy,
                        modifier = Modifier.semantics {
                            contentDescription = pickVariantDescription
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint =
                                when {
                                    app.preferredVariantStale -> MaterialTheme.colorScheme.error
                                    hasPin -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }

                    Checkbox(
                        checked = app.includePreReleases,
                        onCheckedChange = onTogglePreReleases,
                        enabled = !isBusy,
                        modifier =
                            Modifier.semantics {
                                contentDescription = preReleaseString
                            },
                    )

                    var showRowOverflow by remember { mutableStateOf(false) }
                    val moreActionsLabel =
                        stringResource(Res.string.apps_compact_more_actions, app.appName)
                    Box {
                        IconButton(
                            onClick = { showRowOverflow = true },
                            enabled = !isBusy,
                            modifier = Modifier.semantics {
                                contentDescription = moreActionsLabel
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        GhsDropdownMenu(
                            expanded = showRowOverflow,
                            onDismissRequest = { showRowOverflow = false },
                        ) {
                            run {
                                val baseLabel = stringResource(Res.string.apps_ignore_updates)
                                GhsDropdownMenuItem(
                                    text = if (!app.updateCheckEnabled) "$baseLabel  ✓" else baseLabel,
                                    onClick = {
                                        showRowOverflow = false
                                        onToggleUpdateCheck(!app.updateCheckEnabled)
                                    },
                                )
                            }

                            if (app.skippedReleaseTag != null) {
                                GhsDropdownMenuItem(
                                    text = stringResource(Res.string.apps_skip_version_unskip),
                                    onClick = {
                                        showRowOverflow = false
                                        onUnskipVersionClick()
                                    },
                                )
                            } else if (app.isUpdateAvailable && !(app.latestVersion ?: app.latestVersionName).isNullOrBlank()) {
                                GhsDropdownMenuItem(
                                    text = stringResource(Res.string.apps_skip_version),
                                    onClick = {
                                        showRowOverflow = false
                                        onSkipVersionClick()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (val state = appItem.updateState) {
                is UpdateState.Downloading -> {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(Res.string.downloading),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            if (appItem.downloadProgress != null) {
                                Text(
                                    text = "${appItem.downloadProgress}%",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearWavyProgressIndicator(
                            progress = { (appItem.downloadProgress ?: 0) / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                is UpdateState.Installing -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(Res.string.installing),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                is UpdateState.CheckingUpdate -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(Res.string.checking),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }

                is UpdateState.Success -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(Res.string.updated_successfully),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                is UpdateState.Error -> {
                    Text(
                        text = stringResource(Res.string.error_with_message, state.message),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                UpdateState.Idle -> {}
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onUninstallClick,
                    enabled = !isBusy,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(Res.string.uninstall),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }

                when (appItem.updateState) {
                    is UpdateState.Downloading, is UpdateState.Installing, is UpdateState.CheckingUpdate -> {
                        GhsButton(
                            onClick = onCancelClick,
                            label = stringResource(Res.string.cancel),
                            variant = GhsButtonVariant.Destructive,
                            leadingIcon = Icons.Default.Cancel,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    else -> {
                        if (app.pendingInstallFilePath != null) {

                            GhsButton(
                                onClick = onInstallPendingClick,
                                label = stringResource(Res.string.install),
                                variant = GhsButtonVariant.Primary,
                                leadingIcon = Icons.Default.Update,
                                enabled = !isBusy,
                                modifier = Modifier.weight(1f),
                            )

                            IconButton(onClick = onDiscardPendingClick) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = stringResource(Res.string.discard_pending_install),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else if (app.isUpdateAvailable && !app.isPendingInstall) {
                            GhsButton(
                                onClick = onUpdateClick,
                                label = stringResource(Res.string.update),
                                variant = GhsButtonVariant.Primary,
                                leadingIcon = Icons.Default.Update,
                                modifier = Modifier.weight(1f),
                            )
                        } else if (app.isPendingInstall) {

                            GhsButton(
                                onClick = onDiscardPendingClick,
                                label = stringResource(Res.string.discard_pending_install),
                                variant = GhsButtonVariant.Destructive,
                                leadingIcon = Icons.Default.Cancel,
                                modifier = Modifier.weight(1f),
                            )
                        } else {
                            GhsButton(
                                onClick = onOpenClick,
                                label = stringResource(Res.string.open),
                                variant = GhsButtonVariant.Primary,
                                leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
                                enabled = !isBusy,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun buildVersionLabel(
    installedVersion: String,
    latestVersion: String?,
    latestReleasePublishedAt: String?,
    lastUpdatedAt: Long,
): String {
    val displayDate =
        if (latestVersion != null) {
            formatIsoDate(latestReleasePublishedAt)
        } else {
            formatEpochDate(lastUpdatedAt)
        }

    return buildString {
        append(installedVersion)
        if (latestVersion != null) {
            append(" → ")
            append(latestVersion)
        }
        displayDate?.let {
            append(" (")
            append(it)
            append(")")
        }
    }
}

private fun formatIsoDate(isoTimestamp: String?): String? {
    if (isoTimestamp.isNullOrBlank()) return null

    return try {
        Instant
            .parse(isoTimestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
    } catch (_: IllegalArgumentException) {
        null
    }
}

private fun formatEpochDate(timestamp: Long): String? {
    if (timestamp <= 0L) return null
    return Instant
        .fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}

@Composable
private fun formatLastChecked(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val minutes = diff / (60 * 1000)
    val hours = diff / (60 * 60 * 1000)

    return when {
        minutes < 1 -> stringResource(Res.string.last_checked_just_now)
        minutes < 60 -> stringResource(Res.string.last_checked_minutes_ago, minutes.toInt())
        else -> stringResource(Res.string.last_checked_hours_ago, hours.toInt())
    }
}

@Preview
@Composable
private fun Preview() {
    GithubStoreTheme {
        AppsScreen(
            state = AppsState(),
            onAction = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}
