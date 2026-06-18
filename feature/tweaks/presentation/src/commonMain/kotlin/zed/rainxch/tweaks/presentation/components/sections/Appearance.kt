package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.appearance.AppTheme
import zed.rainxch.core.domain.model.appearance.ContentWidth
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.utils.displayName
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState

internal enum class ModeChoice { LIGHT, DARK, SYSTEM }

internal fun isDarkToChoice(value: Boolean?): ModeChoice = when (value) {
    true -> ModeChoice.DARK
    false -> ModeChoice.LIGHT
    null -> ModeChoice.SYSTEM
}

internal fun choiceToIsDark(choice: ModeChoice): Boolean? = when (choice) {
    ModeChoice.DARK -> true
    ModeChoice.LIGHT -> false
    ModeChoice.SYSTEM -> null
}

@Composable
internal fun ModeTiles(
    current: ModeChoice,
    paletteForPreview: AppTheme,
    onSelected: (ModeChoice) -> Unit,
) {
    val lightScheme = previewScheme(isDark = false)
    val darkScheme = previewScheme(isDark = true)
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
            .clip(RoundedCornerShape(LocalPersonality.current.shape.corner))
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
internal fun PaletteGrid(
    isDarkTheme: Boolean?,
    amoledEnabled: Boolean,
    selected: AppTheme,
    onSelected: (AppTheme) -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val resolvedDark = isDarkTheme ?: systemDark
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
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
            AppTheme.entries
                .filter { it != AppTheme.DYNAMIC }
                .forEach { palette ->
                    PaletteSwatch(
                        palette = palette,
                        isDark = resolvedDark,
                        isSelected = palette == selected,
                        onClick = { onSelected(palette) },
                    )
                }
        }
    }
}

private fun previewScheme(isDark: Boolean): ColorScheme =
    if (isDark) darkColorScheme() else lightColorScheme()

private val AppTheme.displayName: String
    @Composable
    get() = stringResource(
        when (this) {
            AppTheme.DYNAMIC -> Res.string.theme_dynamic
            AppTheme.NORD -> Res.string.theme_nord
            AppTheme.CREAM -> Res.string.theme_cream
            AppTheme.FOREST -> Res.string.theme_forest
            AppTheme.PLUM -> Res.string.theme_plum
        },
    )

@Composable
private fun PaletteSwatch(
    palette: AppTheme,
    isDark: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = previewScheme(isDark)
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
internal fun ContentWidthCard(
    selected: ContentWidth,
    onSelected: (ContentWidth) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
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
