package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.AppTheme
import zed.rainxch.core.domain.model.ContentWidth
import zed.rainxch.core.domain.model.FontTheme
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.theme.tokens.Tokens
import zed.rainxch.core.presentation.theme.tokens.colorSchemeFor
import zed.rainxch.core.presentation.utils.displayName
import zed.rainxch.core.presentation.utils.toTokenPalette
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.components.ToggleSettingCard

fun LazyListScope.appearanceSection(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    item {
        ThemePickerCard(
            isDarkTheme = state.isDarkTheme,
            selectedPalette = state.selectedThemeColor,
            amoledEnabled = state.isAmoledThemeEnabled,
            onDarkThemeChange = { onAction(TweaksAction.OnDarkThemeChange(it)) },
            onPaletteSelected = { onAction(TweaksAction.OnThemeColorSelected(it)) },
            onAmoledToggled = { onAction(TweaksAction.OnAmoledThemeToggled(it)) },
        )

        Spacer(Modifier.height(12.dp))

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

        if (getPlatform() != Platform.ANDROID) {
            Spacer(Modifier.height(8.dp))
            ToggleSettingCard(
                title = stringResource(Res.string.scrollbar_option_title),
                description = stringResource(Res.string.scrollbar_option_description),
                checked = state.isScrollbarEnabled,
                onCheckedChange = { onAction(TweaksAction.OnScrollbarToggled(it)) },
            )
            Spacer(Modifier.height(8.dp))
            ContentWidthCard(
                selected = state.contentWidth,
                onSelected = { onAction(TweaksAction.OnContentWidthSelected(it)) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemePickerCard(
    isDarkTheme: Boolean?,
    selectedPalette: AppTheme,
    amoledEnabled: Boolean,
    onDarkThemeChange: (Boolean?) -> Unit,
    onPaletteSelected: (AppTheme) -> Unit,
    onAmoledToggled: (Boolean) -> Unit,
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
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.theme_color),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.height(12.dp))

            // Mode segment
            ModeSegment(
                isDarkTheme = isDarkTheme,
                onChange = onDarkThemeChange,
            )

            Spacer(Modifier.height(16.dp))

            // Palette grid
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AppTheme.entries.forEach { palette ->
                    PaletteSwatch(
                        palette = palette,
                        mode = previewMode,
                        selected = palette == selectedPalette,
                        onClick = { onPaletteSelected(palette) },
                    )
                }
            }

            AnimatedVisibility(
                visible = resolvedDark,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(14.dp))
                    InlineToggleRow(
                        title = stringResource(Res.string.amoled_black_theme),
                        description = stringResource(Res.string.amoled_black_description),
                        checked = amoledEnabled,
                        onCheckedChange = onAmoledToggled,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeSegment(
    isDarkTheme: Boolean?,
    onChange: (Boolean?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentItem(
            icon = Icons.Default.LightMode,
            label = stringResource(Res.string.theme_light),
            selected = isDarkTheme == false,
            onClick = { onChange(false) },
            modifier = Modifier.weight(1f),
        )
        SegmentItem(
            icon = Icons.Default.DarkMode,
            label = stringResource(Res.string.theme_dark),
            selected = isDarkTheme == true,
            onClick = { onChange(true) },
            modifier = Modifier.weight(1f),
        )
        SegmentItem(
            icon = Icons.Outlined.SettingsBrightness,
            label = stringResource(Res.string.theme_system),
            selected = isDarkTheme == null,
            onClick = { onChange(null) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container =
        if (selected) MaterialTheme.colorScheme.primary
        else Color.Transparent
    val content =
        if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = content,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = content,
            )
        }
    }
}

@Composable
private fun PaletteSwatch(
    palette: AppTheme,
    mode: Tokens.Mode,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = colorSchemeFor(palette.toTokenPalette(), mode)
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.0f else 0.98f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "swatch_scale",
    )
    Column(
        modifier = Modifier
            .width(74.dp)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(width = 74.dp, height = 56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.background)
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    },
                    shape = RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            // Mini palette preview: primary blob + secondary dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 22.dp, height = 22.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(scheme.primary),
                )
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(scheme.secondary),
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(scheme.tertiary),
                )
            }
            if (selected) {
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
private fun InlineToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
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
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
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
                    val container =
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    val content =
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
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
