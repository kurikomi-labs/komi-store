package zed.rainxch.core.presentation.components.section

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.theme.tokens.Radii

enum class BannerTint { Info, Success, Warning, Danger }

@Composable
fun Banner(
    tint: BannerTint = BannerTint.Info,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val (bg, border) = when (tint) {
        BannerTint.Info -> cs.primaryContainer to cs.primary.copy(alpha = 0.2f)
        BannerTint.Success -> cs.tertiaryContainer to cs.tertiary.copy(alpha = 0.2f)
        BannerTint.Warning -> Color(0xFFFFE6CC) to Color(0xFFC49652).copy(alpha = 0.4f)
        BannerTint.Danger -> cs.errorContainer to cs.error.copy(alpha = 0.2f)
    }
    Row(
        modifier = modifier
            .clip(Radii.card)
            .background(bg)
            .border(width = 1.dp, color = border, shape = Radii.card)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke()
        androidx.compose.foundation.layout.Box(modifier = Modifier.weight(1f)) {
            content()
        }
        trailing?.invoke()
    }
}
