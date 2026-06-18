package zed.rainxch.core.presentation.personality

import androidx.compose.runtime.Immutable
import zed.rainxch.core.presentation.personality.manga.HeadlineMarker
import zed.rainxch.core.presentation.personality.manga.MangaAccent
import zed.rainxch.core.presentation.personality.manga.MangaPaper
import zed.rainxch.core.presentation.personality.manga.MangaShadow
import zed.rainxch.core.presentation.personality.manga.MangaShape
import zed.rainxch.core.presentation.personality.manga.MangaType
import zed.rainxch.core.presentation.personality.manga.mangaColors
import zed.rainxch.core.presentation.personality.manga.mangaMotion
import zed.rainxch.core.presentation.personality.model.MotionLevel
import zed.rainxch.core.presentation.personality.model.PersonalityColors
import zed.rainxch.core.presentation.personality.model.PersonalityMotion
import zed.rainxch.core.presentation.personality.model.PersonalityShadow
import zed.rainxch.core.presentation.personality.model.PersonalityShape
import zed.rainxch.core.presentation.personality.model.PersonalityType

@Immutable
data class MangaPersonality(
    override val colors: PersonalityColors,
    override val type: PersonalityType,
    override val shape: PersonalityShape,
    override val shadow: PersonalityShadow,
    override val motion: PersonalityMotion,
    val headlineMarker: HeadlineMarker,
    val screentone: Boolean,
    val speedLines: Boolean,
    val starburst: Boolean,
    val inkedIcons: Boolean,
    val panelTilt: Boolean,
) : Personality

fun mangaPersonality(
    paper: MangaPaper = MangaPaper.DAY,
    accent: MangaAccent = MangaAccent.CRIMSON,
    motion: MotionLevel = MotionLevel.SUBTLE,
    headlineMarker: HeadlineMarker = HeadlineMarker.Stamp,
): MangaPersonality =
    MangaPersonality(
        colors = mangaColors(paper, accent),
        type = MangaType,
        shape = MangaShape,
        shadow = MangaShadow,
        motion = mangaMotion(motion),
        headlineMarker = headlineMarker,
        screentone = true,
        speedLines = true,
        starburst = true,
        inkedIcons = true,
        panelTilt = false,
    )
