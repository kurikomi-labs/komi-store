package zed.rainxch.core.presentation.components.buttons

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Outline tertiary action — transparent fill, 1dp outline ring, pill-shaped.
 * Inspect / Refresh / Filter / Cancel (DESIGN.md §7.1).
 */
@Composable
fun OutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val pillShape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(pillShape)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = if (enabled) 1f else 0.38f),
                shape = pillShape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (enabled) 1f else 0.38f,
            ),
        ) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                content()
            }
        }
    }
}
