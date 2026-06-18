@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package zed.rainxch.search.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.overlays.KomiSheet
import zed.rainxch.core.presentation.components.overlays.KomiSheetPlacement
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.search_filters_apply
import zed.rainxch.githubstore.core.presentation.res.search_filters_reset
import zed.rainxch.githubstore.core.presentation.res.search_filters_section_language
import zed.rainxch.githubstore.core.presentation.res.search_filters_section_platform
import zed.rainxch.githubstore.core.presentation.res.search_filters_section_sort
import zed.rainxch.githubstore.core.presentation.res.search_filters_section_source
import zed.rainxch.githubstore.core.presentation.res.search_filters_title
import zed.rainxch.search.presentation.model.ProgrammingLanguageUi
import zed.rainxch.search.presentation.model.SearchPlatformUi
import zed.rainxch.search.presentation.model.SearchSourceUi
import zed.rainxch.search.presentation.model.SortByUi
import zed.rainxch.search.presentation.utils.label

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchFiltersSheet(
    selectedSource: SearchSourceUi,
    availableSources: ImmutableList<SearchSourceUi>,
    selectedPlatform: SearchPlatformUi,
    selectedLanguage: ProgrammingLanguageUi,
    selectedSortBy: SortByUi,
    onSourceSelected: (SearchSourceUi) -> Unit,
    onPlatformSelected: (SearchPlatformUi) -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onOpenSortPicker: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    KomiSheet(
        onDismiss = onDismiss,
        placement = KomiSheetPlacement.Bottom,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.search_filters_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                KomiButton(
                    onClick = onReset,
                    label = stringResource(Res.string.search_filters_reset),
                    variant = KomiButtonVariant.Text,
                    size = KomiButtonSize.Sm,
                )
            }

            FilterSection(title = stringResource(Res.string.search_filters_section_source)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    availableSources.forEach { source ->
                        SelectableChip(
                            text = source.label,
                            selected = selectedSource == source,
                            onClick = { onSourceSelected(source) },
                        )
                    }
                }
            }

            FilterSection(title = stringResource(Res.string.search_filters_section_platform)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SearchPlatformUi.entries.forEach { platform ->
                        SelectableChip(
                            text = platform.name
                                .lowercase()
                                .replaceFirstChar { it.uppercase() },
                            selected = selectedPlatform == platform,
                            onClick = { onPlatformSelected(platform) },
                        )
                    }
                }
            }

            FilterSection(title = stringResource(Res.string.search_filters_section_language)) {
                NavRow(
                    leadingIcon = Icons.Outlined.Language,
                    value = stringResource(selectedLanguage.label()),
                    onClick = onOpenLanguagePicker,
                )
            }

            FilterSection(title = stringResource(Res.string.search_filters_section_sort)) {
                NavRow(
                    leadingIcon = Icons.AutoMirrored.Filled.Sort,
                    value = stringResource(selectedSortBy.label()),
                    onClick = onOpenSortPicker,
                )
            }

            Spacer(Modifier.height(4.dp))

            KomiButton(
                onClick = onDismiss,
                label = stringResource(Res.string.search_filters_apply),
                variant = KomiButtonVariant.Primary,
                size = KomiButtonSize.Lg,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val border = if (selected) {
        BorderStroke(0.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(border, RoundedCornerShape(50))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = content,
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                ),
                color = content,
            )
        }
    }
}

@Composable
private fun NavRow(
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
