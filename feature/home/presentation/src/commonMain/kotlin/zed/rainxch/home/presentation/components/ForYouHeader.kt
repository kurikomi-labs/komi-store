package zed.rainxch.home.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.icon.KomiIcon
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.manga.decoration.speedLineWash
import zed.rainxch.core.presentation.personality.usesDecor
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.home_masthead_subtitle
import zed.rainxch.githubstore.core.presentation.res.home_masthead_title
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ForYouHeader(
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    val personality = LocalPersonality.current
    val colors = personality.colors
    val type = personality.type
    val washColor = colors.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .clipToBounds()
            .speedLineWash(color = washColor)
            .padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        BurstEmblem()

        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = stringResource(Res.string.home_masthead_title).uppercase(),
                style = type.title.copy(
                    fontSize = 27.sp,
                    lineHeight = 0.92.em,
                    color = colors.onSurface,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (LocalPersonality.current.usesDecor) {
                BasicText(
                    text = stringResource(Res.string.home_masthead_subtitle),
                    style = type.label.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 0.18.em,
                        color = colors.onSurfaceVariant,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        trailing?.invoke()
    }
}

@Composable
private fun BurstEmblem() {
    val colors = LocalPersonality.current.colors
    Box(
        modifier = Modifier.size(42.dp).rotate(-4f),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = size.minDimension / 42f
            val cx = 21f * s
            val cy = 21f * s
            val outer = 19.5f * s
            val inner = 12.5f * s
            val spikes = 9
            val path = Path()
            for (i in 0 until spikes * 2) {
                val r = if (i % 2 == 0) outer else inner
                val a = (-PI / 2 + i * PI / spikes).toFloat()
                val x = cx + r * cos(a)
                val y = cy + r * sin(a)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path = path, color = colors.primary, style = Fill)
            drawPath(
                path = path,
                color = colors.outline,
                style = Stroke(width = 2f * s, join = StrokeJoin.Round),
            )
        }
        KomiIcon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = colors.onPrimary,
        )
    }
}
