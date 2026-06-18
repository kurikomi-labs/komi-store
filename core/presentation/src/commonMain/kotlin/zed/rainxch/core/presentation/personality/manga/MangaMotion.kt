package zed.rainxch.core.presentation.personality.manga

import zed.rainxch.core.presentation.personality.model.MotionLevel
import zed.rainxch.core.presentation.personality.model.PersonalityMotion

internal fun mangaMotion(level: MotionLevel): PersonalityMotion =
    when (level) {
        MotionLevel.FULL -> PersonalityMotion(level, cardEnterMs = 420, screenTransitionMs = 430, pressMs = 100)
        MotionLevel.SUBTLE -> PersonalityMotion(level, cardEnterMs = 240, screenTransitionMs = 0, pressMs = 100)
        MotionLevel.OFF -> PersonalityMotion(level, cardEnterMs = 0, screenTransitionMs = 0, pressMs = 90)
    }
