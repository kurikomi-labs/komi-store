package zed.rainxch.core.presentation.theme.tokens

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Maps a [Tokens.PaletteColors] to a Material 3 [ColorScheme] so existing M3 components
 * (Button, Card, TextField, ...) keep working unchanged. Tokens not natively expressed
 * in M3 (status colors, custom motion, shape radii) are exposed via the composition
 * locals provided by GhsTheme — this mapper only covers M3 slots.
 *
 * Mapping rules:
 *   bg       → background
 *   surface  → surface / surfaceContainerLow / surfaceBright
 *   surface2 → surfaceVariant / surfaceContainer / surfaceContainerHigh
 *   ink      → onBackground / onSurface
 *   ink2     → onSurfaceVariant
 *   outline  → outline / outlineVariant
 *   primary  → primary (foreground = bg in light, ink in dark for contrast)
 *   tintP    → primaryContainer
 *   danger   → error
 *   dangerT  → errorContainer
 *   success* → exposed via LocalStatusColors only (M3 has no success slot)
 */
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

/** Resolves a [Tokens.Palette] + [Tokens.Mode] to its M3 [ColorScheme]. */
fun colorSchemeFor(palette: Tokens.Palette, mode: Tokens.Mode): ColorScheme {
    val tokens = Tokens.palette(palette, mode)
    return if (mode == Tokens.Mode.LIGHT) toLightColorScheme(tokens) else toDarkColorScheme(tokens)
}
