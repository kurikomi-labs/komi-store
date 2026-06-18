package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.custom_forges_count
import zed.rainxch.githubstore.core.presentation.res.custom_forges_entry_label
import zed.rainxch.githubstore.core.presentation.res.platform_section_android
import zed.rainxch.githubstore.core.presentation.res.platform_section_linux
import zed.rainxch.githubstore.core.presentation.res.platform_section_macos
import zed.rainxch.githubstore.core.presentation.res.platform_section_windows
import zed.rainxch.githubstore.core.presentation.res.remove_search_history_item
import zed.rainxch.githubstore.core.presentation.res.tweaks_sources_add_a_host
import zed.rainxch.githubstore.core.presentation.res.tweaks_sources_added_hosts_section
import zed.rainxch.githubstore.core.presentation.res.tweaks_sources_github_mirror_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_sources_intro_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_sources_intro_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_sources_mirror_default
import zed.rainxch.githubstore.core.presentation.res.tweaks_sources_platforms_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_sources_platforms_title
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.components.CustomForgesDialog

@Composable
fun sourcesSectionContent(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToMirrorPicker: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.tweaks_sources_intro_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.tweaks_sources_intro_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        DrillRow(
            icon = Icons.Outlined.NetworkCheck,
            title = stringResource(Res.string.tweaks_sources_github_mirror_title),
            subtitle = stringResource(Res.string.tweaks_sources_mirror_default),
            accent = MaterialTheme.colorScheme.primary,
            onClick = onNavigateToMirrorPicker,
        )
        Spacer(Modifier.height(8.dp))

        run {
            val count = state.customForgeHosts.size
            DrillRow(
                icon = Icons.Outlined.Dns,
                title = stringResource(Res.string.custom_forges_entry_label),
                subtitle = if (count == 0) {
                    stringResource(Res.string.tweaks_sources_add_a_host)
                } else {
                    pluralStringResource(Res.plurals.custom_forges_count, count, count)
                },
                accent = MaterialTheme.colorScheme.primary,
                onClick = { onAction(TweaksAction.OnOpenCustomForgesDialog) },
            )
        }
        Spacer(Modifier.height(16.dp))

        DiscoveryPlatformsCard(
            selected = state.selectedDiscoveryPlatforms,
            onToggle = { onAction(TweaksAction.OnDiscoveryPlatformToggled(it)) },
        )

        if (state.customForgeHosts.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.tweaks_sources_added_hosts_section),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
            )

            state.customForgeHosts.sorted().forEach { host ->
                ForgeHostRow(
                    host = host,
                    onRemove = { onAction(TweaksAction.OnRemoveCustomForge(host)) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (state.showCustomForgesDialog) {
        CustomForgesDialog(
            state = state,
            onAction = { onAction(it) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiscoveryPlatformsCard(
    selected: Set<DiscoveryPlatform>,
    onToggle: (DiscoveryPlatform) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.tweaks_sources_platforms_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.tweaks_sources_platforms_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DiscoveryPlatform.selectablePlatforms.forEach { platform ->
                    PlatformChip(
                        label = stringResource(platform.labelRes()),
                        isSelected = platform in selected,
                        onClick = { onToggle(platform) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlatformChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(LocalPersonality.current.shape.cornerSmall))
            .background(container)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = content,
        )
    }
}

private fun DiscoveryPlatform.labelRes() = when (this) {
    DiscoveryPlatform.Android -> Res.string.platform_section_android
    DiscoveryPlatform.Macos -> Res.string.platform_section_macos
    DiscoveryPlatform.Windows -> Res.string.platform_section_windows
    DiscoveryPlatform.Linux -> Res.string.platform_section_linux
    DiscoveryPlatform.All -> Res.string.platform_section_android
}

@Composable
private fun DrillRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    accent: Color = Color.Unspecified,
) {
    val tileBg = if (accent == Color.Unspecified) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        accent.copy(alpha = 0.14f)
    }
    val tint = if (accent == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        accent
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocalPersonality.current.shape.corner))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(LocalPersonality.current.shape.cornerSmall))
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun ForgeHostRow(
    host: String,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = host,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(LocalPersonality.current.shape.cornerSmall)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(Res.string.remove_search_history_item),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
