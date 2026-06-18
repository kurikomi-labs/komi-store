package zed.rainxch.core.presentation.personality.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp

@Immutable
data class PersonalityShape(
    val borderPanel: Dp,
    val borderButton: Dp,
    val borderChip: Dp,
    val corner: Dp,
    val cornerSmall: Dp,
    val skewStampDeg: Float,
    val badgeRotationDeg: Float,
)
