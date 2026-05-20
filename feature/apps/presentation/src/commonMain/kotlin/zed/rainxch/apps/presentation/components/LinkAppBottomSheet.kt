package zed.rainxch.apps.presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.AppsAction
import zed.rainxch.apps.presentation.AppsState
import zed.rainxch.apps.presentation.LinkStep
import zed.rainxch.apps.presentation.model.DeviceAppUi
import zed.rainxch.apps.presentation.model.GithubAssetUi
import zed.rainxch.core.domain.model.InstallerCategory
import zed.rainxch.core.domain.system.RepoMatchSource
import zed.rainxch.core.domain.system.RepoMatchSuggestion
import zed.rainxch.githubstore.core.presentation.res.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkAppBottomSheet(
    state: AppsState,
    onAction: (AppsAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = { onAction(AppsAction.OnDismissLinkSheet) },
        sheetState = sheetState,
    ) {
        AnimatedContent(
            targetState = state.linkStep,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "link_step",
        ) { step ->
            when (step) {
                LinkStep.PickApp -> PickAppStep(
                    deviceApps = state.filteredDeviceApps,
                    searchQuery = state.deviceAppSearchQuery,
                    onSearchChange = { onAction(AppsAction.OnDeviceAppSearchChange(it)) },
                    onAppSelected = { onAction(AppsAction.OnDeviceAppSelected(it)) },
                )

                LinkStep.SmartMatch -> SmartMatchStep(
                    selectedApp = state.selectedDeviceApp,
                    loading = state.linkSearchLoading,
                    suggestions = state.linkSuggestions,
                    error = state.linkSearchError,
                    isValidating = state.isValidatingRepo,
                    validationStatus = state.linkValidationStatus,
                    onSuggestionSelected = { owner, repo, sourceHost ->
                        onAction(AppsAction.OnLinkSuggestionSelected(owner, repo, sourceHost))
                    },
                    onEnterUrlManually = { onAction(AppsAction.OnLinkEnterUrlManually) },
                    onRetry = { onAction(AppsAction.OnRetryLinkSearch) },
                    onBack = { onAction(AppsAction.OnBackToAppPicker) },
                )

                LinkStep.EnterUrl -> EnterUrlStep(
                    selectedApp = state.selectedDeviceApp,
                    repoUrl = state.repoUrl,
                    isValidating = state.isValidatingRepo,
                    validationError = state.repoValidationError,
                    validationStatus = state.linkValidationStatus,
                    onUrlChanged = { onAction(AppsAction.OnRepoUrlChanged(it)) },
                    onConfirm = { onAction(AppsAction.OnValidateAndLinkRepo) },
                    onBack = { onAction(AppsAction.OnBackToSmartMatch) },
                )

                LinkStep.PickAsset -> PickAssetStep(
                    allAssets = state.linkInstallableAssets,
                    visibleAssets = state.filteredLinkAssets,
                    selectedAsset = state.linkSelectedAsset,
                    downloadProgress = state.linkDownloadProgress,
                    validationStatus = state.linkValidationStatus,
                    validationError = state.repoValidationError,
                    filterValue = state.linkAssetFilter,
                    filterError = state.linkAssetFilterError,
                    fallbackEnabled = state.linkFallbackToOlder,
                    onFilterChanged = { onAction(AppsAction.OnLinkAssetFilterChanged(it)) },
                    onFallbackToggled = { onAction(AppsAction.OnLinkFallbackToggled(it)) },
                    onAssetSelected = { onAction(AppsAction.OnLinkAssetSelected(it)) },
                    onBack = { onAction(AppsAction.OnBackToEnterUrl) },
                )
            }
        }
    }
}

@Composable
private fun PickAppStep(
    deviceApps: List<DeviceAppUi>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onAppSelected: (DeviceAppUi) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = stringResource(Res.string.link_app_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.pick_installed_app),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        TextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            placeholder = {
                Text(stringResource(Res.string.search_apps_hint))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
        ) {
            items(
                items = deviceApps,
                key = { it.packageName },
            ) { app ->
                DeviceAppItem(
                    app = app,
                    onClick = { onAppSelected(app) },
                )
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }

            if (deviceApps.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.no_apps_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DeviceAppItem(
    app: DeviceAppUi,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                InstallerCategoryChip(app.installerCategory)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        app.versionName?.let { version ->
            Text(
                text = version,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 96.dp),
            )
        }
    }
}

@Composable
private fun InstallerCategoryChip(category: InstallerCategory) {
    val label = when (category) {
        InstallerCategory.SIDE_STORE -> stringResource(Res.string.installer_category_side_store)
        InstallerCategory.SIDELOADED -> stringResource(Res.string.installer_category_sideloaded)
        InstallerCategory.VENDOR_STORE -> stringResource(Res.string.installer_category_vendor_store)
        InstallerCategory.PLAY_STORE -> stringResource(Res.string.installer_category_play_store)
        InstallerCategory.SYSTEM_UPDATE -> stringResource(Res.string.installer_category_system_update)
    }
    val container = when (category) {
        InstallerCategory.SIDE_STORE -> MaterialTheme.colorScheme.primaryContainer
        InstallerCategory.SIDELOADED -> MaterialTheme.colorScheme.secondaryContainer
        InstallerCategory.VENDOR_STORE -> MaterialTheme.colorScheme.tertiaryContainer
        InstallerCategory.PLAY_STORE -> MaterialTheme.colorScheme.surfaceVariant
        InstallerCategory.SYSTEM_UPDATE -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (category) {
        InstallerCategory.SIDE_STORE -> MaterialTheme.colorScheme.onPrimaryContainer
        InstallerCategory.SIDELOADED -> MaterialTheme.colorScheme.onSecondaryContainer
        InstallerCategory.VENDOR_STORE -> MaterialTheme.colorScheme.onTertiaryContainer
        InstallerCategory.PLAY_STORE -> MaterialTheme.colorScheme.onSurfaceVariant
        InstallerCategory.SYSTEM_UPDATE -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = container,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SmartMatchStep(
    selectedApp: DeviceAppUi?,
    loading: Boolean,
    suggestions: List<RepoMatchSuggestion>,
    error: String?,
    isValidating: Boolean,
    validationStatus: String?,
    onSuggestionSelected: (owner: String, repo: String, sourceHost: String?) -> Unit,
    onEnterUrlManually: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = !isValidating) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }
            Text(
                text = stringResource(Res.string.link_smart_search_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        if (selectedApp != null) {
            Text(
                text = selectedApp.appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = selectedApp.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))

        when {
            loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(Res.string.link_smart_search_searching),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            isValidating -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = validationStatus ?: stringResource(Res.string.validating_repo),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            error != null -> {
                Text(
                    text = stringResource(Res.string.link_smart_search_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(stringResource(Res.string.retry))
                }
            }

            suggestions.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.link_smart_search_no_matches),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp),
                ) {
                    items(
                        items = suggestions,
                        // Lists may now mix GitHub + Forgejo hits for
                        // the same owner/repo slug across hosts —
                        // include sourceHost in the key so identical
                        // slugs on different forges render as distinct
                        // rows.
                        key = { "${it.sourceHost ?: "github"}|${it.owner}/${it.repo}" },
                    ) { suggestion ->
                        SuggestionRow(
                            suggestion = suggestion,
                            onClick = {
                                onSuggestionSelected(
                                    suggestion.owner,
                                    suggestion.repo,
                                    suggestion.sourceHost,
                                )
                            },
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        FilledTonalButton(
            onClick = onEnterUrlManually,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isValidating,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(stringResource(Res.string.link_smart_search_enter_manually))
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: RepoMatchSuggestion,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${suggestion.owner}/${suggestion.repo}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            val description = suggestion.description?.takeIf { it.isNotBlank() }
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Host badge — answers "where is this suggestion from".
                HostBadge(suggestion.sourceHost)
                Spacer(Modifier.width(6.dp))
                MatchSourceChip(suggestion.source)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "${(suggestion.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                suggestion.stars?.let { stars ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "★ $stars",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun HostBadge(sourceHost: String?) {
    val (label, bg, fg) = when {
        sourceHost == null ->
            Triple(
                "GitHub",
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.onSurfaceVariant,
            )
        sourceHost.equals("codeberg.org", ignoreCase = true) ->
            Triple(
                "Codeberg",
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
            )
        else ->
            Triple(
                sourceHost,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
            )
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun MatchSourceChip(source: RepoMatchSource) {
    val label = when (source) {
        RepoMatchSource.MANIFEST -> stringResource(Res.string.match_source_manifest)
        RepoMatchSource.FINGERPRINT -> stringResource(Res.string.match_source_fingerprint)
        RepoMatchSource.SEARCH -> stringResource(Res.string.match_source_search)
        RepoMatchSource.MANUAL -> stringResource(Res.string.match_source_manual)
        // Reuses the "search" string for now — distinguishing the
        // forge in the chip would need a new translated label per
        // locale, which can come later. The underlying source-host
        // is what actually drives URL building and the row's source
        // chip on the Details / Apps screen.
        RepoMatchSource.FORGEJO_SEARCH -> stringResource(Res.string.match_source_search)
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EnterUrlStep(
    selectedApp: DeviceAppUi?,
    repoUrl: String,
    isValidating: Boolean,
    validationError: String?,
    validationStatus: String?,
    onUrlChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }

            Text(
                text = stringResource(Res.string.link_app_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Selected app info
        if (selectedApp != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = selectedApp.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = selectedApp.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                selectedApp.versionName?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = repoUrl,
            onValueChange = onUrlChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.enter_repo_url)) },
            placeholder = { Text(stringResource(Res.string.repo_url_hint)) },
            singleLine = true,
            isError = validationError != null,
            supportingText = validationError?.let {
                { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(20.dp))

        FilledTonalButton(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = repoUrl.isNotBlank() && !isValidating,
            shape = RoundedCornerShape(12.dp),
        ) {
            if (isValidating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(Res.string.validating_repo))
            } else {
                Text(
                    text = stringResource(Res.string.link_and_track),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (isValidating && validationStatus != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = validationStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PickAssetStep(
    allAssets: List<GithubAssetUi>,
    visibleAssets: List<GithubAssetUi>,
    selectedAsset: GithubAssetUi?,
    downloadProgress: Int?,
    validationStatus: String?,
    validationError: String?,
    filterValue: String,
    filterError: String?,
    fallbackEnabled: Boolean,
    onFilterChanged: (String) -> Unit,
    onFallbackToggled: (Boolean) -> Unit,
    onAssetSelected: (GithubAssetUi) -> Unit,
    onBack: () -> Unit,
) {
    val isProcessing = selectedAsset != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, enabled = !isProcessing) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                )
            }

            Text(
                text = stringResource(Res.string.select_asset_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = stringResource(Res.string.select_asset_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )

        Spacer(Modifier.height(12.dp))

        // Asset filter — for monorepos that ship multiple apps from the
        // same repo. Live-narrows the visible list and is persisted with
        // the link, so the update checker only ever resolves matching APKs.
        OutlinedTextField(
            value = filterValue,
            onValueChange = onFilterChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(Res.string.asset_filter_label)) },
            placeholder = { Text(stringResource(Res.string.asset_filter_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = null,
                )
            },
            singleLine = true,
            isError = filterError != null,
            supportingText = {
                Text(
                    text =
                        when {
                            filterError != null -> stringResource(Res.string.asset_filter_invalid)
                            visibleAssets.isEmpty() && filterValue.isNotBlank() ->
                                stringResource(Res.string.asset_filter_no_match)
                            filterValue.isNotBlank() ->
                                // Pass the total asset count as the plural
                                // quantity so Polish/Russian inflection picks
                                // the right form based on the *collection*
                                // size, and supply both counts as format args.
                                pluralStringResource(
                                    Res.plurals.asset_filter_visible_count,
                                    allAssets.size,
                                    visibleAssets.size,
                                    allAssets.size,
                                )
                            else -> stringResource(Res.string.asset_filter_help)
                        },
                    color =
                        if (filterError != null) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                )
            },
            enabled = !isProcessing,
            shape = RoundedCornerShape(12.dp),
        )

        Spacer(Modifier.height(8.dp))

        // Fall-back-to-older-releases toggle. Only meaningful when a filter
        // is set; in monorepos, the latest release is often for the wrong
        // app, so the checker needs to walk back to find this app's APK.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isProcessing) { onFallbackToggled(!fallbackEnabled) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.fallback_older_releases_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(Res.string.fallback_older_releases_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = fallbackEnabled,
                onCheckedChange = onFallbackToggled,
                enabled = !isProcessing,
            )
        }

        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
        ) {
            items(
                items = visibleAssets,
                key = { it.id },
            ) { asset ->
                val isSelected = selectedAsset?.id == asset.id

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isSelected) {
                                Modifier.background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp),
                                )
                            } else {
                                Modifier
                            },
                        )
                        .clickable(enabled = !isProcessing) { onAssetSelected(asset) }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = formatFileSize(asset.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (isSelected && downloadProgress != null) {
                        Spacer(Modifier.width(8.dp))
                        CircularProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$downloadProgress%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                )
            }

            if (visibleAssets.isEmpty()) {
                item {
                    // Three distinct empty states:
                    //  - No installable assets at all in the repo release
                    //    (defensive: validateAndLinkRepo short-circuits
                    //    this today, but guard in case flows change)
                    //  - Filter regex is invalid (shown in error color)
                    //  - Filter is valid but matched nothing
                    val (message, isError) = when {
                        allAssets.isEmpty() ->
                            stringResource(Res.string.asset_none_available) to false
                        filterError != null ->
                            stringResource(Res.string.asset_filter_invalid) to true
                        else ->
                            stringResource(Res.string.asset_filter_no_match) to false
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            color =
                                if (isError) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                        )
                    }
                }
            }
        }

        if (validationStatus != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = validationStatus,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (validationError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = validationError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String =
    when {
        bytes >= 1_073_741_824 -> "%.1f GB".format(bytes / 1_073_741_824.0)
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes B"
    }
