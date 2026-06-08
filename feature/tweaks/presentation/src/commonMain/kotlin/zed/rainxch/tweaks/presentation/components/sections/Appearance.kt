package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.appearance.AppTheme
import zed.rainxch.core.domain.model.appearance.ContentWidth
import zed.rainxch.core.domain.model.appearance.FontTheme
import zed.rainxch.core.domain.model.system.Platform
import zed.rainxch.core.presentation.components.hub.GhsSectionHeader
import zed.rainxch.core.presentation.theme.dynamicColorScheme
import zed.rainxch.core.presentation.theme.isDynamicColorAvailable
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.theme.tokens.Tokens
import zed.rainxch.core.presentation.theme.tokens.colorSchemeFor
import zed.rainxch.core.presentation.utils.displayName
import zed.rainxch.core.presentation.utils.toTokenPalette
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.components.ToggleSettingCard

private enum class ModeChoice { LIGHT, DARK, SYSTEM }

private fun isDarkToChoice(value: Boolean?): ModeChoice = when (value) {
    true -> ModeChoice.DARK
    false -> ModeChoice.LIGHT
    null -> ModeChoice.SYSTEM
}

private fun choiceToIsDark(choice: ModeChoice): Boolean? = when (choice) {
    ModeChoice.DARK -> true
    ModeChoice.LIGHT -> false
    ModeChoice.SYSTEM -> null
}

@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.appearanceSection(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    item(key = "mode_header") {
        GhsSectionHeader(text = stringResource(Res.string.appearance_section_mode))
        Spacer(Modifier.height(8.dp))
    }
    item(key = "mode_tiles") {
        ModeTiles(
            current = isDarkToChoice(state.isDarkTheme),
            paletteForPreview = state.selectedThemeColor,
            onSelected = { onAction(TweaksAction.OnDarkThemeChange(choiceToIsDark(it))) },
        )
        Spacer(Modifier.height(16.dp))
    }
    item(key = "palette_header") {
        GhsSectionHeader(text = stringResource(Res.string.theme_color))
        Spacer(Modifier.height(8.dp))
    }
    item(key = "palette_grid") {
        PaletteGrid(
            isDarkTheme = state.isDarkTheme,
            amoledEnabled = state.isAmoledThemeEnabled,
            selected = state.selectedThemeColor,
            onSelected = { onAction(TweaksAction.OnThemeColorSelected(it)) },
        )
    }
    item(key = "amoled_toggle") {
        val systemDark = isSystemInDarkTheme()
        val resolvedDark = state.isDarkTheme ?: systemDark
        AnimatedVisibility(
            visible = resolvedDark,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                ToggleSettingCard(
                    title = stringResource(Res.string.amoled_black_theme),
                    description = stringResource(Res.string.amoled_black_description),
                    checked = state.isAmoledThemeEnabled,
                    onCheckedChange = { onAction(TweaksAction.OnAmoledThemeToggled(it)) },
                )
            }
        }
    }
    item(key = "display_header") {
        Spacer(Modifier.height(16.dp))
        GhsSectionHeader(text = stringResource(Res.string.appearance_section_display))
        Spacer(Modifier.height(8.dp))
    }
    item(key = "system_font") {
        ToggleSettingCard(
            title = stringResource(Res.string.system_font),
            description = stringResource(Res.string.system_font_description),
            checked = state.selectedFontTheme == FontTheme.SYSTEM,
            onCheckedChange = { enabled ->
                onAction(
                    TweaksAction.OnFontThemeSelected(
                        if (enabled) FontTheme.SYSTEM else FontTheme.CUSTOM,
                    ),
                )
            },
        )
    }
    if (getPlatform() != Platform.ANDROID) {
        item(key = "scrollbar") {
            Spacer(Modifier.height(8.dp))
            ToggleSettingCard(
                title = stringResource(Res.string.scrollbar_option_title),
                description = stringResource(Res.string.scrollbar_option_description),
                checked = state.isScrollbarEnabled,
                onCheckedChange = { onAction(TweaksAction.OnScrollbarToggled(it)) },
            )
        }
        item(key = "content_width") {
            Spacer(Modifier.height(8.dp))
            ContentWidthCard(
                selected = state.contentWidth,
                onSelected = { onAction(TweaksAction.OnContentWidthSelected(it)) },
            )
        }
    }
}

@Composable
private fun ModeTiles(
    current: ModeChoice,
    paletteForPreview: AppTheme,
    onSelected: (ModeChoice) -> Unit,
) {
    val token = paletteForPreview.toTokenPalette()
    val lightScheme = colorSchemeFor(token, Tokens.Mode.LIGHT)
    val darkScheme = colorSchemeFor(token, Tokens.Mode.DARK)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ModeTile(
            label = stringResource(Res.string.theme_light),
            selected = current == ModeChoice.LIGHT,
            preview = { ThemePreviewCanvas(scheme = lightScheme) },
            onClick = { onSelected(ModeChoice.LIGHT) },
            modifier = Modifier.weight(1f),
        )
        ModeTile(
            label = stringResource(Res.string.theme_dark),
            selected = current == ModeChoice.DARK,
            preview = { ThemePreviewCanvas(scheme = darkScheme) },
            onClick = { onSelected(ModeChoice.DARK) },
            modifier = Modifier.weight(1f),
        )
        ModeTile(
            label = stringResource(Res.string.theme_system),
            selected = current == ModeChoice.SYSTEM,
            preview = {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        ThemePreviewCanvas(scheme = lightScheme, edgeFade = true)
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        ThemePreviewCanvas(scheme = darkScheme, edgeFade = true)
                    }
                }
            },
            onClick = { onSelected(ModeChoice.SYSTEM) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModeTile(
    label: String,
    selected: Boolean,
    preview: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(durationMillis = 220),
        label = "mode_border",
    )
    val borderWidth by animateFloatAsState(
        targetValue = if (selected) 2f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "mode_border_w",
    )
    Column(
        modifier = modifier
            .clip(Radii.row)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    width = borderWidth.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            preview()
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ThemePreviewCanvas(
    scheme: androidx.compose.material3.ColorScheme,
    edgeFade: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(scheme.onSurface.copy(alpha = if (edgeFade) 0.55f else 0.85f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(scheme.surfaceContainerHigh)
                    .padding(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(scheme.primary),
                    )
                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .fillMaxWidth(0.5f)
                            .clip(RoundedCornerShape(50))
                            .background(scheme.onSurfaceVariant.copy(alpha = 0.6f)),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(scheme.primary),
                )
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .width(28.dp)
                        .clip(RoundedCornerShape(50))
                        .background(scheme.secondaryContainer),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaletteGrid(
    isDarkTheme: Boolean?,
    amoledEnabled: Boolean,
    selected: AppTheme,
    onSelected: (AppTheme) -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val resolvedDark = isDarkTheme ?: systemDark
    val previewMode = when {
        resolvedDark && amoledEnabled -> Tokens.Mode.AMOLED
        resolvedDark -> Tokens.Mode.DARK
        else -> Tokens.Mode.LIGHT
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val dynamicAvailable = isDynamicColorAvailable()
            AppTheme.entries
                .filter { it != AppTheme.DYNAMIC || dynamicAvailable }
                .forEach { palette ->
                    PaletteSwatch(
                        palette = palette,
                        mode = previewMode,
                        isSelected = palette == selected,
                        onClick = { onSelected(palette) },
                    )
                }
        }
    }
}

@Composable
private fun PaletteSwatch(
    palette: AppTheme,
    mode: Tokens.Mode,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isDark = mode != Tokens.Mode.LIGHT
    val scheme = if (palette == AppTheme.DYNAMIC) {
        dynamicColorScheme(isDark = isDark) ?: colorSchemeFor(Tokens.Palette.NORD, mode)
    } else {
        colorSchemeFor(palette.toTokenPalette(), mode)
    }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.0f else 0.96f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "swatch_scale",
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        },
        animationSpec = tween(durationMillis = 220),
        label = "swatch_border",
    )
    val borderWidth by animateFloatAsState(
        targetValue = if (isSelected) 2f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "swatch_border_w",
    )
    Column(
        modifier = Modifier
            .width(82.dp)
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 82.dp, height = 78.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.background)
                .border(
                    width = borderWidth.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(16.dp),
                ),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(scheme.primary),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .fillMaxWidth(0.75f)
                                .clip(RoundedCornerShape(50))
                                .background(scheme.onSurface.copy(alpha = 0.55f)),
                        )
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .fillMaxWidth(0.45f)
                                .clip(RoundedCornerShape(50))
                                .background(scheme.onSurface.copy(alpha = 0.32f)),
                        )
                        Spacer(Modifier.height(1.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(scheme.secondary),
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(scheme.tertiary),
                            )
                        }
                    }
                }
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        }
        Text(
            text = palette.displayName,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            ),
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

@Composable
private fun ContentWidthCard(
    selected: ContentWidth,
    onSelected: (ContentWidth) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.content_width_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.content_width_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ContentWidth.entries.forEach { width ->
                    val isSelected = width == selected
                    val container by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        animationSpec = tween(durationMillis = 220),
                        label = "cw_container",
                    )
                    val content by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        animationSpec = tween(durationMillis = 220),
                        label = "cw_content",
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(container)
                            .clickable { onSelected(width) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = width.displayName,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = content,
                        )
                    }
                }
            }
        }
    }
}
