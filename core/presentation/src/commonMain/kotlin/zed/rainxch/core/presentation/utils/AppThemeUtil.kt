package zed.rainxch.core.presentation.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.presentation.theme.tokens.Tokens
import zed.rainxch.core.presentation.theme.tokens.colorSchemeFor
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.theme_cream
import zed.rainxch.githubstore.core.presentation.res.theme_forest
import zed.rainxch.githubstore.core.presentation.res.theme_nord
import zed.rainxch.githubstore.core.presentation.res.theme_plum

fun AppTheme.toTokenPalette(): Tokens.Palette = when (this) {
    AppTheme.NORD -> Tokens.Palette.NORD
    AppTheme.CREAM -> Tokens.Palette.CREAM
    AppTheme.FOREST -> Tokens.Palette.FOREST
    AppTheme.PLUM -> Tokens.Palette.PLUM
}

val AppTheme.lightScheme: ColorScheme
    get() = colorSchemeFor(toTokenPalette(), Tokens.Mode.LIGHT)

val AppTheme.darkScheme: ColorScheme
    get() = colorSchemeFor(toTokenPalette(), Tokens.Mode.DARK)

val AppTheme.amoledScheme: ColorScheme
    get() = colorSchemeFor(toTokenPalette(), Tokens.Mode.AMOLED)

val AppTheme.primaryColor: Color
    get() = Tokens.palette(toTokenPalette(), Tokens.Mode.LIGHT).primary

val AppTheme.displayName: String
    @Composable
    get() = stringResource(
        when (this) {
            AppTheme.NORD -> Res.string.theme_nord
            AppTheme.CREAM -> Res.string.theme_cream
            AppTheme.FOREST -> Res.string.theme_forest
            AppTheme.PLUM -> Res.string.theme_plum
        },
    )
