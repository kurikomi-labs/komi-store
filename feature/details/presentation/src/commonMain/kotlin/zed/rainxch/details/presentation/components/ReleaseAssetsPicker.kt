package zed.rainxch.details.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.GithubAsset
import zed.rainxch.core.domain.model.GithubUser
import zed.rainxch.core.domain.util.AssetVariant
import zed.rainxch.details.presentation.DetailsAction
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.githubstore.core.presentation.res.Res

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun ReleaseAssetsPicker(
    onAction: (DetailsAction) -> Unit,
    assetsList: List<GithubAsset>,
    modifier: Modifier = Modifier,
    selectedAsset: GithubAsset? = null,
    isPickerVisible: Boolean = false,
    pinnedVariant: String? = null,
    showAllPlatforms: Boolean = false,
    crossPlatformAssets: List<GithubAsset> = emptyList(),
) {

    val isPickerEnabled by remember(assetsList, crossPlatformAssets) {
        derivedStateOf {
            assetsList.isNotEmpty() || crossPlatformAssets.isNotEmpty()
        }
    }

    ReleaseAssetsItemsPicker(
        showPicker = isPickerVisible,
        assetsList = assetsList,
        crossPlatformAssets = crossPlatformAssets,
        showAllPlatforms = showAllPlatforms,
        selectedAsset = selectedAsset,
        pinnedVariant = pinnedVariant,
        onDismiss = { onAction(DetailsAction.ToggleReleaseAssetsPicker) },
        onSelect = { onAction(DetailsAction.SelectDownloadAsset(it)) },
        onUnpin = { onAction(DetailsAction.UnpinPreferredVariant) },
        onToggleShowAllPlatforms = { enabled ->
            onAction(DetailsAction.OnToggleShowAllPlatforms(enabled))
        },
        onDownloadForTransfer = { asset ->
            onAction(DetailsAction.OnDownloadForTransfer(asset.downloadUrl))
        },
    )

    Column(
        modifier = modifier.wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.assets_title),
            style = MaterialTheme.typography.labelLargeEmphasized,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        OutlinedCard(
            onClick = { onAction(DetailsAction.ToggleReleaseAssetsPicker) },
            enabled = isPickerEnabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .heightIn(min = 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = selectedAsset?.name ?: stringResource(Res.string.no_assets_selected),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Default.UnfoldMore,
                    contentDescription = stringResource(Res.string.select_version),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ReleaseAssetsItemsPicker(
    assetsList: List<GithubAsset>,
    crossPlatformAssets: List<GithubAsset>,
    showAllPlatforms: Boolean,
    selectedAsset: GithubAsset?,
    pinnedVariant: String?,
    showPicker: Boolean,
    onDismiss: () -> Unit,
    onSelect: (GithubAsset) -> Unit,
    onUnpin: () -> Unit,
    onToggleShowAllPlatforms: (Boolean) -> Unit,
    onDownloadForTransfer: (GithubAsset) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!showPicker) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }

    ReleaseAssetsAboutDialog(
        showDialog = showInfoDialog,
        onDismiss = { showInfoDialog = false },
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.assets_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .weight(1f),
                )
                IconButton(onClick = { showInfoDialog = true }) {
                    Icon(imageVector = Icons.Outlined.Info, contentDescription = stringResource(Res.string.icon_content_description_info))
                }
            }

            if (!pinnedVariant.isNullOrBlank()) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.variant_picker_pinned, pinnedVariant),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.TextButton(onClick = onUnpin) {
                        Text(stringResource(Res.string.variant_picker_unpin))
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier =
                        Modifier
                            .clickable(onClick = { onToggleShowAllPlatforms(!showAllPlatforms) })
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Devices,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = stringResource(Res.string.show_all_platforms_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.Switch(
                        checked = showAllPlatforms,
                        onCheckedChange = onToggleShowAllPlatforms,
                    )
                }
            }

            val groups = remember(crossPlatformAssets) {
                crossPlatformAssets
                    .groupBy {
                        zed.rainxch.core.domain.util.assetPlatformOf(it.name)
                    }
                    .filterKeys { it != null }
                    .mapKeys { it.key!! }
            }
            val installableIds = remember(assetsList) {
                assetsList.map { it.id }.toSet()
            }
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {

                if (showAllPlatforms && groups.isNotEmpty()) {

                    val sectionOrder =
                        listOf(
                            zed.rainxch.core.domain.model.DiscoveryPlatform.Android to Res.string.platform_section_android,
                            zed.rainxch.core.domain.model.DiscoveryPlatform.Windows to Res.string.platform_section_windows,
                            zed.rainxch.core.domain.model.DiscoveryPlatform.Macos to Res.string.platform_section_macos,
                            zed.rainxch.core.domain.model.DiscoveryPlatform.Linux to Res.string.platform_section_linux,
                        ).sortedByDescending { (platform, _) ->
                            groups[platform]?.any { it.id in installableIds } == true
                        }
                    sectionOrder.forEach { (platform, labelRes) ->
                        val assets = groups[platform].orEmpty()
                        if (assets.isEmpty()) return@forEach
                        val isCurrentDevice =
                            assets.any { it.id in installableIds }
                        item(key = "section-${platform.name}") {
                            PlatformSectionCard(
                                platformLabel = stringResource(labelRes),
                                isCurrentDevice = isCurrentDevice,
                                installableIds = installableIds,
                                assets = assets,
                                selectedAsset = selectedAsset,
                                pinnedVariant = pinnedVariant,
                                onAssetClick = { asset ->
                                    if (asset.id in installableIds) {
                                        onSelect(asset)
                                    } else {
                                        onDownloadForTransfer(asset)
                                    }
                                },
                            )
                        }
                    }
                } else if (assetsList.isNotEmpty()) {
                    items(items = assetsList, key = { it.id }) { asset ->
                        val variantTag = AssetVariant.extract(asset.name)
                        val isPinned =
                            !pinnedVariant.isNullOrBlank() &&
                                variantTag?.equals(pinnedVariant, ignoreCase = true) == true
                        ReleaseAssetItem(
                            asset = asset,
                            isSelected = asset.id == selectedAsset?.id,
                            isPinned = isPinned,
                            onClick = { onSelect(asset) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    item {
                        Text(
                            text = stringResource(Res.string.no_assets_in_list),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReleaseAssetsAboutDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    containerColor: Color = AlertDialogDefaults.containerColor,
    shape: Shape = AlertDialogDefaults.shape,
) {
    if (!showDialog) return

    BasicAlertDialog(onDismissRequest = onDismiss, modifier = modifier, properties = properties) {
        Surface(
            color = containerColor,
            contentColor = contentColorFor(containerColor),
            shape = shape,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(Res.string.multiple_assets_info_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlertDialogDefaults.titleContentColor,
                )
                Text(
                    text = stringResource(Res.string.multiple_assets_info_dialog_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AlertDialogDefaults.textContentColor,
                )
            }
        }
    }
}

@Composable
private fun ReleaseAssetItem(
    asset: GithubAsset,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPinned: Boolean = false,
) {
    Row(
        modifier =
            modifier
                .clickable(
                    onClickLabel = stringResource(Res.string.assets_selection_label),
                    onClick = onClick,
                ).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 2,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (isPinned) {
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.variant_picker_pinned_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Text(
                text = formatFileSize(asset.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (isSelected) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
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

@Preview
@Composable
private fun ReleaseAssetsPickerItemPreview() {
    ReleaseAssetItem(
        asset =
            GithubAsset(
                id = -1,
                name = "Random",
                contentType = "",
                size = 20 * 1024,
                downloadUrl = "",
                uploader = GithubUser(id = -1, login = "", avatarUrl = "", htmlUrl = ""),
            ),
        onClick = {},
        isSelected = false,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlatformSectionCard(
    platformLabel: String,
    isCurrentDevice: Boolean,
    installableIds: Set<Long>,
    assets: List<GithubAsset>,
    selectedAsset: GithubAsset?,
    pinnedVariant: String?,
    onAssetClick: (GithubAsset) -> Unit,
) {
    OutlinedCard(
        colors =
            CardDefaults.outlinedCardColors(
                containerColor =
                    if (isCurrentDevice) {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    },
            ),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = platformLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            SectionChip(
                label =
                    if (isCurrentDevice) {
                        stringResource(Res.string.section_chip_your_device)
                    } else {
                        stringResource(Res.string.section_chip_for_transfer)
                    },
                isPrimary = isCurrentDevice,
            )
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )

        assets.forEachIndexed { index, asset ->
            val isInstallableHere = asset.id in installableIds
            val variantTag = AssetVariant.extract(asset.name)
            val isPinned =
                isInstallableHere &&
                    !pinnedVariant.isNullOrBlank() &&
                    variantTag?.equals(pinnedVariant, ignoreCase = true) == true
            ReleaseAssetItem(
                asset = asset,
                isSelected = isInstallableHere && asset.id == selectedAsset?.id,
                isPinned = isPinned,
                onClick = { onAssetClick(asset) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (index < assets.lastIndex) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionChip(
    label: String,
    isPrimary: Boolean,
) {
    val container =
        if (isPrimary) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.tertiaryContainer
        }
    val content =
        if (isPrimary) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onTertiaryContainer
        }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = container,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}
