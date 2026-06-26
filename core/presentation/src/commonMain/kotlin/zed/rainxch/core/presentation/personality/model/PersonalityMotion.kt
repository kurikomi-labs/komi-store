package zed.rainxch.core.presentation.personality.model

import androidx.compose.runtime.Immutable

@Immutable
data class PersonalityMotion(
    val level: MotionLevel,
    val cardEnterMs: Int,
    val screenTransitionMs: Int,
    val pressMs: Int,
)
