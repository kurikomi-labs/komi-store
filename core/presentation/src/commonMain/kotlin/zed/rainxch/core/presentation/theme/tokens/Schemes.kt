package zed.rainxch.core.presentation.theme.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

fun toLightColorScheme(p: Tokens.PaletteColors): ColorScheme = lightColorScheme(
    primary = p.primary,
    onPrimary = Color.White,
    primaryContainer = p.tintP,
    onPrimaryContainer = p.ink,
    secondary = p.primary,
    onSecondary = Color.White,
    secondaryContainer = p.tintP,
    onSecondaryContainer = p.ink,
    tertiary = p.success,
    onTertiary = Color.White,
    tertiaryContainer = p.successT,
    onTertiaryContainer = p.ink,
    error = p.danger,
    onError = Color.White,
    errorContainer = p.dangerT,
    onErrorContainer = p.ink,
    background = p.bg,
    onBackground = p.ink,
    surface = p.surface,
    onSurface = p.ink,
    surfaceVariant = p.surface2,
    onSurfaceVariant = p.ink2,
    outline = p.outline,
    outlineVariant = p.outline,
    scrim = Color.Black,
    inverseSurface = p.ink,
    inverseOnSurface = p.bg,
    inversePrimary = p.tintP,
    surfaceTint = p.primary,
    surfaceBright = p.surface,
    surfaceDim = p.bg,
    surfaceContainerLowest = p.surface,
    surfaceContainerLow = p.surface,
    surfaceContainer = p.surface2,
    surfaceContainerHigh = p.surface2,
    surfaceContainerHighest = p.surface2,
)

fun toDarkColorScheme(p: Tokens.PaletteColors): ColorScheme = darkColorScheme(
    primary = p.primary,
    onPrimary = p.ink,
    primaryContainer = p.tintP,
    onPrimaryContainer = p.ink,
    secondary = p.primary,
    onSecondary = p.ink,
    secondaryContainer = p.tintP,
    onSecondaryContainer = p.ink,
    tertiary = p.success,
    onTertiary = p.ink,
    tertiaryContainer = p.successT,
    onTertiaryContainer = p.ink,
    error = p.danger,
    onError = Color.White,
    errorContainer = p.dangerT,
    onErrorContainer = p.ink,
    background = p.bg,
    onBackground = p.ink,
    surface = p.surface,
    onSurface = p.ink,
    surfaceVariant = p.surface2,
    onSurfaceVariant = p.ink2,
    outline = p.outline,
    outlineVariant = p.outline,
    scrim = Color.Black,
    inverseSurface = p.ink,
    inverseOnSurface = p.bg,
    inversePrimary = p.tintP,
    surfaceTint = p.primary,
    surfaceBright = p.surface2,
    surfaceDim = p.bg,
    surfaceContainerLowest = p.bg,
    surfaceContainerLow = p.surface,
    surfaceContainer = p.surface,
    surfaceContainerHigh = p.surface2,
    surfaceContainerHighest = p.surface2,
)

fun colorSchemeFor(palette: Tokens.Palette, mode: Tokens.Mode): ColorScheme {
    val tokens = Tokens.palette(palette, mode)
    return if (mode == Tokens.Mode.LIGHT) toLightColorScheme(tokens) else toDarkColorScheme(tokens)
}
