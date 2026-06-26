package zed.rainxch.core.presentation.locals

import androidx.compose.runtime.staticCompositionLocalOf
import zed.rainxch.core.presentation.status.StatusColors
import zed.rainxch.core.presentation.status.statusColors

val LocalStatusColors = staticCompositionLocalOf<StatusColors> { statusColors(dark = false) }
