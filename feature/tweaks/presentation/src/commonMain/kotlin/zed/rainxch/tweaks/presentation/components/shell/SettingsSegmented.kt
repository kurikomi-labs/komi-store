package zed.rainxch.tweaks.presentation.components.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality

data class SettingsSegment<T>(
    val value: T,
    val label: String,
)

@Composable
fun <T> SettingsSegmented(
    value: T,
    options: List<SettingsSegment<T>>,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
) {
    val colors = LocalPersonality.current.colors
    val barHeight = if (small) 28.dp else 34.dp
    Row(
        modifier =
            modifier
                .height(barHeight)
                .background(colors.surfaceVariant)
                .border(2.5.dp, colors.outline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, segment ->
            val active = segment.value == value
            if (index > 0) {
                Box(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(colors.outline),
                )
            }
            Box(
                modifier =
                    Modifier
                        .fillMaxHeight()
                        .background(if (active) colors.primary else Color.Transparent)
                        .clickable { onSelect(segment.value) }
                        .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                KomiText(
                    text = segment.label,
                    role = KomiTextRole.Stamp,
                    color = if (active) colors.onPrimary else colors.onSurface,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
