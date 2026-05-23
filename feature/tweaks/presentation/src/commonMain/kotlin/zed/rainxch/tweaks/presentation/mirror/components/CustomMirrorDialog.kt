package zed.rainxch.tweaks.presentation.mirror.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.cancel
import zed.rainxch.githubstore.core.presentation.res.mirror_custom_dialog_hint
import zed.rainxch.githubstore.core.presentation.res.mirror_custom_dialog_title
import zed.rainxch.githubstore.core.presentation.res.mirror_custom_save

@Composable
fun CustomMirrorDialog(
    draft: String,
    error: StringResource?,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.mirror_custom_dialog_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                GhsTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = stringResource(Res.string.mirror_custom_dialog_hint),
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                if (error != null) {
                    Text(
                        text = stringResource(error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = draft.isNotBlank() && error == null,
            ) {
                Text(stringResource(Res.string.mirror_custom_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
