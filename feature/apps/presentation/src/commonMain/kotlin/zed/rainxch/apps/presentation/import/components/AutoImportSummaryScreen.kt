package zed.rainxch.apps.presentation.import.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.external_import_auto_summary_body
import zed.rainxch.githubstore.core.presentation.res.external_import_auto_summary_continue
import zed.rainxch.githubstore.core.presentation.res.external_import_auto_summary_headline
import zed.rainxch.githubstore.core.presentation.res.external_import_auto_summary_more_count
import zed.rainxch.githubstore.core.presentation.res.external_import_auto_summary_review_hint
import zed.rainxch.githubstore.core.presentation.res.external_import_auto_summary_undo_all

@Composable
fun AutoImportSummaryScreen(
    autoLinkedCount: Int,
    autoLinkedLabels: ImmutableList<String>,
    cardsRemaining: Int,
    onContinue: () -> Unit,
    onUndoAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.widthIn(max = 480.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )

            Text(
                text = pluralStringResource(
                    Res.plurals.external_import_auto_summary_headline,
                    autoLinkedCount,
                    autoLinkedCount,
                ),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Text(
                text = stringResource(Res.string.external_import_auto_summary_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (cardsRemaining > 0) {
                Text(
                    text = pluralStringResource(
                        Res.plurals.external_import_auto_summary_review_hint,
                        cardsRemaining,
                        cardsRemaining,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }

            if (autoLinkedLabels.isNotEmpty()) {
                AutoLinkedChipRow(autoLinkedLabels = autoLinkedLabels)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onUndoAll) {
                    Text(stringResource(Res.string.external_import_auto_summary_undo_all))
                }
                Button(onClick = onContinue) {
                    Text(stringResource(Res.string.external_import_auto_summary_continue))
                }
            }
        }
    }
}

@Composable
private fun AutoLinkedChipRow(autoLinkedLabels: ImmutableList<String>) {
    val visible = autoLinkedLabels.take(MAX_VISIBLE_CHIPS)
    val overflow = autoLinkedLabels.size - visible.size

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        visible.forEach { label ->
            ChipSurface(text = label)
        }
        if (overflow > 0) {
            ChipSurface(
                text = stringResource(
                    Res.string.external_import_auto_summary_more_count,
                    overflow,
                ),
            )
        }
    }
}

@Composable
private fun ChipSurface(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

private const val MAX_VISIBLE_CHIPS = 5
