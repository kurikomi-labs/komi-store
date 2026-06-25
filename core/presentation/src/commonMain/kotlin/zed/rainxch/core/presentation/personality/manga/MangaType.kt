package zed.rainxch.core.presentation.personality.manga

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.personality.model.PersonalityType

// Real families (Anton / Noto Sans per script / JetBrains Mono) are injected at theme time
// by withMangaFonts() — @Composable, since CMP Res fonts require composition. These
// defaults only show if a Personality is read without PersonalityTheme wrapping it.
private val MangaDisplayFamily = FontFamily.Default
private val MangaBodyFamily = FontFamily.Default
private val MangaMonoFamily = FontFamily.Default

internal val MangaType =
    PersonalityType(
        display =
            TextStyle(
                fontFamily = MangaDisplayFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.02.em,
            ),
        title =
            TextStyle(
                fontFamily = MangaDisplayFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                lineHeight = 23.sp,
                letterSpacing = 0.01.em,
            ),
        stamp =
            TextStyle(
                fontFamily = MangaDisplayFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 15.sp,
                letterSpacing = 0.06.em,
            ),
        body =
            TextStyle(
                fontFamily = MangaBodyFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
            ),
        label =
            TextStyle(
                fontFamily = MangaBodyFamily,
                fontWeight = FontWeight.Black,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.02.em,
            ),
        mono =
            TextStyle(
                fontFamily = MangaMonoFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
        uppercaseHeadings = true,
    )
