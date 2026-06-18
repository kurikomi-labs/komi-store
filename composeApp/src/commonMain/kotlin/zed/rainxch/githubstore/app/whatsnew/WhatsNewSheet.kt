package zed.rainxch.githubstore.app.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.announcement.WhatsNewEntry
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.whats_new_cta_dismiss
import zed.rainxch.githubstore.core.presentation.res.whats_new_cta_history
import zed.rainxch.githubstore.core.presentation.res.whats_new_sheet_heading
import zed.rainxch.githubstore.core.presentation.res.whats_new_version_label
import zed.rainxch.profile.presentation.whatsnew.SectionBlock

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
                    KomiButton(
                        onClick = onDismiss,
                        label = stringResource(Res.string.whats_new_cta_dismiss),
                        variant = KomiButtonVariant.Primary,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (showHistoryAction) {
                        KomiButton(
                            onClick = onViewHistory,
                            label = stringResource(Res.string.whats_new_cta_history),
                            variant = KomiButtonVariant.Text,
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
            text =
                stringResource(
                    Res.string.whats_new_version_label,
                    entry.versionName,
                    entry.releaseDate,
                ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
