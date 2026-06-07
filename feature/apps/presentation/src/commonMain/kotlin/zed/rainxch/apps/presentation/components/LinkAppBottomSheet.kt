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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.core.presentation.components.overlays.GhsBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.AppsAction
import zed.rainxch.apps.presentation.AppsState
import zed.rainxch.apps.presentation.model.DeviceAppUi
import zed.rainxch.apps.presentation.model.LinkStep
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
    GhsBottomSheet(
        onDismissRequest = { onAction(AppsAction.OnDismissLinkSheet) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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

        GhsTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = stringResource(Res.string.search_apps_hint),
            leadingIcon = Icons.Default.Search,
            singleLine = true,
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
    suggestions: ImmutableList<RepoMatchSuggestion>,
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

                GhsButton(
                    onClick = onRetry,
                    label = stringResource(Res.string.retry),
                    variant = GhsButtonVariant.Tonal,
                    modifier = Modifier.fillMaxWidth(),
                )
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

        GhsButton(
            onClick = onEnterUrlManually,
            label = stringResource(Res.string.link_smart_search_enter_manually),
            variant = GhsButtonVariant.Tonal,
            enabled = !isValidating,
            modifier = Modifier.fillMaxWidth(),
        )
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

        RepoMatchSource.FORGEJO_SEARCH -> stringResource(Res.string.match_source_search)
        RepoMatchSource.STARRED -> stringResource(Res.string.match_source_starred)
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

        GhsTextField(
            value = repoUrl,
            onValueChange = onUrlChanged,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.enter_repo_url),
            placeholder = stringResource(Res.string.repo_url_hint),
            singleLine = true,
            isError = validationError != null,
            supportingText = validationError,
        )

        Spacer(Modifier.height(20.dp))

        GhsButton(
            onClick = onConfirm,
            label = if (isValidating) {
                stringResource(Res.string.validating_repo)
            } else {
                stringResource(Res.string.link_and_track)
            },
            variant = GhsButtonVariant.Tonal,
            enabled = repoUrl.isNotBlank() && !isValidating,
            loading = isValidating,
            modifier = Modifier.fillMaxWidth(),
        )

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

        val filterSupporting = when {
            filterError != null -> stringResource(Res.string.asset_filter_invalid)
            visibleAssets.isEmpty() && filterValue.isNotBlank() ->
                stringResource(Res.string.asset_filter_no_match)
            filterValue.isNotBlank() ->
                pluralStringResource(
                    Res.plurals.asset_filter_visible_count,
                    allAssets.size,
                    visibleAssets.size,
                    allAssets.size,
                )
            else -> stringResource(Res.string.asset_filter_help)
        }
        GhsTextField(
            value = filterValue,
            onValueChange = onFilterChanged,
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.asset_filter_label),
            placeholder = stringResource(Res.string.asset_filter_placeholder),
            leadingIcon = Icons.Default.FilterAlt,
            singleLine = true,
            isError = filterError != null,
            supportingText = filterSupporting,
            enabled = !isProcessing,
        )

        Spacer(Modifier.height(8.dp))

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
