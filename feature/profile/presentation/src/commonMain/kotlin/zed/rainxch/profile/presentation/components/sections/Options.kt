package zed.rainxch.profile.presentation.components.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.profile.presentation.ProfileAction

fun LazyListScope.options(
    isUserLoggedIn: Boolean,
    hasUnreadAnnouncements: Boolean,
    onAction: (ProfileAction) -> Unit,
) {
    item {
        if (isUserLoggedIn) {
            OptionCard(
                icon = Icons.Default.Star,
                label = stringResource(Res.string.stars),
                description = stringResource(Res.string.profile_stars_description),
                onClick = { onAction(ProfileAction.OnStarredReposClick) },
            )
            Spacer(Modifier.height(8.dp))
        }

        OptionCard(
            icon = Icons.Default.Favorite,
            label = stringResource(Res.string.favourites),
            description = stringResource(Res.string.profile_favourites_description),
            onClick = { onAction(ProfileAction.OnFavouriteReposClick) },
        )
        Spacer(Modifier.height(8.dp))

        OptionCard(
            icon = Icons.Default.Schedule,
            label = stringResource(Res.string.recently_viewed),
            description = stringResource(Res.string.profile_recently_viewed_description),
            onClick = { onAction(ProfileAction.OnRecentlyViewedClick) },
        )
        Spacer(Modifier.height(8.dp))

        OptionCard(
            icon = Icons.Default.Campaign,
            label = stringResource(Res.string.whats_new_title),
            description = stringResource(Res.string.whats_new_profile_description),
            onClick = { onAction(ProfileAction.OnWhatsNewClick) },
            onLongClick = { onAction(ProfileAction.OnWhatsNewLongClick) },
        )
        Spacer(Modifier.height(8.dp))

        OptionCard(
            icon = Icons.Default.Notifications,
            label = stringResource(Res.string.announcements_title),
            description = stringResource(Res.string.announcements_profile_description),
            onClick = { onAction(ProfileAction.OnAnnouncementsClick) },
            onLongClick = { onAction(ProfileAction.OnAnnouncementsLongClick) },
            hasBadge = hasUnreadAnnouncements,
        )
        Spacer(Modifier.height(8.dp))

        OptionCard(
            icon = Icons.Default.Tune,
            label = stringResource(Res.string.tweaks_title),
            description = stringResource(Res.string.profile_tweaks_description),
            onClick = { onAction(ProfileAction.OnTweaksClick) },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OptionCard(
    icon: ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    hasBadge: Boolean = false,
) {
    val clickMod = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    } else {
        Modifier.combinedClickable(onClick = onClick)
    }
    Surface(
        modifier = modifier,
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = clickMod.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                if (hasBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error),
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
