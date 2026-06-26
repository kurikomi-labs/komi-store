package zed.rainxch.core.presentation.locals

import androidx.compose.runtime.staticCompositionLocalOf
import zed.rainxch.core.presentation.personality.Personality

val LocalPersonality =
    staticCompositionLocalOf<Personality> {
        error("No Personality provided. Wrap content in PersonalityTheme { }.")
    }
