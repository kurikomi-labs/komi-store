package zed.rainxch.search.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.close
import zed.rainxch.githubstore.core.presentation.res.sort_by
import zed.rainxch.search.presentation.model.SortByUi
import zed.rainxch.search.presentation.model.SortOrderUi
import zed.rainxch.search.presentation.utils.label

@Composable
fun SortByBottomSheet(
    selectedSortBy: SortByUi,
    selectedSortOrder: SortOrderUi,
    onSortBySelected: (SortByUi) -> Unit,
    onSortOrderSelected: (SortOrderUi) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {},
        dismissButton = {
            GhsButton(
                onClick = onDismissRequest,
                label = stringResource(Res.string.close),
                variant = GhsButtonVariant.Text,
                size = GhsButtonSize.Sm,
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.sort_by),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Column(
                modifier = modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                SortByUi.entries.forEach { option ->
                    val isSelected = option == selectedSortBy
                    GhsButton(
                        onClick = { onSortBySelected(option) },
                        label = stringResource(option.label()) + if (isSelected) "  ✓" else "",
                        variant = GhsButtonVariant.Text,
                        size = GhsButtonSize.Sm,
                        modifier = Modifier.fillMaxWidth(),
                        contentColorOverride = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }

                HorizontalDivider()

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SortOrderUi.entries.forEach { order ->
                        FilterChip(
                            selected = order == selectedSortOrder,
                            onClick = { onSortOrderSelected(order) },
                            label = {
                                Text(
                                    text = stringResource(order.label()),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                        )
                    }
                }
            }
        },
    )
}
