package zed.rainxch.core.presentation.personality

import zed.rainxch.core.presentation.personality.model.PersonalityColors
import zed.rainxch.core.presentation.personality.model.PersonalityDecor
import zed.rainxch.core.presentation.personality.model.PersonalityMotion
import zed.rainxch.core.presentation.personality.model.PersonalityShadow
import zed.rainxch.core.presentation.personality.model.PersonalityShape
import zed.rainxch.core.presentation.personality.model.PersonalityType

sealed interface Personality {
    val colors: PersonalityColors
    val type: PersonalityType
    val shape: PersonalityShape
    val shadow: PersonalityShadow
    val motion: PersonalityMotion
    val decor: PersonalityDecor get() = PersonalityDecor.None
}

val Personality.usesDecor: Boolean get() = decor != PersonalityDecor.None
