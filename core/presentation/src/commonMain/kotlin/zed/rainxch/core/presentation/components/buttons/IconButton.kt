package zed.rainxch.core.presentation.components.buttons

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * 36×36 transparent icon button. Back / Share / Favorite / More (DESIGN.md §7.1).
 * Min 48dp touch target preserved via padding inside the click area.
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    sizeDp: Int = 36,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .size(sizeDp.dp.coerceAtLeast(48.dp)),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
