@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalTime::class)

package zed.rainxch.apps.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
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
import zed.rainxch.apps.presentation.import.components.ImportProposalBanner
import zed.rainxch.apps.presentation.model.AppItem
import zed.rainxch.apps.presentation.model.AppSortRule
import zed.rainxch.apps.presentation.model.UpdateAllProgress
import zed.rainxch.apps.presentation.model.UpdateState
import zed.rainxch.core.presentation.components.ExpressiveCard
import zed.rainxch.core.presentation.components.ScrollbarContainer
import zed.rainxch.core.presentation.locals.LocalBottomNavigationHeight
import zed.rainxch.core.presentation.locals.LocalScrollbarEnabled
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.ObserveAsEvents
import zed.rainxch.core.presentation.utils.arrowKeyScroll
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.add_by_link
import zed.rainxch.githubstore.core.presentation.res.add_from_starred_title
import zed.rainxch.githubstore.core.presentation.res.direct_url_menu_title
import zed.rainxch.githubstore.core.presentation.res.advanced_settings_open
import zed.rainxch.githubstore.core.presentation.res.apps_compact_more_actions
import zed.rainxch.githubstore.core.presentation.res.apps_ignore_updates
import zed.rainxch.githubstore.core.presentation.res.apps_skip_version
import zed.rainxch.githubstore.core.presentation.res.apps_skip_version_unskip
import zed.rainxch.githubstore.core.presentation.res.apps_section_up_to_date
import zed.rainxch.githubstore.core.presentation.res.apps_section_updates_available
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
import zed.rainxch.githubstore.core.presentation.res.currently_updating
import zed.rainxch.githubstore.core.presentation.res.downloading
import zed.rainxch.githubstore.core.presentation.res.error_with_message
import zed.rainxch.githubstore.core.presentation.res.export_apps
import zed.rainxch.githubstore.core.presentation.res.export_apps_obtainium
import zed.rainxch.githubstore.core.presentation.res.external_import_rescan_menu
import zed.rainxch.githubstore.core.presentation.res.import_apps
import zed.rainxch.githubstore.core.presentation.res.installed_apps
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
import zed.rainxch.githubstore.core.presentation.res.update_all
import zed.rainxch.githubstore.core.presentation.res.updated_successfully
import zed.rainxch.githubstore.core.presentation.res.updating_x_of_y

@Composable
fun AppsRoot(
    onNavigateBack: () -> Unit,
    onNavigateToRepo: (repoId: Long) -> Unit,
    onNavigateToExternalImport: () -> Unit,
    onNavigateToStarredPicker: () -> Unit,
    viewModel: AppsViewModel = koinViewModel(),
    state: AppsState,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Re-entry sync: fires the cooldown-gated update check whenever the
    // user returns to the Apps screen. Catches external installs that
    // `PackageEventReceiver` missed when GHS was background-killed by
    // an aggressive OEM ROM. The VM debounces network calls via
    // `UPDATE_CHECK_COOLDOWN_MS` (30 min) so rapid resumes are cheap.
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
                onNavigateToRepo(event.repoId)
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

            is AppsEvent.AppLinkedSuccessfully -> { // handled by ShowSuccess
            }

            is AppsEvent.ImportComplete -> { // handled by ShowSuccess
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
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.installed_apps),
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                },
                actions = {
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = stringResource(Res.string.sort_apps),
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.sort_updates_first)) },
                                onClick = {
                                    showSortMenu = false
                                    onAction(AppsAction.OnSortRuleSelected(AppSortRule.UpdatesFirst))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.sort_recently_updated)) },
                                onClick = {
                                    showSortMenu = false
                                    onAction(AppsAction.OnSortRuleSelected(AppSortRule.RecentlyUpdated))
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.sort_name)) },
                                onClick = {
                                    showSortMenu = false
                                    onAction(AppsAction.OnSortRuleSelected(AppSortRule.Name))
                                },
                            )
                        }
                    }

                    IconButton(
                        onClick = { onAction(AppsAction.OnCheckAllForUpdates) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.check_for_updates),
                        )
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.export_apps)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onAction(AppsAction.OnExportApps)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.export_apps_obtainium)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onAction(AppsAction.OnExportObtainium)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FileUpload, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.import_apps)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onAction(AppsAction.OnImportApps)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.external_import_rescan_menu)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onAction(AppsAction.OnRescanForGithubApps)
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Search, contentDescription = null)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.add_from_starred_title)) },
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
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.direct_url_menu_title)) },
                                onClick = {
                                    showOverflowMenu = false
                                    onAction(AppsAction.OnAddDirectUrlClick)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Link,
                                        contentDescription = null,
                                    )
                                },
                            )
                        }
                    }
                },
            )
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

        // Link app bottom sheet
        if (state.showLinkSheet) {
            LinkAppBottomSheet(
                state = state,
                onAction = onAction,
            )
        }

        if (state.showDirectUrlSheet) {
            zed.rainxch.apps.presentation.components.DirectUrlBottomSheet(
                state = state,
                onAction = onAction,
            )
        }

        // Per-app advanced settings (monorepo filter / fallback)
        if (state.advancedSettingsApp != null) {
            AdvancedAppSettingsBottomSheet(
                state = state,
                onAction = onAction,
            )
        }

        // Variant picker dialog (shown for stale variants or explicit picks)
        if (state.variantPickerApp != null) {
            VariantPickerDialog(
                state = state,
                onAction = onAction,
            )
        }

        // Import summary sheet
        state.importSummary?.let { summary ->
            zed.rainxch.apps.presentation.components.ImportSummarySheet(
                summary = summary,
                onDismiss = { onAction(AppsAction.OnDismissImportSummary) },
            )
        }

        // Uninstall confirmation dialog
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
                    TextButton(
                        onClick = { onAction(AppsAction.OnUninstallConfirmed(app)) },
                    ) {
                        Text(
                            text = stringResource(Res.string.uninstall),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onAction(AppsAction.OnDismissUninstallDialog) },
                    ) {
                        Text(text = stringResource(Res.string.cancel))
                    }
                },
            )
        }

        // Discard-pending-install confirmation dialog. Mirrors the
        // uninstall flow because both branches blow away DB rows the
        // user might still want — the discard target also deletes the
        // parked APK from disk, so the prompt is doubly warranted.
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
                    TextButton(
                        onClick = { onAction(AppsAction.OnConfirmDiscardPendingInstall(app)) },
                    ) {
                        Text(
                            text = stringResource(Res.string.discard_pending_install),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onAction(AppsAction.OnDismissDiscardPendingDialog) },
                    ) {
                        Text(text = stringResource(Res.string.cancel))
                    }
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
                TextField(
                    value = state.searchQuery,
                    onValueChange = { onAction(AppsAction.OnSearchChange(it)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    placeholder = { Text(stringResource(Res.string.search_your_apps)) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = CircleShape,
                    colors =
                        TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
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
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }

                val hasUpdates =
                    state.apps.any {
                        it.installedApp.isUpdateAvailable && it.installedApp.updateCheckEnabled
                    }
                if (hasUpdates && !state.isUpdatingAll) {
                    Button(
                        onClick = { onAction(AppsAction.OnUpdateAll) },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        enabled = state.updateAllButtonEnabled,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Update,
                            contentDescription = null,
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = stringResource(Res.string.update_all),
                        )
                    }
                }

                if (state.isUpdatingAll && state.updateAllProgress != null) {
                    UpdateAllProgressCard(
                        progress = state.updateAllProgress,
                        onCancel = {
                            onAction(AppsAction.OnCancelUpdateAll)
                        },
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

                        // Split filteredApps into the "Updates available" group (rich
                        // rows) and the "Up to date" group (compact rows) — issue #463.
                        // Sort order is already applied by the ViewModel.
                        val updatesGroup =
                            state.filteredApps.filter {
                                it.installedApp.isUpdateAvailable && it.installedApp.updateCheckEnabled
                            }
                        val idleGroup =
                            state.filteredApps.filter {
                                !it.installedApp.isUpdateAvailable || !it.installedApp.updateCheckEnabled
                            }

                        ScrollbarContainer(
                            listState = listState,
                            enabled = isScrollbarEnabled,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize().arrowKeyScroll(listState),
                                // Bottom inset clears the Add-by-link FAB so
                                // the last list item isn't hidden under it.
                                // FAB ≈ 56dp + 16dp scaffold inset + 16dp
                                // breathing room.
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

                                if (updatesGroup.isNotEmpty()) {
                                    item(key = "header-updates-available") {
                                        AppsSectionHeader(
                                            title = stringResource(Res.string.apps_section_updates_available),
                                            count = updatesGroup.size,
                                            isExpanded = true,
                                            collapsible = false,
                                            onToggle = {},
                                        )
                                    }
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
                                                onRepoClick = {
                                                    if (appItem.installedApp.installSource != zed.rainxch.core.domain.model.InstallSource.DIRECT_URL) {
                                                        onAction(AppsAction.OnNavigateToRepo(appItem.installedApp.repoId))
                                                    }
                                                },
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
                                                    onRowClick = {
                                                        if (appItem.installedApp.installSource != zed.rainxch.core.domain.model.InstallSource.DIRECT_URL) {
                                                            onAction(AppsAction.OnNavigateToRepo(appItem.installedApp.repoId))
                                                        }
                                                    },
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
fun UpdateAllProgressCard(
    progress: UpdateAllProgress,
    onCancel: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text =
                        stringResource(
                            Res.string.updating_x_of_y,
                            progress.current,
                            progress.total,
                        ),
                    style = MaterialTheme.typography.titleMedium,
                )
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.cancel),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text =
                    stringResource(
                        Res.string.currently_updating,
                        progress.currentAppName,
                    ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))

            LinearWavyProgressIndicator(
                progress = { progress.current.toFloat() / progress.total },
                modifier = Modifier.fillMaxWidth(),
            )
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
                        if (app.installSource == zed.rainxch.core.domain.model.InstallSource.DIRECT_URL) {
                            zed.rainxch.apps.presentation.components.DirectUrlBadge()
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = app.repoUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        } else {
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
                        }
                    }

                    when {
                        // Highest priority: a download finished while
                        // the user wasn't watching, the file is on disk
                        // and ready to be installed with one tap.
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
                            // Tap-to-fix label: route through the same OnUpdateApp
                            // intercept that would have opened the picker anyway,
                            // but also surface a tappable hint here for users
                            // who notice the warning before tapping Update.
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
                            // Show the pinned variant tag inline so users can
                            // see at a glance which APK they'll get when they
                            // tap Update.
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
                    // Subtle visual cue when a monorepo filter is active —
                    // the icon tints to primary, so users can tell at a
                    // glance which apps have an active filter without
                    // having to open the sheet.
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

                    // Always-visible "Pick variant" entry point. Tints to
                    // primary when a variant is pinned (so users can see
                    // at a glance whether the app has a sticky pick) and
                    // to error when the pinned variant has gone stale.
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
                        DropdownMenu(
                            expanded = showRowOverflow,
                            onDismissRequest = { showRowOverflow = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    val baseLabel = stringResource(Res.string.apps_ignore_updates)
                                    Text(text = if (!app.updateCheckEnabled) "$baseLabel  ✓" else baseLabel)
                                },
                                onClick = {
                                    showRowOverflow = false
                                    onToggleUpdateCheck(!app.updateCheckEnabled)
                                },
                            )

                            // Skip-this-version is only meaningful when an
                            // update is currently being prompted (we have a
                            // latestVersion to skip). When the row already
                            // has a skipped tag stored, surface the unskip
                            // affordance instead so the user can revert the
                            // suppression without leaving the row.
                            if (app.skippedReleaseTag != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.apps_skip_version_unskip)) },
                                    onClick = {
                                        showRowOverflow = false
                                        onUnskipVersionClick()
                                    },
                                )
                            } else if (app.isUpdateAvailable && !(app.latestVersion ?: app.latestVersionName).isNullOrBlank()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.apps_skip_version)) },
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
                        Button(
                            onClick = onCancelClick,
                            modifier = Modifier.weight(1f),
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = stringResource(Res.string.cancel),
                                modifier = Modifier.size(18.dp),
                            )

                            Spacer(Modifier.width(4.dp))

                            Text(
                                text = stringResource(Res.string.cancel),
                            )
                        }
                    }

                    else -> {
                        if (app.pendingInstallFilePath != null) {
                            // One-tap install for a deferred download.
                            // Bypasses the download phase entirely —
                            // the file is already on disk.
                            Button(
                                onClick = onInstallPendingClick,
                                modifier = Modifier.weight(1f),
                                enabled = !isBusy,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Update,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(Res.string.install),
                                )
                            }
                            // Quick escape hatch: user cancelled the
                            // system prompt and doesn't want this app.
                            // Discard removes the parked file + DB row.
                            IconButton(onClick = onDiscardPendingClick) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = stringResource(Res.string.discard_pending_install),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else if (app.isUpdateAvailable && !app.isPendingInstall) {
                            Button(
                                onClick = onUpdateClick,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Update,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(Res.string.update),
                                )
                            }
                        } else if (app.isPendingInstall) {
                            // Pending row whose parked file is gone
                            // (legacy row from before we persisted the
                            // path, or file deleted out-of-band). The
                            // app isn't actually installed — Open would
                            // snackbar an error. Offer Discard so the
                            // user can clear the row.
                            Button(
                                onClick = onDiscardPendingClick,
                                modifier = Modifier.weight(1f),
                                colors =
                                    ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                                    ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(Res.string.discard_pending_install),
                                )
                            }
                        } else {
                            Button(
                                shapes = ButtonDefaults.shapes(),
                                onClick = onOpenClick,
                                modifier = Modifier.weight(1f),
                                enabled = !isBusy,
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(Res.string.open),
                                )
                            }
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
