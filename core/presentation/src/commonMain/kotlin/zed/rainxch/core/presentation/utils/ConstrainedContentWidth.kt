package zed.rainxch.core.presentation.utils

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.presentation.locals.LocalContentWidth

private val MIN_CONTENT_DP = 480.dp
private val MAX_CONTENT_DP = 1600.dp

private fun ContentWidth.fraction(): Float = when (this) {
    ContentWidth.COMPACT -> 0.55f
    ContentWidth.WIDE -> 0.75f
    ContentWidth.EXTRA_WIDE -> 0.95f
}

@Composable
@ReadOnlyComposable
fun contentWidthCap(): Dp {
    val fraction = LocalContentWidth.current.fraction()
    val windowPx = LocalWindowInfo.current.containerSize.width
    val windowDp = with(LocalDensity.current) { windowPx.toDp() }
    return (windowDp * fraction).coerceIn(MIN_CONTENT_DP, MAX_CONTENT_DP)
}

@Composable
@ReadOnlyComposable
fun Modifier.constrainedContentWidth(): Modifier = widthIn(max = contentWidthCap())
