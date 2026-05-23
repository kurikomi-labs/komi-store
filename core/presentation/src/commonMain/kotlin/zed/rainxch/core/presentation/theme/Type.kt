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
import zed.rainxch.githubstore.core.presentation.res.geist
import zed.rainxch.githubstore.core.presentation.res.geist_mono

val geist
    @Composable get() = FontFamily(
        Font(Res.font.geist, FontWeight.Normal, FontStyle.Normal),
        Font(Res.font.geist, FontWeight.Medium, FontStyle.Normal),
        Font(Res.font.geist, FontWeight.SemiBold, FontStyle.Normal),
        Font(Res.font.geist, FontWeight.Bold, FontStyle.Normal),
        Font(Res.font.geist, FontWeight.Black, FontStyle.Normal),
    )

val geistMono
    @Composable get() = FontFamily(
        Font(Res.font.geist_mono, FontWeight.Normal),
        Font(Res.font.geist_mono, FontWeight.Medium),
        Font(Res.font.geist_mono, FontWeight.SemiBold),
        Font(Res.font.geist_mono, FontWeight.Bold),
    )

@Deprecated("Use geist", ReplaceWith("geist"))
val fraunces
    @Composable get() = geist

@Deprecated("Use geist", ReplaceWith("geist"))
val interTight
    @Composable get() = geist

@Deprecated("Use geistMono", ReplaceWith("geistMono"))
val jetbrainsMono
    @Composable get() = geistMono

private val baseline = Typography()

@Composable
fun getAppTypography(fontTheme: FontTheme = FontTheme.CUSTOM): Typography {
    if (fontTheme == FontTheme.SYSTEM) return baseline

    val family = geist

    fun TextStyle.display(weight: FontWeight) = copy(
        fontFamily = family,
        fontWeight = weight,
        fontStyle = FontStyle.Normal,
        letterSpacing = (-0.022).em,
        textDecoration = TextDecoration.None,
    )

    fun TextStyle.body(weight: FontWeight) = copy(
        fontFamily = family,
        fontWeight = weight,
        fontStyle = FontStyle.Normal,
        textDecoration = TextDecoration.None,
    )

    return Typography(
        displayLarge = baseline.displayLarge.display(FontWeight.Bold).copy(
            letterSpacing = (-0.028).em,
            fontSize = 36.sp,
        ),
        displayMedium = baseline.displayMedium.display(FontWeight.Bold).copy(fontSize = 32.sp),
        displaySmall = baseline.displaySmall.display(FontWeight.Bold).copy(fontSize = 28.sp),
        headlineLarge = baseline.headlineLarge.display(FontWeight.SemiBold).copy(fontSize = 26.sp),
        headlineMedium = baseline.headlineMedium.display(FontWeight.SemiBold).copy(fontSize = 22.sp),
        headlineSmall = baseline.headlineSmall.display(FontWeight.SemiBold).copy(fontSize = 20.sp),
        titleLarge = baseline.titleLarge.display(FontWeight.SemiBold).copy(fontSize = 18.sp),
        titleMedium = baseline.titleMedium.display(FontWeight.SemiBold).copy(fontSize = 16.sp),
        titleSmall = baseline.titleSmall.display(FontWeight.SemiBold).copy(fontSize = 14.sp),

        bodyLarge = baseline.bodyLarge.body(FontWeight.Normal).copy(fontSize = 14.sp),
        bodyMedium = baseline.bodyMedium.body(FontWeight.Normal).copy(fontSize = 13.sp),
        bodySmall = baseline.bodySmall.body(FontWeight.Medium).copy(fontSize = 12.sp),
        labelLarge = baseline.labelLarge.body(FontWeight.SemiBold),
        labelMedium = baseline.labelMedium.body(FontWeight.SemiBold),
        labelSmall = baseline.labelSmall.body(FontWeight.SemiBold),
    )
}
