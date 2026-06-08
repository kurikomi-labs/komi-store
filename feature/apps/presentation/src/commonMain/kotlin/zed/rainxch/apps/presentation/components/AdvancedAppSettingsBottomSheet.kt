package zed.rainxch.apps.presentation.components

import zed.rainxch.core.presentation.utils.formatFileSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.core.presentation.components.overlays.GhsBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.AppsAction
import zed.rainxch.apps.presentation.AppsState
import zed.rainxch.apps.presentation.model.GithubAssetUi
import zed.rainxch.githubstore.core.presentation.res.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAppSettingsBottomSheet(
    state: AppsState,
    onAction: (AppsAction) -> Unit,
) {
    val app = state.advancedSettingsApp ?: return

    GhsBottomSheet(
        onDismissRequest = { onAction(AppsAction.OnDismissAdvancedSettings) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.FilterAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.advanced_settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    )

                    Text(
                        text = "${app.repoOwner}/${app.repoName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = stringResource(Res.string.advanced_settings_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            val advancedSupporting = when {
                state.advancedFilterError != null ->
                    stringResource(Res.string.asset_filter_invalid)
                else -> stringResource(Res.string.asset_filter_help)
            }
            GhsTextField(
                value = state.advancedFilterDraft,
                onValueChange = { onAction(AppsAction.OnAdvancedFilterChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(Res.string.asset_filter_label),
                placeholder = stringResource(Res.string.asset_filter_placeholder),
                leadingIcon = Icons.Default.FilterAlt,
                trailingIcon = {
                    if (state.advancedFilterDraft.isNotEmpty()) {
                        GhsButton(
                            onClick = { onAction(AppsAction.OnAdvancedClearFilter) },
                            label = stringResource(Res.string.clear),
                            variant = GhsButtonVariant.Text,
                            size = GhsButtonSize.Sm,
                        )
                    }
                },
                singleLine = true,
                isError = state.advancedFilterError != null,
                supportingText = advancedSupporting,
                enabled = !state.advancedSavingFilter,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.fallback_older_releases_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                    )

                    Text(
                        text = stringResource(Res.string.fallback_older_releases_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Switch(
                    checked = state.advancedFallbackDraft,
                    onCheckedChange = { onAction(AppsAction.OnAdvancedFallbackToggled(it)) },
                    enabled = !state.advancedSavingFilter,
                )
            }

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(Res.string.advanced_filter_variant_relation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            VariantRow(
                pinnedVariant = app.preferredAssetVariant,
                isStale = app.preferredVariantStale,
                onClick = { onAction(AppsAction.OnOpenVariantPicker(app)) },
            )

            Spacer(Modifier.height(16.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )

            Spacer(Modifier.height(16.dp))

            PreviewSection(
                isLoading = state.advancedPreviewLoading,
                matchedAssets = state.advancedPreviewMatched,
                matchedTag = state.advancedPreviewTag,
                message = state.advancedPreviewMessage,
                onRefresh = { onAction(AppsAction.OnAdvancedRefreshPreview) },
            )

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GhsButton(
                    onClick = { onAction(AppsAction.OnDismissAdvancedSettings) },
                    label = stringResource(Res.string.cancel),
                    variant = GhsButtonVariant.Outline,
                    enabled = !state.advancedSavingFilter,
                    modifier = Modifier.weight(1f),
                )

                GhsButton(
                    onClick = { onAction(AppsAction.OnAdvancedSaveFilter) },
                    label = stringResource(Res.string.advanced_save),
                    variant = GhsButtonVariant.Tonal,
                    enabled = !state.advancedSavingFilter && state.advancedFilterError == null,
                    loading = state.advancedSavingFilter,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PreviewSection(
    isLoading: Boolean,
    matchedAssets: ImmutableList<GithubAssetUi>,
    matchedTag: String?,
    message: String?,
    onRefresh: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = stringResource(Res.string.advanced_preview_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )

        IconButton(onClick = onRefresh, enabled = !isLoading) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(Res.string.advanced_preview_refresh),
            )
        }
    }

    Spacer(Modifier.height(4.dp))

    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 2.dp,
                )
            }
        }

        message == "no_match" -> {
            Text(
                text = stringResource(Res.string.advanced_preview_no_match),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        message == "preview_failed" || message == "save_failed" -> {
            Text(
                text = stringResource(Res.string.advanced_preview_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        matchedAssets.isEmpty() -> {
            Text(
                text = stringResource(Res.string.advanced_preview_pending),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        else -> {
            if (matchedTag != null) {
                Text(
                    text =
                        pluralStringResource(
                            Res.plurals.advanced_preview_release,
                            matchedAssets.size,
                            matchedTag,
                            matchedAssets.size,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )

                Spacer(Modifier.height(6.dp))
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 0.dp, max = 180.dp),
            ) {
                items(matchedAssets, key = { it.id }) { asset ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )

                        Spacer(Modifier.width(10.dp))

                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Text(
                            text = formatFileSize(asset.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantRow(
    pinnedVariant: String?,
    isStale: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (isStale) Icons.Default.Warning else Icons.Default.Tune,
            contentDescription = null,
            tint =
                if (isStale) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            modifier = Modifier.size(20.dp),
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.variant_picker_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )

            Text(
                text =
                    when {
                        isStale ->
                            stringResource(Res.string.variant_picker_stale_title)
                        pinnedVariant.isNullOrBlank() ->
                            stringResource(Res.string.variant_picker_auto_subtitle)
                        else ->
                            stringResource(Res.string.variant_picker_pinned, pinnedVariant)
                    },
                style = MaterialTheme.typography.bodySmall,
                color =
                    if (isStale) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

