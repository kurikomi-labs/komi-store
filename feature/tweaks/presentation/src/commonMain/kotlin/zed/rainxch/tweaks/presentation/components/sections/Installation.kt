package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.getPlatform
import zed.rainxch.core.domain.model.DhizukuAvailability
import zed.rainxch.core.domain.model.InstallerType
import zed.rainxch.core.domain.model.Platform
import zed.rainxch.core.domain.model.RootAvailability
import zed.rainxch.core.domain.model.ShizukuAvailability
import zed.rainxch.core.presentation.components.ExpressiveCard
import zed.rainxch.core.presentation.components.inputs.GhsTextField
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState
import zed.rainxch.tweaks.presentation.components.SectionHeader

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.installationSection(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    if (getPlatform() != Platform.ANDROID) return

    item {
        InstallerTypeCard(
            selectedType = state.installerType,
            shizukuAvailability = state.shizukuAvailability,
            dhizukuAvailability = state.dhizukuAvailability,
            rootAvailability = state.rootAvailability,
            onTypeSelected = { type ->
                onAction(TweaksAction.OnInstallerTypeSelected(type))
            },
            onRequestShizukuPermission = {
                onAction(TweaksAction.OnRequestShizukuPermission)
            },
            onRequestDhizukuPermission = {
                onAction(TweaksAction.OnRequestDhizukuPermission)
            },
            onRequestRootPermission = {
                onAction(TweaksAction.OnRequestRootPermission)
            },
        )

        val silentReady = (
            state.installerType == InstallerType.SHIZUKU &&
                state.shizukuAvailability == ShizukuAvailability.READY
            ) || (
            state.installerType == InstallerType.DHIZUKU &&
                state.dhizukuAvailability == DhizukuAvailability.READY
            ) || (
            state.installerType == InstallerType.ROOT &&
                state.rootAvailability == RootAvailability.READY
            )
        if (silentReady) {
            Spacer(Modifier.height(12.dp))

            AutoUpdateCard(
                enabled = state.autoUpdateEnabled,
                onToggle = { enabled ->
                    onAction(TweaksAction.OnAutoUpdateToggled(enabled))
                }
            )

            Spacer(Modifier.height(12.dp))

            InstallerAttributionCard(state = state, onAction = onAction)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InstallerAttributionCard(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    val attribution = state.installerAttribution
    ExpressiveCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.installer_attribution_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.installer_attribution_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))

            AttributionRadioRow(
                title = stringResource(Res.string.installer_attribution_preset_system),
                selected = attribution is zed.rainxch.core.domain.model.InstallerAttribution.SystemDefault,
                onClick = { onAction(TweaksAction.OnInstallerAttributionSystemDefault) },
            )
            AttributionRadioRow(
                title = stringResource(Res.string.installer_attribution_preset_playstore),
                caption = "com.android.vending",
                selected = (attribution as? zed.rainxch.core.domain.model.InstallerAttribution.Preset)?.key
                    == zed.rainxch.core.domain.model.PresetKey.PLAY_STORE,
                onClick = {
                    onAction(
                        TweaksAction.OnInstallerAttributionPresetSelected(
                            zed.rainxch.core.domain.model.PresetKey.PLAY_STORE,
                        ),
                    )
                },
            )
            AttributionRadioRow(
                title = stringResource(Res.string.installer_attribution_preset_fdroid),
                caption = "org.fdroid.fdroid",
                selected = (attribution as? zed.rainxch.core.domain.model.InstallerAttribution.Preset)?.key
                    == zed.rainxch.core.domain.model.PresetKey.FDROID,
                onClick = {
                    onAction(
                        TweaksAction.OnInstallerAttributionPresetSelected(
                            zed.rainxch.core.domain.model.PresetKey.FDROID,
                        ),
                    )
                },
            )
            AttributionRadioRow(
                title = stringResource(Res.string.installer_attribution_preset_obtainium),
                caption = "dev.imranr.obtainium.app",
                selected = (attribution as? zed.rainxch.core.domain.model.InstallerAttribution.Preset)?.key
                    == zed.rainxch.core.domain.model.PresetKey.OBTAINIUM,
                onClick = {
                    onAction(
                        TweaksAction.OnInstallerAttributionPresetSelected(
                            zed.rainxch.core.domain.model.PresetKey.OBTAINIUM,
                        ),
                    )
                },
            )
            AttributionRadioRow(
                title = stringResource(Res.string.installer_attribution_preset_custom),
                caption = (attribution as? zed.rainxch.core.domain.model.InstallerAttribution.Custom)
                    ?.packageName,
                selected = attribution is zed.rainxch.core.domain.model.InstallerAttribution.Custom,
                onClick = { onAction(TweaksAction.OnInstallerAttributionCustomToggleExpanded) },
            )

            if (state.installerAttributionCustomExpanded ||
                attribution is zed.rainxch.core.domain.model.InstallerAttribution.Custom
            ) {
                CustomInstallerEditor(state = state, onAction = onAction)
            }

            Spacer(Modifier.height(4.dp))
            HintText(text = stringResource(Res.string.installer_attribution_disclosure))
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CustomInstallerEditor(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val customSupporting = state.installerAttributionCustomError?.let {
            stringResource(Res.string.installer_attribution_custom_error)
        }
        GhsTextField(
            value = state.installerAttributionCustomDraft,
            onValueChange = { onAction(TweaksAction.OnInstallerAttributionCustomChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(Res.string.installer_attribution_custom_label),
            placeholder = "com.example.installer",
            singleLine = true,
            isError = state.installerAttributionCustomError != null,
            supportingText = customSupporting,
        )
        FilledTonalButton(
            onClick = { onAction(TweaksAction.OnInstallerAttributionCustomSave) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.installer_attribution_custom_apply),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AttributionRadioRow(
    title: String,
    caption: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        androidx.compose.material3.RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!caption.isNullOrBlank()) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun LazyListScope.updatesSection(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
) {
    if (getPlatform() != Platform.ANDROID) return

    item {
        if (state.showBatteryOptimizationCard) {
            BatteryOptimizationCard(
                onOpenSettings = {
                    onAction(TweaksAction.OnOpenBatteryOptimizationSettings)
                },
                onDismiss = {
                    onAction(TweaksAction.OnDismissBatteryOptimizationCard)
                },
            )
            Spacer(Modifier.height(12.dp))
        }

        BackgroundUpdateCheckToggleCard(
            enabled = state.updateCheckEnabled,
            onToggle = { enabled ->
                onAction(TweaksAction.OnUpdateCheckEnabledToggled(enabled))
            }
        )

        Spacer(Modifier.height(12.dp))

        UpdateCheckIntervalCard(
            selectedIntervalHours = state.updateCheckIntervalHours,
            enabled = state.updateCheckEnabled,
            onIntervalSelected = { hours ->
                onAction(TweaksAction.OnUpdateCheckIntervalChanged(hours))
            }
        )

        Spacer(Modifier.height(12.dp))

        PreReleaseToggleCard(
            enabled = state.includePreReleases,
            onToggle = { enabled ->
                onAction(TweaksAction.OnIncludePreReleasesToggled(enabled))
            }
        )

        Spacer(Modifier.height(12.dp))

        SkippedUpdatesEntryCard(
            onClick = { onAction(TweaksAction.OnSkippedUpdatesClick) },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HiddenRepositoriesEntryCard(
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.hidden_repositories_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.hidden_repositories_entry_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SkippedUpdatesEntryCard(
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        colors =
            CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
        shape = RoundedCornerShape(32.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.skipped_updates_entry_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(Res.string.skipped_updates_entry_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BackgroundUpdateCheckToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    ExpressiveCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(Res.string.update_check_enabled_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(Res.string.update_check_enabled_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InstallerTypeCard(
    selectedType: InstallerType,
    shizukuAvailability: ShizukuAvailability,
    dhizukuAvailability: DhizukuAvailability,
    rootAvailability: RootAvailability,
    onTypeSelected: (InstallerType) -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onRequestDhizukuPermission: () -> Unit,
    onRequestRootPermission: () -> Unit,
) {
    ExpressiveCard {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InstallerOption(
                icon = Icons.Outlined.InstallMobile,
                title = stringResource(Res.string.installer_type_default),
                description = stringResource(Res.string.installer_type_default_description),
                isSelected = selectedType == InstallerType.DEFAULT,
                onClick = { onTypeSelected(InstallerType.DEFAULT) }
            )

            InstallerOption(
                icon = Icons.Outlined.Speed,
                title = stringResource(Res.string.installer_type_shizuku),
                description = stringResource(Res.string.installer_type_shizuku_description),
                isSelected = selectedType == InstallerType.SHIZUKU,
                onClick = { onTypeSelected(InstallerType.SHIZUKU) },
                statusBadge = {
                    ShizukuStatusBadge(
                        availability = shizukuAvailability
                    )
                }
            )

            if (selectedType == InstallerType.SHIZUKU) {
                ShizukuStatusActions(
                    availability = shizukuAvailability,
                    onRequestPermission = onRequestShizukuPermission
                )
            }

            InstallerOption(
                icon = Icons.Outlined.Security,
                title = stringResource(Res.string.installer_type_dhizuku),
                description = stringResource(Res.string.installer_type_dhizuku_description),
                isSelected = selectedType == InstallerType.DHIZUKU,
                onClick = { onTypeSelected(InstallerType.DHIZUKU) },
                statusBadge = {
                    DhizukuStatusBadge(
                        availability = dhizukuAvailability
                    )
                }
            )

            if (selectedType == InstallerType.DHIZUKU) {
                DhizukuStatusActions(
                    availability = dhizukuAvailability,
                    onRequestPermission = onRequestDhizukuPermission
                )
            }

            InstallerOption(
                icon = Icons.Outlined.AdminPanelSettings,
                title = stringResource(Res.string.installer_type_root),
                description = stringResource(Res.string.installer_type_root_description),
                isSelected = selectedType == InstallerType.ROOT,
                onClick = { onTypeSelected(InstallerType.ROOT) },
                statusBadge = { RootStatusBadge(availability = rootAvailability) },
            )

            if (selectedType == InstallerType.ROOT) {
                RootStatusActions(
                    availability = rootAvailability,
                    onRequestPermission = onRequestRootPermission,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RootStatusActions(
    availability: RootAvailability,
    onRequestPermission: () -> Unit,
) {
    when (availability) {
        RootAvailability.PERMISSION_NEEDED -> {
            FilledTonalButton(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(Res.string.root_grant_permission),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        RootAvailability.UNAVAILABLE -> {
            HintText(text = stringResource(Res.string.root_unavailable_hint))
        }
        RootAvailability.READY -> Unit
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RootStatusBadge(availability: RootAvailability) {
    val (color, label) = when (availability) {
        RootAvailability.READY -> Pair(
            Color(0xFF4CAF50),
            stringResource(Res.string.root_status_ready),
        )
        RootAvailability.PERMISSION_NEEDED -> Pair(
            Color(0xFFFF9800),
            stringResource(Res.string.root_status_permission_needed),
        )
        RootAvailability.UNAVAILABLE -> Pair(
            MaterialTheme.colorScheme.outline,
            stringResource(Res.string.root_status_unavailable),
        )
    }
    StatusDot(color = color, label = label)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShizukuStatusActions(
    availability: ShizukuAvailability,
    onRequestPermission: () -> Unit
) {
    when (availability) {
        ShizukuAvailability.PERMISSION_NEEDED -> {
            FilledTonalButton(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.shizuku_grant_permission),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        ShizukuAvailability.UNAVAILABLE -> {
            HintText(text = stringResource(Res.string.shizuku_install_hint))
        }
        ShizukuAvailability.NOT_RUNNING -> {
            HintText(text = stringResource(Res.string.shizuku_start_hint))
        }
        ShizukuAvailability.READY -> Unit
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DhizukuStatusActions(
    availability: DhizukuAvailability,
    onRequestPermission: () -> Unit
) {
    when (availability) {
        DhizukuAvailability.PERMISSION_NEEDED -> {
            FilledTonalButton(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.dhizuku_grant_permission),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        DhizukuAvailability.UNAVAILABLE -> {
            HintText(text = stringResource(Res.string.dhizuku_install_hint))
        }
        DhizukuAvailability.NOT_RUNNING -> {
            HintText(text = stringResource(Res.string.dhizuku_start_hint))
        }
        DhizukuAvailability.READY -> Unit
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InstallerOption(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    statusBadge: (@Composable () -> Unit)? = null
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            )
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                )
                .padding(8.dp),
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }

        if (statusBadge != null) {
            statusBadge()
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ShizukuStatusBadge(
    availability: ShizukuAvailability
) {
    val (color, label) = when (availability) {
        ShizukuAvailability.READY -> Pair(
            Color(0xFF4CAF50),
            stringResource(Res.string.shizuku_status_ready)
        )

        ShizukuAvailability.PERMISSION_NEEDED -> Pair(
            Color(0xFFFF9800),
            stringResource(Res.string.shizuku_status_permission_needed)
        )

        ShizukuAvailability.NOT_RUNNING -> Pair(
            Color(0xFFFF5722),
            stringResource(Res.string.shizuku_status_not_running)
        )

        ShizukuAvailability.UNAVAILABLE -> Pair(
            MaterialTheme.colorScheme.outline,
            stringResource(Res.string.shizuku_status_not_installed)
        )
    }

    StatusDot(color = color, label = label)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DhizukuStatusBadge(
    availability: DhizukuAvailability
) {
    val (color, label) = when (availability) {
        DhizukuAvailability.READY -> Pair(
            Color(0xFF4CAF50),
            stringResource(Res.string.dhizuku_status_ready)
        )

        DhizukuAvailability.PERMISSION_NEEDED -> Pair(
            Color(0xFFFF9800),
            stringResource(Res.string.dhizuku_status_permission_needed)
        )

        DhizukuAvailability.NOT_RUNNING -> Pair(
            Color(0xFFFF5722),
            stringResource(Res.string.dhizuku_status_not_running)
        )

        DhizukuAvailability.UNAVAILABLE -> Pair(
            MaterialTheme.colorScheme.outline,
            stringResource(Res.string.dhizuku_status_not_installed)
        )
    }

    StatusDot(color = color, label = label)
}

@Composable
private fun StatusDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AutoUpdateCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    ExpressiveCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(Res.string.auto_update_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(Res.string.auto_update_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateCheckIntervalCard(
    selectedIntervalHours: Long,
    enabled: Boolean,
    onIntervalSelected: (Long) -> Unit,
) {
    val intervals = listOf(
        3L to Res.string.interval_3h,
        6L to Res.string.interval_6h,
        12L to Res.string.interval_12h,
        24L to Res.string.interval_24h,
    )

    ExpressiveCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.update_check_interval_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(Res.string.update_check_interval_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                intervals.forEach { (hours, labelRes) ->
                    val isSelected = selectedIntervalHours == hours

                    FilterChip(
                        selected = isSelected,
                        enabled = enabled,
                        onClick = { onIntervalSelected(hours) },
                        label = {
                            Text(
                                text = stringResource(labelRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PreReleaseToggleCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    ExpressiveCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(Res.string.include_pre_releases_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(Res.string.include_pre_releases_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BatteryOptimizationCard(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    ExpressiveCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.battery_optimization_card_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(Res.string.battery_optimization_card_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                androidx.compose.material3.TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.battery_optimization_card_dismiss))
                }
                FilledTonalButton(onClick = onOpenSettings) {
                    Text(stringResource(Res.string.battery_optimization_card_open))
                }
            }
        }
    }
}
