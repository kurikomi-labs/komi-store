package zed.rainxch.core.presentation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class FormFactor {
    COMPACT,
    MEDIUM,
    EXPANDED,
}

val FormFactor.isCompact: Boolean get() = this == FormFactor.COMPACT
val FormFactor.isExpanded: Boolean get() = this == FormFactor.EXPANDED

object WindowBreakpoints {
    val Medium: Dp = 600.dp
    val Expanded: Dp = 840.dp
}

fun formFactorFor(width: Dp): FormFactor =
    when {
        width < WindowBreakpoints.Medium -> FormFactor.COMPACT
        width < WindowBreakpoints.Expanded -> FormFactor.MEDIUM
        else -> FormFactor.EXPANDED
    }

@Composable
@ReadOnlyComposable
fun currentWindowSize(): DpSize {
    val info = LocalWindowInfo.current
    return with(LocalDensity.current) {
        DpSize(info.containerSize.width.toDp(), info.containerSize.height.toDp())
    }
}

@Composable
@ReadOnlyComposable
fun currentWindowWidth(): Dp = currentWindowSize().width

@Composable
@ReadOnlyComposable
fun currentFormFactor(): FormFactor = formFactorFor(currentWindowWidth())
