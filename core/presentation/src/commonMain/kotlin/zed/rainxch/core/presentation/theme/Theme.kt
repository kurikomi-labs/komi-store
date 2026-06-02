package zed.rainxch.core.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.presentation.theme.tokens.Tokens
import zed.rainxch.core.presentation.theme.tokens.colorSchemeFor
import zed.rainxch.core.presentation.utils.toTokenPalette

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GithubStoreTheme(
    isDarkTheme: Boolean = false,
    appTheme: AppTheme = AppTheme.NORD,
    fontTheme: FontTheme = FontTheme.CUSTOM,
    isAmoledTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val mode = when {
        !isDarkTheme -> Tokens.Mode.LIGHT
        isAmoledTheme -> Tokens.Mode.AMOLED
        else -> Tokens.Mode.DARK
    }
    val tokenPalette = appTheme.toTokenPalette()
    val palette = Tokens.palette(tokenPalette, mode)
    val dynamic = if (appTheme == AppTheme.DYNAMIC) {
        dynamicColorScheme(isDark = isDarkTheme)
    } else {
        null
    }
    val baseScheme = colorSchemeFor(palette = tokenPalette, mode = mode)
    val targetScheme = if (dynamic != null) {
        applyDynamicSurfaces(
            dynamic = dynamic,
            isDark = isDarkTheme,
            isAmoled = isAmoledTheme && isDarkTheme,
        )
    } else {
        baseScheme
    }
    val animatedScheme = animateColorScheme(target = targetScheme)

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalStatusColors provides if (isDarkTheme) darkStatusColors else defaultStatusColors,
        LocalThresholds provides defaultThresholds,
        LocalMotion provides defaultMotion,
        LocalSpacing provides defaultSpacing,
    ) {
        MaterialExpressiveTheme(
            colorScheme = animatedScheme,
            typography = getAppTypography(fontTheme),
            motionScheme = MotionScheme.expressive(),
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}

private fun applyDynamicSurfaces(
    dynamic: ColorScheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val accent = dynamic.primary
    val base = when {
        isAmoled -> Color.Black
        isDark -> Color(0xFF101319)
        else -> Color.White
    }
    fun tint(ratio: Float): Color = blend(base, accent, ratio)
    return dynamic.copy(
        background = if (isAmoled) Color.Black else tint(if (isDark) 0.04f else 0.03f),
        surface = if (isAmoled) Color(0xFF0B0F14) else tint(if (isDark) 0.04f else 0.03f),
        surfaceVariant = tint(if (isDark) 0.15f else 0.11f),
        surfaceTint = accent,
        surfaceBright = tint(if (isDark) 0.18f else 0.02f),
        surfaceDim = tint(if (isDark) 0.02f else 0.10f),
        surfaceContainerLowest = if (isAmoled) Color.Black else if (isDark) base else Color.White,
        surfaceContainerLow = tint(if (isDark) 0.07f else 0.06f),
        surfaceContainer = tint(if (isDark) 0.10f else 0.09f),
        surfaceContainerHigh = tint(if (isDark) 0.13f else 0.12f),
        surfaceContainerHighest = tint(if (isDark) 0.17f else 0.15f),
        outline = tint(if (isDark) 0.32f else 0.28f),
        outlineVariant = tint(if (isDark) 0.20f else 0.16f),
    )
}

private fun blend(base: Color, accent: Color, ratio: Float): Color {
    val r = ratio.coerceIn(0f, 1f)
    val inv = 1f - r
    return Color(
        red = base.red * inv + accent.red * r,
        green = base.green * inv + accent.green * r,
        blue = base.blue * inv + accent.blue * r,
        alpha = 1f,
    )
}
