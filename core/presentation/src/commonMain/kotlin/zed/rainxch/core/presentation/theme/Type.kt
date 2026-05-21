package zed.rainxch.core.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.fraunces
import zed.rainxch.githubstore.core.presentation.res.fraunces_italic
import zed.rainxch.githubstore.core.presentation.res.inter_tight
import zed.rainxch.githubstore.core.presentation.res.jetbrains_mono

val fraunces
    @Composable get() = FontFamily(
        Font(Res.font.fraunces, FontWeight.Medium, FontStyle.Normal),
        Font(Res.font.fraunces, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.fraunces, FontWeight.Bold, FontStyle.Normal),
        Font(Res.font.fraunces_italic, FontWeight.Medium, FontStyle.Italic),
        Font(Res.font.fraunces_italic, FontWeight.SemiBold, FontStyle.Italic),
        Font(Res.font.fraunces_italic, FontWeight.Bold, FontStyle.Italic),
    )

val interTight
    @Composable get() = FontFamily(
        Font(Res.font.inter_tight, FontWeight.Normal),
        Font(Res.font.inter_tight, FontWeight.Medium),
        Font(Res.font.inter_tight, FontWeight.SemiBold),
        Font(Res.font.inter_tight, FontWeight.Bold),
    )

val jetbrainsMono
    @Composable get() = FontFamily(
        Font(Res.font.jetbrains_mono, FontWeight.Normal),
        Font(Res.font.jetbrains_mono, FontWeight.Medium),
        Font(Res.font.jetbrains_mono, FontWeight.Bold),
    )

private val baseline = Typography()

@Composable
fun getAppTypography(fontTheme: FontTheme = FontTheme.CUSTOM): Typography {
    if (fontTheme == FontTheme.SYSTEM) return baseline

    val serif = fraunces
    val sans = interTight

    fun TextStyle.fraunces(weight: FontWeight) = copy(
        fontFamily = serif,
        fontWeight = weight,
        fontStyle = FontStyle.Italic,
        letterSpacing = (-0.02).em,
    )

    fun TextStyle.sans(weight: FontWeight) = copy(
        fontFamily = sans,
        fontWeight = weight,
        textDecoration = TextDecoration.None,
    )

    return Typography(

        displayLarge = baseline.displayLarge.fraunces(FontWeight.SemiBold).copy(
            letterSpacing = (-0.025).em,
            fontSize = 36.sp,
        ),
        displayMedium = baseline.displayMedium.fraunces(FontWeight.SemiBold).copy(fontSize = 32.sp),
        displaySmall = baseline.displaySmall.fraunces(FontWeight.SemiBold).copy(fontSize = 28.sp),
        headlineLarge = baseline.headlineLarge.fraunces(FontWeight.SemiBold).copy(fontSize = 26.sp),
        headlineMedium = baseline.headlineMedium.fraunces(FontWeight.SemiBold).copy(fontSize = 22.sp),
        headlineSmall = baseline.headlineSmall.fraunces(FontWeight.SemiBold).copy(fontSize = 20.sp),
        titleLarge = baseline.titleLarge.fraunces(FontWeight.SemiBold).copy(fontSize = 18.sp),
        titleMedium = baseline.titleMedium.fraunces(FontWeight.SemiBold).copy(fontSize = 16.sp),
        titleSmall = baseline.titleSmall.fraunces(FontWeight.SemiBold).copy(fontSize = 14.sp),

        bodyLarge = baseline.bodyLarge.sans(FontWeight.Normal).copy(fontSize = 14.sp),
        bodyMedium = baseline.bodyMedium.sans(FontWeight.Normal).copy(fontSize = 13.sp),
        bodySmall = baseline.bodySmall.sans(FontWeight.Medium).copy(fontSize = 12.sp),
        labelLarge = baseline.labelLarge.sans(FontWeight.SemiBold),
        labelMedium = baseline.labelMedium.sans(FontWeight.SemiBold),
        labelSmall = baseline.labelSmall.sans(FontWeight.SemiBold),
    )
}
