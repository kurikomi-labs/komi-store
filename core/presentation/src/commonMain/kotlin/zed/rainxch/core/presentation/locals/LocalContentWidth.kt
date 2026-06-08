package zed.rainxch.core.presentation.locals

import androidx.compose.runtime.compositionLocalOf
import zed.rainxch.core.domain.model.appearance.ContentWidth

val LocalContentWidth = compositionLocalOf { ContentWidth.COMPACT }
