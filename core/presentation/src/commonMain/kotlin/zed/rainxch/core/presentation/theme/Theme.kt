package zed.rainxch.core.presentation.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
    val targetScheme = colorSchemeFor(palette = tokenPalette, mode = mode)
    val animatedScheme = animateColorScheme(target = targetScheme)

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalStatusColors provides defaultStatusColors,
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
