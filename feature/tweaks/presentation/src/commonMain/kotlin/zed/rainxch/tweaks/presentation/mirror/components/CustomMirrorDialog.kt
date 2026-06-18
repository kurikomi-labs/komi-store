package zed.rainxch.tweaks.presentation.mirror.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.inputs.KomiTextField
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
                KomiTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    placeholder = stringResource(Res.string.mirror_custom_dialog_hint),
                    error = error?.let { stringResource(it) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            KomiButton(
                onClick = onConfirm,
                label = stringResource(Res.string.mirror_custom_save),
                variant = KomiButtonVariant.Text,
                size = KomiButtonSize.Sm,
                enabled = draft.isNotBlank() && error == null,
            )
        },
        dismissButton = {
            KomiButton(
                onClick = onDismiss,
                label = stringResource(Res.string.cancel),
                variant = KomiButtonVariant.Text,
                size = KomiButtonSize.Sm,
            )
        },
    )
}
