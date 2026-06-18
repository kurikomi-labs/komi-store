package zed.rainxch.core.presentation.personality.classic

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import zed.rainxch.core.presentation.personality.model.PersonalityType
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.geist
import zed.rainxch.githubstore.core.presentation.res.geist_mono

@Composable
fun classicTypeWithFonts(base: PersonalityType): PersonalityType {
    val geist =
        FontFamily(
            Font(Res.font.geist, FontWeight.Normal),
            Font(Res.font.geist, FontWeight.Medium),
            Font(Res.font.geist, FontWeight.SemiBold),
            Font(Res.font.geist, FontWeight.Bold),
        )
    val geistMono =
        FontFamily(
            Font(Res.font.geist_mono, FontWeight.Normal),
            Font(Res.font.geist_mono, FontWeight.Medium),
        )
    return base.withFamilies(display = geist, body = geist, mono = geistMono)
}
