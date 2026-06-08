package zed.rainxch.apps.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.components.overlays.GhsBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.domain.model.ImportFormat
import zed.rainxch.apps.domain.model.ImportResult
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.import_summary_already_tracked
import zed.rainxch.githubstore.core.presentation.res.import_summary_close
import zed.rainxch.githubstore.core.presentation.res.import_summary_collapse
import zed.rainxch.githubstore.core.presentation.res.import_summary_expand
import zed.rainxch.githubstore.core.presentation.res.import_summary_failed
import zed.rainxch.githubstore.core.presentation.res.import_summary_format_native
import zed.rainxch.githubstore.core.presentation.res.import_summary_format_obtainium
import zed.rainxch.githubstore.core.presentation.res.import_summary_imported
import zed.rainxch.githubstore.core.presentation.res.import_summary_non_github
import zed.rainxch.githubstore.core.presentation.res.import_summary_non_github_caption
import zed.rainxch.githubstore.core.presentation.res.import_summary_title
import zed.rainxch.githubstore.core.presentation.res.import_summary_unknown_caption
import zed.rainxch.githubstore.core.presentation.res.import_summary_unknown_format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSummarySheet(
    summary: ImportResult,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (summary.sourceFormat == ImportFormat.UNKNOWN) {
        UnknownFormatSheet(
            preview = summary.unknownFormatPreview,
            sheetState = sheetState,
            onDismiss = onDismiss,
        )
        return
    }

    GhsBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val formatLabel = when (summary.sourceFormat) {
                ImportFormat.OBTAINIUM -> stringResource(Res.string.import_summary_format_obtainium)
                else -> stringResource(Res.string.import_summary_format_native)
            }
            Text(
                text = stringResource(Res.string.import_summary_title, formatLabel),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )

            val importedPart = stringResource(Res.string.import_summary_imported, summary.imported)
            val skippedPart = stringResource(Res.string.import_summary_already_tracked, summary.skipped)
            val nonGitHubPart = if (summary.nonGitHubSkipped > 0) {
                stringResource(Res.string.import_summary_non_github, summary.nonGitHubSkipped)
            } else {
                null
            }
            val failedPart = stringResource(Res.string.import_summary_failed, summary.failed)
            val announcement = listOfNotNull(importedPart, skippedPart, nonGitHubPart, failedPart)
                .joinToString(", ")
            Text(
                text = "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .height(0.dp)
                    .semantics {
                        contentDescription = announcement
                        liveRegion = LiveRegionMode.Polite
                    },
            )

            SummaryBucket(
                icon = Icons.Outlined.CheckCircle,
                tint = MaterialTheme.colorScheme.primary,
                title = stringResource(Res.string.import_summary_imported, summary.imported),
                items = summary.importedItems,
            )

            if (summary.skipped > 0) {
                SummaryBucket(
                    icon = Icons.Outlined.RemoveCircleOutline,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(Res.string.import_summary_already_tracked, summary.skipped),
                    items = summary.skippedItems,
                )
            }

            if (summary.nonGitHubSkipped > 0) {
                SummaryBucket(
                    icon = Icons.Outlined.WarningAmber,
                    tint = MaterialTheme.colorScheme.tertiary,
                    title = stringResource(Res.string.import_summary_non_github, summary.nonGitHubSkipped),
                    caption = stringResource(Res.string.import_summary_non_github_caption),
                    items = summary.nonGitHubItems,
                )
            }

            if (summary.failed > 0) {
                SummaryBucket(
                    icon = Icons.Outlined.ErrorOutline,
                    tint = MaterialTheme.colorScheme.error,
                    title = stringResource(Res.string.import_summary_failed, summary.failed),
                    items = summary.failedItems,
                )
            }

            GhsButton(
                onClick = onDismiss,
                label = stringResource(Res.string.import_summary_close),
                variant = GhsButtonVariant.Primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnknownFormatSheet(
    preview: String?,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
) {
    GhsBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(Res.string.import_summary_unknown_format),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                text = stringResource(Res.string.import_summary_unknown_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!preview.isNullOrBlank()) {
                SelectionContainer {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                    )
                }
            }

            GhsButton(
                onClick = onDismiss,
                label = stringResource(Res.string.import_summary_close),
                variant = GhsButtonVariant.Primary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SummaryBucket(
    icon: ImageVector,
    tint: Color,
    title: String,
    caption: String? = null,
    items: ImmutableList<String>,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint)

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )

                if (!caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (items.isNotEmpty()) {
                val expandLabel = stringResource(
                    if (expanded) Res.string.import_summary_collapse else Res.string.import_summary_expand,
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = expandLabel,
                    )
                }
            }
        }

        if (expanded && items.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(start = 32.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(items.size) { idx ->
                    Text(
                        text = items[idx],
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

