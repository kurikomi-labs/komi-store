package zed.rainxch.tweaks.presentation.components.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality

fun Modifier.settingsRowDivider(
    color: androidx.compose.ui.graphics.Color,
    show: Boolean,
): Modifier =
    if (!show) this
    else
        drawBehind {
            val stroke = 2.dp.toPx()
            drawLine(
                color = color,
                start = Offset(0f, size.height - stroke / 2f),
                end = Offset(size.width, size.height - stroke / 2f),
                strokeWidth = stroke,
            )
        }

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    strong: Boolean = false,
    last: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = LocalPersonality.current.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .settingsRowDivider(colors.outline.copy(alpha = 0.22f), show = !last)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            KomiText(
                text = title,
                role = KomiTextRole.Body,
                color = colors.onSurface,
                fontSize = 14.5.sp,
                fontWeight = if (strong) FontWeight.Black else FontWeight.Bold,
                uppercase = false,
            )
            if (subtitle != null) {
                KomiText(
                    text = subtitle,
                    role = KomiTextRole.Body,
                    color = colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    uppercase = false,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        if (trailing != null) {
            Row(
                modifier = Modifier.widthIn(min = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = { trailing() },
            )
        }
    }
}
