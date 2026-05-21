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

/**
 * App-wide theme entry point. Resolves the active [AppTheme] palette + light/dark/amoled
 * mode to a Material 3 [ColorScheme] backed by the design tokens in
 * [zed.rainxch.core.presentation.theme.tokens.Tokens], plus provides composition locals
 * that expose richer surfaces (status colors, thresholds, motion, spacing).
 *
 * `Main.kt` is the only call site — kept under the legacy `GithubStoreTheme` name until
 * P6 chrome polish swaps in the user-facing rename to `GhsTheme`.
 */
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
    val scheme = colorSchemeFor(palette = tokenPalette, mode = mode)

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalStatusColors provides defaultStatusColors,
        LocalThresholds provides defaultThresholds,
        LocalMotion provides defaultMotion,
        LocalSpacing provides defaultSpacing,
    ) {
        MaterialExpressiveTheme(
            colorScheme = scheme,
            typography = getAppTypography(fontTheme),
            motionScheme = MotionScheme.expressive(),
            shapes = MaterialTheme.shapes,
            content = content,
        )
    }
}
