package zed.rainxch.core.presentation.components.whatsnew

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.WhatsNewEntry
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.domain.model.WhatsNewSection
import zed.rainxch.core.domain.model.WhatsNewSectionType
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.whats_new_cta_dismiss
import zed.rainxch.githubstore.core.presentation.res.whats_new_cta_history
import zed.rainxch.githubstore.core.presentation.res.whats_new_section_fixed
import zed.rainxch.githubstore.core.presentation.res.whats_new_section_heads_up
import zed.rainxch.githubstore.core.presentation.res.whats_new_section_improved
import zed.rainxch.githubstore.core.presentation.res.whats_new_section_new
import zed.rainxch.githubstore.core.presentation.res.whats_new_sheet_heading
import zed.rainxch.githubstore.core.presentation.res.whats_new_version_label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    entry: WhatsNewEntry,
    showHistoryAction: Boolean,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { SheetHeader(entry) }

            items(entry.sections) { section ->
                SectionBlock(section)
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GhsButton(
                        onClick = onDismiss,
                        label = stringResource(Res.string.whats_new_cta_dismiss),
                        variant = GhsButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (showHistoryAction) {
                        GhsButton(
                            onClick = onViewHistory,
                            label = stringResource(Res.string.whats_new_cta_history),
                            variant = GhsButtonVariant.Text,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SheetHeader(entry: WhatsNewEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(Res.string.whats_new_sheet_heading, entry.versionName),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                Res.string.whats_new_version_label,
                entry.versionName,
                entry.releaseDate,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun WhatsNewEntryCard(entry: WhatsNewEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = entry.versionName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = entry.releaseDate,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            entry.sections.forEach { section ->
                SectionBlock(section)
            }
        }
    }
}

@Composable
private fun SectionBlock(section: WhatsNewSection) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(type = section.type)

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                section.bullets.forEach { bullet ->
                    BulletRow(text = bullet)
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(type: WhatsNewSectionType) {
    val (label, color) = when (type) {
        WhatsNewSectionType.NEW -> stringResource(Res.string.whats_new_section_new) to
            MaterialTheme.colorScheme.primary
        WhatsNewSectionType.IMPROVED -> stringResource(Res.string.whats_new_section_improved) to
            MaterialTheme.colorScheme.tertiary
        WhatsNewSectionType.FIXED -> stringResource(Res.string.whats_new_section_fixed) to
            MaterialTheme.colorScheme.secondary
        WhatsNewSectionType.HEADS_UP -> stringResource(Res.string.whats_new_section_heads_up) to
            MaterialTheme.colorScheme.error
    }

    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
    )
}

@Composable
private fun BulletRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
