package zed.rainxch.tweaks.presentation.components.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.manga.decoration.hardShadow

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val personality = LocalPersonality.current
    val colors = personality.colors
    val shape = RoundedCornerShape(personality.shape.corner)
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .hardShadow(DpOffset(6.dp, 6.dp), colors.shadow, shape)
                .background(colors.surface, shape)
                .border(3.dp, colors.outline, shape),
        content = content,
    )
}
