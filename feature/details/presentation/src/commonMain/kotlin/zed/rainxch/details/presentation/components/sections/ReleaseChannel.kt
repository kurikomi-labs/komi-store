package zed.rainxch.details.presentation.components.sections

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.locals.LocalPersonality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.details.presentation.DetailsAction
import zed.rainxch.details.presentation.DetailsState
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.action_switch_to_stable
import zed.rainxch.githubstore.core.presentation.res.channel_chip_coachmark_body
import zed.rainxch.githubstore.core.presentation.res.channel_chip_coachmark_dismiss
import zed.rainxch.githubstore.core.presentation.res.channel_chip_coachmark_title
import zed.rainxch.githubstore.core.presentation.res.channel_chip_include_betas
import zed.rainxch.githubstore.core.presentation.res.channel_chip_stable_only
import zed.rainxch.githubstore.core.presentation.res.merged_whats_changed_title
import zed.rainxch.githubstore.core.presentation.res.stalled_project_warning_days
import zed.rainxch.githubstore.core.presentation.res.stalled_project_warning_description
import zed.rainxch.githubstore.core.presentation.res.stalled_project_warning_months

fun LazyListScope.releaseChannel(
    state: DetailsState,
    onAction: (DetailsAction) -> Unit,
) {
    val installedApp = state.installedApp
    val showMerged = !state.mergedChangelog.isNullOrBlank() && state.mergedChangelogBaseTag != null
    val showStalled = state.stalledStableSinceDays != null
    val showSwitchToStable = state.canSwitchToStable
    val showChannelChip = installedApp != null

    if (!showMerged && !showStalled && !showSwitchToStable && !showChannelChip) return

    item(key = "release-channel-controls") {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (showChannelChip || showSwitchToStable) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (installedApp != null) {
                        val includeBetas = installedApp.includePreReleases
                        val channelLabel =
                            if (includeBetas) {
                                stringResource(Res.string.channel_chip_include_betas)
                            } else {
                                stringResource(Res.string.channel_chip_stable_only)
                            }
                        val pulse by rememberChipPulse(active = state.isChannelChipCoachmarkPending)
                        Box(modifier = Modifier.scale(pulse)) {
                            ChannelChip(
                                label = channelLabel,
                                icon = Icons.Default.Science,

                                tint =
                                    if (includeBetas) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                onClick = { onAction(DetailsAction.ToggleIncludeBetas) },

                                contentDescriptionText = channelLabel,
                            )
                            if (state.isChannelChipCoachmarkPending) {
                                ChannelChipCoachmark(
                                    onDismiss = {
                                        onAction(DetailsAction.OnAcknowledgeChannelChipCoachmark)
                                    },
                                )
                            }
                        }
                    }

                    if (showSwitchToStable) {
                        val stable = state.latestStableRelease
                        if (stable != null) {
                            ChannelChip(
                                label =
                                    stringResource(
                                        Res.string.action_switch_to_stable,
                                        stable.tagName,
                                    ),
                                icon = Icons.Default.Restore,
                                tint = MaterialTheme.colorScheme.primary,
                                onClick = { onAction(DetailsAction.SwitchToStable) },
                                contentDescriptionText = null,
                            )
                        }
                    }
                }
            }

            val stalledDays = state.stalledStableSinceDays
            if (stalledDays != null) {
                val days = stalledDays
                val title =
                    if (days >= 30) {
                        stringResource(Res.string.stalled_project_warning_months, days / 30)
                    } else {
                        stringResource(Res.string.stalled_project_warning_days, days)
                    }
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    Res.string.stalled_project_warning_description,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }

            val mergedBaseTag = state.mergedChangelogBaseTag
            if (mergedBaseTag != null && !state.mergedChangelog.isNullOrBlank()) {
                val baseTag = mergedBaseTag
                Card(
                    colors =
                        CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = stringResource(
                                    Res.string.merged_whats_changed_title,
                                    baseTag,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.mergedChangelog.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChannelChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    contentDescriptionText: String?,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .then(
                    if (contentDescriptionText != null) {
                        Modifier.semantics { contentDescription = contentDescriptionText }
                    } else {
                        Modifier
                    },
                ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = tint,
            )
        }
    }
}

@Composable
private fun rememberChipPulse(active: Boolean) =
    rememberInfiniteTransition(label = "channel-chip-pulse")
        .animateFloat(
            initialValue = 1f,
            targetValue = if (active) 1.06f else 1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(durationMillis = 1100),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "channel-chip-pulse-scale",
        )

@Composable
private fun ChannelChipCoachmark(onDismiss: () -> Unit) {
    Popup(
        alignment = Alignment.TopStart,
        offset = androidx.compose.ui.unit.IntOffset(x = 0, y = -220),
        properties =
            PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
            ),
        onDismissRequest = onDismiss,
    ) {
        Surface(
            shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 6.dp,
            modifier = Modifier.width(280.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(Res.string.channel_chip_coachmark_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Text(
                    text = stringResource(Res.string.channel_chip_coachmark_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    KomiButton(
                        onClick = onDismiss,
                        label = stringResource(Res.string.channel_chip_coachmark_dismiss),
                        variant = KomiButtonVariant.Text,
                        size = KomiButtonSize.Sm,
                    )
                }
            }
        }
    }
}
