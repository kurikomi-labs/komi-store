package zed.rainxch.tweaks.presentation.components.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.components.icon.KomiIcon
import zed.rainxch.core.presentation.locals.LocalPersonality

@Composable
fun ColumnScope.SettingsExpandableRow(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    last: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalPersonality.current.colors
    SettingsRow(
        title = title,
        subtitle = subtitle,
        last = last && !expanded,
        onClick = onToggle,
        modifier = modifier,
        trailing = {
            KomiIcon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(20.dp).rotate(if (expanded) 90f else 0f),
            )
        },
    )
    AnimatedVisibility(visible = expanded) {
        Column(modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 15.dp)) {
            content()
        }
    }
}
