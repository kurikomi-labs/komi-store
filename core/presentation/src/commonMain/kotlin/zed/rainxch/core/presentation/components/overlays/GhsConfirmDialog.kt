package zed.rainxch.core.presentation.components.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape
import zed.rainxch.core.presentation.vocabulary.Squiggle

@Composable
fun GhsConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancelLabel: String = "Cancel",
    destructive: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    Dialog(onDismissRequest = onDismiss) {
        val cs = MaterialTheme.colorScheme
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .clip(WonkySquircleShape.Dialog)
                .background(cs.surface)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leading != null) {
                Box(modifier = Modifier.padding(bottom = 4.dp)) { leading() }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                ),
                color = cs.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = body,
                color = cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Squiggle()
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GhsButton(
                    onClick = onDismiss,
                    label = cancelLabel,
                    variant = GhsButtonVariant.Outline,
                    size = GhsButtonSize.Sm,
                )
                GhsButton(
                    onClick = onConfirm,
                    label = confirmLabel,
                    variant = if (destructive) GhsButtonVariant.Destructive else GhsButtonVariant.Primary,
                    size = GhsButtonSize.Sm,
                )
            }
        }
    }
}
