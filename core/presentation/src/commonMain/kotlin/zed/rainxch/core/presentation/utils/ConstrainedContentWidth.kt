package zed.rainxch.core.presentation.utils

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.presentation.locals.LocalContentWidth

@Composable
@ReadOnlyComposable
fun Modifier.constrainedContentWidth(): Modifier {
    val cap = when (LocalContentWidth.current) {
        ContentWidth.COMPACT -> 680.dp
        ContentWidth.WIDE -> 960.dp
        ContentWidth.EXTRA_WIDE -> null
    }
    return if (cap != null) widthIn(max = cap) else this
}
