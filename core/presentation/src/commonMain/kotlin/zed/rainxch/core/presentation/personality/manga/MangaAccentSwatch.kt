package zed.rainxch.core.presentation.personality.manga

import androidx.compose.ui.graphics.Color

fun mangaAccentSwatch(accent: MangaAccent): Pair<Color, Color>? =
    when (accent) {
        MangaAccent.MONO -> null
        MangaAccent.CRIMSON -> Color(0xFFD8202A) to Color(0xFFFFFFFF)
        MangaAccent.COBALT -> Color(0xFF1F4ED8) to Color(0xFFFFFFFF)
        MangaAccent.SUN -> Color(0xFFF5A300) to Color(0xFF1B150D)
        MangaAccent.FROST -> Color(0xFF88C0D0) to Color(0xFF2E3440)
    }
