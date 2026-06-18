package zed.rainxch.core.presentation.personality.classic

import androidx.compose.ui.graphics.Color
import zed.rainxch.core.presentation.personality.model.PersonalityColors

fun classicColors(dark: Boolean): PersonalityColors = if (dark) ClassicDark else ClassicLight

private val ClassicLight =
    PersonalityColors(
        primary = Color(0xFF3B5BDB),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDCE1FF),
        onPrimaryContainer = Color(0xFF00164E),
        background = Color(0xFFFBFBFD),
        onBackground = Color(0xFF1A1C1E),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = Color(0xFFEEF0F4),
        onSurfaceVariant = Color(0xFF44474E),
        surfaceContainer = Color(0xFFF3F4F8),
        surfaceContainerHigh = Color(0xFFECEEF3),
        outline = Color(0xFFC4C6CF),
        outlineVariant = Color(0xFFE2E2E6),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        shadow = Color(0xFF000000),
        scrim = Color(0xFF000000),
        screentoneOpacity = 0f,
        gridOpacity = 0f,
    )

private val ClassicDark =
    PersonalityColors(
        primary = Color(0xFFB6C4FF),
        onPrimary = Color(0xFF00257A),
        primaryContainer = Color(0xFF2C44A8),
        onPrimaryContainer = Color(0xFFDCE1FF),
        background = Color(0xFF111316),
        onBackground = Color(0xFFE3E2E6),
        surface = Color(0xFF1A1C1E),
        onSurface = Color(0xFFE3E2E6),
        surfaceVariant = Color(0xFF2A2D33),
        onSurfaceVariant = Color(0xFFC4C6CF),
        surfaceContainer = Color(0xFF1E2023),
        surfaceContainerHigh = Color(0xFF282A2E),
        outline = Color(0xFF8E9099),
        outlineVariant = Color(0xFF44474E),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        shadow = Color(0xFF000000),
        scrim = Color(0xFF000000),
        screentoneOpacity = 0f,
        gridOpacity = 0f,
    )
