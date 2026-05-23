package zed.rainxch.tweaks.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState

@Composable
fun CustomForgesDialog(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(TweaksAction.OnDismissCustomForgesDialog) },
        shape = zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape.Dialog,
        title = { Text(stringResource(Res.string.custom_forges_dialog_title)) },
        text = {
            Column {

                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(bottom = 10.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.custom_forges_dialog_builtin_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Text(
                    text = stringResource(Res.string.custom_forges_dialog_help),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GhsTextField(
                        value = state.customForgeDraft,
                        onValueChange = { onAction(TweaksAction.OnCustomForgeDraftChanged(it)) },
                        placeholder = "forgejo.example.com",
                        singleLine = true,
                        isError = state.customForgeError != null,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onAction(TweaksAction.OnAddCustomForge) }) {
                        Text(stringResource(Res.string.custom_forges_add_button))
                    }
                }
                if (state.customForgeError != null) {
                    Text(
                        text = state.customForgeError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (state.customForgeHosts.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.custom_forges_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp).padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(items = state.customForgeHosts.toList(), key = { it }) { host ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(host) },
                                trailingIcon = {
                                    IconButton(onClick = { onAction(TweaksAction.OnRemoveCustomForge(host)) }) {
                                        Icon(Icons.Default.Close, contentDescription = null)
                                    }
                                },
                                colors = InputChipDefaults.inputChipColors(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(TweaksAction.OnDismissCustomForgesDialog) }) {
                Text(stringResource(Res.string.done))
            }
        },
    )
}
