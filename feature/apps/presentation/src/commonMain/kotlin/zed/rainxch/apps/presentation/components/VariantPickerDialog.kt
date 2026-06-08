package zed.rainxch.apps.presentation.components

import zed.rainxch.core.presentation.utils.formatFileSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.AppsAction
import zed.rainxch.apps.presentation.AppsState
import zed.rainxch.core.domain.utils.AssetVariant
import zed.rainxch.githubstore.core.presentation.res.*

@Composable
fun VariantPickerDialog(
    state: AppsState,
    onAction: (AppsAction) -> Unit,
) {
    val app = state.variantPickerApp ?: return

    AlertDialog(
        onDismissRequest = { onAction(AppsAction.OnDismissVariantPicker) },
        shape = zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape.Dialog,
        title = {
            Column {
                Text(
                    text = stringResource(Res.string.variant_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (app.preferredVariantStale) {
                    StaleVariantBanner(currentVariant = state.variantPickerCurrentVariant)

                    Spacer(Modifier.height(12.dp))
                }

                Text(
                    text = stringResource(Res.string.variant_picker_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(12.dp))

                when {
                    state.variantPickerLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        }
                    }

                    state.variantPickerError == "no_assets" -> {
                        Text(
                            text = stringResource(Res.string.variant_picker_no_assets),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    state.variantPickerError == "no_pinnable_variants" -> {
                        Text(
                            text = stringResource(Res.string.variant_picker_no_pinnable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    state.variantPickerError == "load_failed" -> {
                        Text(
                            text = stringResource(Res.string.variant_picker_load_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    state.variantPickerError == "save_failed" -> {
                        Text(
                            text = stringResource(Res.string.variant_picker_save_failed),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    else -> {
                        VariantOptionList(
                            state = state,
                            onPick = { variant ->
                                onAction(AppsAction.OnVariantSelected(variant))
                            },
                            onResetAuto = { onAction(AppsAction.OnResetVariantToAuto) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            GhsButton(
                onClick = {
                    onAction(AppsAction.OnDismissVariantPicker)
                    onAction(AppsAction.OnOpenAdvancedSettings(app))
                },
                label = stringResource(Res.string.variant_picker_open_filter),
                variant = GhsButtonVariant.Text,
                size = GhsButtonSize.Sm,
            )

            GhsButton(
                onClick = { onAction(AppsAction.OnDismissVariantPicker) },
                label = stringResource(Res.string.cancel),
                variant = GhsButtonVariant.Text,
                size = GhsButtonSize.Sm,
            )
        },
    )
}

@Composable
private fun StaleVariantBanner(
    currentVariant: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer,
                RoundedCornerShape(12.dp),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(20.dp),
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.variant_picker_stale_title),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )

            if (currentVariant != null) {
                Text(
                    text = stringResource(Res.string.variant_picker_stale_was, currentVariant),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun VariantOptionList(
    state: AppsState,
    onPick: (variant: String?) -> Unit,
    onResetAuto: () -> Unit,
) {
    val current = state.variantPickerCurrentVariant
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 0.dp, max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        item {
            VariantRow(
                isSelected = current == null,
                title = stringResource(Res.string.variant_picker_auto_title),
                subtitle = stringResource(Res.string.variant_picker_auto_subtitle),
                leadingIcon = Icons.Default.AutoAwesome,
                onClick = onResetAuto,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
            )
        }

        items(state.variantPickerOptions, key = { it.id }) { asset ->
            val variant = AssetVariant.extract(asset.name)
            if (variant.isNullOrEmpty()) return@items
            val isCurrent = variant.equals(current, ignoreCase = true)
            VariantRow(
                isSelected = isCurrent,
                title = variant,
                subtitle = asset.name + "  ·  " + formatFileSize(asset.size),
                onClick = { onPick(variant) },
            )
        }
    }
}

@Composable
private fun VariantRow(
    isSelected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector =
                when {
                    isSelected -> Icons.Default.RadioButtonChecked
                    else -> Icons.Default.RadioButtonUnchecked
                },
            contentDescription = null,
            tint =
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            modifier = Modifier.size(20.dp),
        )

        Spacer(Modifier.width(12.dp))

        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )

            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

