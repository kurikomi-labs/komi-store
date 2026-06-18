package zed.rainxch.core.presentation.personality.manga

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import zed.rainxch.core.presentation.personality.model.PersonalityType
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.anton_regular
import zed.rainxch.githubstore.core.presentation.res.jetbrains_mono
import zed.rainxch.githubstore.core.presentation.res.zen_kaku_black
import zed.rainxch.githubstore.core.presentation.res.zen_kaku_medium

@Composable
fun mangaTypeWithFonts(base: PersonalityType): PersonalityType {
    val anton = FontFamily(Font(Res.font.anton_regular, FontWeight.Normal))
    val zenKaku =
        FontFamily(
            Font(Res.font.zen_kaku_medium, FontWeight.Medium),
            Font(Res.font.zen_kaku_black, FontWeight.Black),
        )
    val jetBrainsMono = FontFamily(Font(Res.font.jetbrains_mono, FontWeight.Normal))
    return base.withFamilies(display = anton, body = zenKaku, mono = jetBrainsMono)
}
