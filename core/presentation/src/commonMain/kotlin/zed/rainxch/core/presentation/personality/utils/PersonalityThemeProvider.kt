package zed.rainxch.core.presentation.personality.utils

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.locals.LocalStatusColors
import zed.rainxch.core.presentation.personality.ClassicPersonality
import zed.rainxch.core.presentation.personality.MangaPersonality
import zed.rainxch.core.presentation.personality.Personality
import zed.rainxch.core.presentation.personality.classic.classicTypeWithFonts
import zed.rainxch.core.presentation.personality.manga.mangaTypeWithFonts
import zed.rainxch.core.presentation.personality.model.PersonalityColors
import zed.rainxch.core.presentation.status.statusColors

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PersonalityTheme(
    personality: Personality,
    content: @Composable () -> Unit,
) {
    val resolved =
        when (personality) {
            is MangaPersonality -> personality.copy(type = mangaTypeWithFonts(personality.type))
            is ClassicPersonality -> personality.copy(type = classicTypeWithFonts(personality.type))
        }
    val colorScheme = resolved.colors.toMaterialColorScheme()
    CompositionLocalProvider(
        LocalPersonality provides resolved,
        LocalStatusColors provides statusColors(resolved.colors.isDark),
    ) {
        when (resolved) {
            is ClassicPersonality -> MaterialExpressiveTheme(colorScheme = colorScheme, content = content)
            is MangaPersonality -> MaterialTheme(colorScheme = colorScheme, content = content)
        }
    }
}

private fun PersonalityColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = primary,
        onSecondary = onPrimary,
        secondaryContainer = primaryContainer,
        onSecondaryContainer = onPrimaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceContainerLowest = background,
        surfaceContainerLow = surface,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHigh,
        outline = outline,
        outlineVariant = outlineVariant,
        error = error,
        onError = onError,
        scrim = scrim,
    )
}
