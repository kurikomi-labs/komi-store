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
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.dividers.KomiHorizontalDivider
import zed.rainxch.core.presentation.components.icon.KomiIcon
import zed.rainxch.core.presentation.components.overlays.KomiDialog
import zed.rainxch.core.presentation.components.progress.KomiCircularProgress
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val colors = LocalPersonality.current.colors

    KomiDialog(
        onDismissRequest = { onAction(AppsAction.OnDismissVariantPicker) },
        title = {
            Column {
                KomiText(
                    text = stringResource(Res.string.variant_picker_title),
                    role = KomiTextRole.Title,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(2.dp))

                KomiText(
                    text = app.appName,
                    role = KomiTextRole.Body,
                    fontSize = 13.sp,
                    uppercase = false,
                    color = colors.onSurfaceVariant,
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

                KomiText(
                    text = stringResource(Res.string.variant_picker_description),
                    role = KomiTextRole.Body,
                    color = colors.onSurfaceVariant,
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
                            KomiCircularProgress()
                        }
                    }

                    state.variantPickerError == "no_assets" -> {
                        KomiText(
                            text = stringResource(Res.string.variant_picker_no_assets),
                            role = KomiTextRole.Body,
                            color = colors.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    state.variantPickerError == "no_pinnable_variants" -> {
                        KomiText(
                            text = stringResource(Res.string.variant_picker_no_pinnable),
                            role = KomiTextRole.Body,
                            color = colors.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    state.variantPickerError == "load_failed" -> {
                        KomiText(
                            text = stringResource(Res.string.variant_picker_load_failed),
                            role = KomiTextRole.Body,
                            color = colors.error,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }

                    state.variantPickerError == "save_failed" -> {
                        KomiText(
                            text = stringResource(Res.string.variant_picker_save_failed),
                            role = KomiTextRole.Body,
                            color = colors.error,
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
        dismissButton = {
            KomiButton(
                onClick = {
                    onAction(AppsAction.OnDismissVariantPicker)
                    onAction(AppsAction.OnOpenAdvancedSettings(app))
                },
                label = stringResource(Res.string.variant_picker_open_filter),
                variant = KomiButtonVariant.Text,
                size = KomiButtonSize.Sm,
            )
        },
        confirmButton = {
            KomiButton(
                onClick = { onAction(AppsAction.OnDismissVariantPicker) },
                label = stringResource(Res.string.cancel),
                variant = KomiButtonVariant.Text,
                size = KomiButtonSize.Sm,
            )
        },
    )
}

@Composable
private fun StaleVariantBanner(
    currentVariant: String?,
) {
    val colors = LocalPersonality.current.colors
    val shape = LocalPersonality.current.shape
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                colors.error,
                RoundedCornerShape(shape.corner),
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KomiIcon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = colors.onError,
            modifier = Modifier.size(20.dp),
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            KomiText(
                text = stringResource(Res.string.variant_picker_stale_title),
                role = KomiTextRole.Body,
                fontWeight = FontWeight.SemiBold,
                color = colors.onError,
            )

            if (currentVariant != null) {
                KomiText(
                    text = stringResource(Res.string.variant_picker_stale_was, currentVariant),
                    role = KomiTextRole.Body,
                    fontSize = 13.sp,
                    uppercase = false,
                    color = colors.onError,
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
    val colors = LocalPersonality.current.colors
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

            KomiHorizontalDivider(
                color = colors.outlineVariant.copy(alpha = 0.4f),
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
    val colors = LocalPersonality.current.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KomiIcon(
            imageVector =
                when {
                    isSelected -> Icons.Default.RadioButtonChecked
                    else -> Icons.Default.RadioButtonUnchecked
                },
            contentDescription = null,
            tint =
                if (isSelected) {
                    colors.primary
                } else {
                    colors.onSurfaceVariant
                },
            modifier = Modifier.size(20.dp),
        )

        Spacer(Modifier.width(12.dp))

        if (leadingIcon != null) {
            KomiIcon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )

            Spacer(Modifier.width(8.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            KomiText(
                text = title,
                role = KomiTextRole.Body,
                color = colors.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                uppercase = false,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            KomiText(
                text = subtitle,
                role = KomiTextRole.Body,
                fontSize = 13.sp,
                uppercase = false,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (isSelected) {
            KomiIcon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

