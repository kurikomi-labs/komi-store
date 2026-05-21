package zed.rainxch.core.presentation.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.presentation.theme.tokens.Tokens
import zed.rainxch.core.presentation.theme.tokens.colorSchemeFor
import zed.rainxch.core.presentation.utils.toTokenPalette

/**
 * App-wide theme entry point. Resolves the active [AppTheme] palette + light/dark/amoled
 * mode to a Material 3 [ColorScheme] backed by the design tokens in
 * [zed.rainxch.core.presentation.theme.tokens.Tokens].
 *
 * Composition locals exposing the richer token surface (status colors, thresholds, motion,
 * spacing) are added in the upcoming `GhsTheme` wrapper — kept here as a thin shim for the
 * existing `Main.kt` call site until P6 chrome polish swaps in the new composable.
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
    val scheme = colorSchemeFor(palette = appTheme.toTokenPalette(), mode = mode)
    MaterialExpressiveTheme(
        colorScheme = scheme,
        typography = getAppTypography(fontTheme),
        motionScheme = MotionScheme.expressive(),
        shapes = MaterialTheme.shapes,
        content = content,
    )
}
