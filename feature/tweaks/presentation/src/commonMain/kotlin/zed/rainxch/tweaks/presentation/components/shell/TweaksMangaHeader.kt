package zed.rainxch.tweaks.presentation.components.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.icon.KomiIcon
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.manga.decoration.hardShadow
import zed.rainxch.core.presentation.personality.manga.decoration.screentoneCorner
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.back_cd
import zed.rainxch.githubstore.core.presentation.res.tweaks_title

@Composable
fun TweaksMangaHeader(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPersonality.current.colors
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(colors.background)
                .screentoneCorner(colors.onSurface, colors.screentoneOpacity)
                .drawBehind {
                    val stroke = 3.dp.toPx()
                    drawLine(
                        color = colors.outline,
                        start = Offset(0f, size.height - stroke / 2f),
                        end = Offset(size.width, size.height - stroke / 2f),
                        strokeWidth = stroke,
                    )
                }
                .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 42.dp, height = 38.dp)
                    .hardShadow(DpOffset(2.dp, 2.dp), colors.shadow)
                    .background(colors.surface)
                    .border(2.5.dp, colors.outline)
                    .clickable(onClick = onNavigateBack),
            contentAlignment = Alignment.Center,
        ) {
            KomiIcon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.back_cd),
                tint = colors.onSurface,
                modifier = Modifier.size(22.dp),
            )
        }
        Column {
            KomiText(
                text = stringResource(Res.string.tweaks_title),
                role = KomiTextRole.Display,
                color = colors.onSurface,
                fontSize = 23.sp,
                lineHeight = 23.sp,
            )
            KomiText(
                text = "設定 · SETTINGS",
                role = KomiTextRole.Label,
                color = colors.onSurfaceVariant,
                fontSize = 10.sp,
                letterSpacing = 0.16.em,
                uppercase = false,
            )
        }
    }
}
