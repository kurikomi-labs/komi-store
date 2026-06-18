package zed.rainxch.tweaks.presentation.feedback.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.feedback_send_via_email
import zed.rainxch.githubstore.core.presentation.res.feedback_send_via_github

@Composable
fun SendActions(
    canSend: Boolean,
    isSending: Boolean,
    onSendEmail: () -> Unit,
    onSendGithub: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KomiButton(
            onClick = onSendGithub,
            label = stringResource(Res.string.feedback_send_via_github),
            variant = KomiButtonVariant.Outline,
            enabled = canSend,
            loading = isSending,
            modifier = Modifier.weight(1f),
        )
        KomiButton(
            onClick = onSendEmail,
            label = stringResource(Res.string.feedback_send_via_email),
            variant = KomiButtonVariant.Primary,
            enabled = canSend,
            loading = isSending,
            modifier = Modifier.weight(1f),
        )
    }
}
