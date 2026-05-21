package zed.rainxch.core.presentation.components.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.shapes.WonkySquircleShape

enum class ToastTint { Default, Success, Error, Info }

@Composable
fun GhsToast(
    modifier: Modifier = Modifier,
    tint: ToastTint = ToastTint.Default,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val (bg, border) = when (tint) {
        ToastTint.Default -> cs.surface to cs.outline
        ToastTint.Success -> cs.tertiaryContainer to cs.tertiary.copy(alpha = 0.55f)
        ToastTint.Error -> cs.errorContainer to cs.error.copy(alpha = 0.55f)
        ToastTint.Info -> cs.primaryContainer to cs.primary.copy(alpha = 0.55f)
    }
    Row(
        modifier = modifier
            .clip(WonkySquircleShape.Toast)
            .background(bg)
            .border(width = 1.dp, color = border, shape = WonkySquircleShape.Toast)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        ProvideTextStyle(value = MaterialTheme.typography.bodyMedium) {
            content()
        }
        trailing?.invoke()
    }
}
