package zed.rainxch.core.presentation.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.theme.tokens.Radii

/**
 * Active/inactive filter chip (DESIGN.md §7.2). Active = tintP bg + primary text +
 * 1dp primary-tinted border. Inactive = transparent + outline border + ink text.
 * Optional `×` chip — caller composes via dismiss arg.
 */
@Composable
fun FilterChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val bg: Color = if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val fg: Color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val border: Color = if (active) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.33f)
    } else {
        MaterialTheme.colorScheme.outline
    }
    Row(
        modifier = modifier
            .clip(Radii.chip)
            .background(bg)
            .border(width = 1.dp, color = border, shape = Radii.chip)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
            ),
        )
        if (active && onDismiss != null) {
            Text(
                text = "×",
                color = fg,
                modifier = Modifier.clickable(onClick = onDismiss),
                fontSize = 14.sp,
            )
        }
    }
}
