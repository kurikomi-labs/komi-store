package zed.rainxch.core.presentation.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@Composable
fun animateColorScheme(
    target: ColorScheme,
    spec: AnimationSpec<Color> = tween(durationMillis = 420),
    label: String = "color_scheme",
): ColorScheme {
    val primary by animateColorAsState(target.primary, spec, label = "$label.primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "$label.onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "$label.primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "$label.onPrimaryContainer")
    val inversePrimary by animateColorAsState(target.inversePrimary, spec, label = "$label.inversePrimary")
    val secondary by animateColorAsState(target.secondary, spec, label = "$label.secondary")
    val onSecondary by animateColorAsState(target.onSecondary, spec, label = "$label.onSecondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, spec, label = "$label.secondaryContainer")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, spec, label = "$label.onSecondaryContainer")
    val tertiary by animateColorAsState(target.tertiary, spec, label = "$label.tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, spec, label = "$label.onTertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, spec, label = "$label.tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, spec, label = "$label.onTertiaryContainer")
    val background by animateColorAsState(target.background, spec, label = "$label.background")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "$label.onBackground")
    val surface by animateColorAsState(target.surface, spec, label = "$label.surface")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "$label.onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "$label.surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "$label.onSurfaceVariant")
    val surfaceTint by animateColorAsState(target.surfaceTint, spec, label = "$label.surfaceTint")
    val inverseSurface by animateColorAsState(target.inverseSurface, spec, label = "$label.inverseSurface")
    val inverseOnSurface by animateColorAsState(target.inverseOnSurface, spec, label = "$label.inverseOnSurface")
    val error by animateColorAsState(target.error, spec, label = "$label.error")
    val onError by animateColorAsState(target.onError, spec, label = "$label.onError")
    val errorContainer by animateColorAsState(target.errorContainer, spec, label = "$label.errorContainer")
    val onErrorContainer by animateColorAsState(target.onErrorContainer, spec, label = "$label.onErrorContainer")
    val outline by animateColorAsState(target.outline, spec, label = "$label.outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, spec, label = "$label.outlineVariant")
    val scrim by animateColorAsState(target.scrim, spec, label = "$label.scrim")
    val surfaceBright by animateColorAsState(target.surfaceBright, spec, label = "$label.surfaceBright")
    val surfaceDim by animateColorAsState(target.surfaceDim, spec, label = "$label.surfaceDim")
    val surfaceContainer by animateColorAsState(target.surfaceContainer, spec, label = "$label.surfaceContainer")
    val surfaceContainerHigh by animateColorAsState(target.surfaceContainerHigh, spec, label = "$label.surfaceContainerHigh")
    val surfaceContainerHighest by animateColorAsState(target.surfaceContainerHighest, spec, label = "$label.surfaceContainerHighest")
    val surfaceContainerLow by animateColorAsState(target.surfaceContainerLow, spec, label = "$label.surfaceContainerLow")
    val surfaceContainerLowest by animateColorAsState(target.surfaceContainerLowest, spec, label = "$label.surfaceContainerLowest")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        inversePrimary = inversePrimary,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = surfaceTint,
        inverseSurface = inverseSurface,
        inverseOnSurface = inverseOnSurface,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        scrim = scrim,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
    )
}
