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
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
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
                onClick = {
                    onAction(ProfileAction.OnStarredReposClick)
                },
            )
        }

        Spacer(Modifier.height(4.dp))

        OptionCard(
            icon = Icons.Default.Favorite,
            label = stringResource(Res.string.favourites),
            description = stringResource(Res.string.profile_favourites_description),
            onClick = {
                onAction(ProfileAction.OnFavouriteReposClick)
            },
        )

        Spacer(Modifier.height(4.dp))

        OptionCard(
            icon = Icons.Default.Schedule,
            label = stringResource(Res.string.recently_viewed),
            description = stringResource(Res.string.profile_recently_viewed_description),
            onClick = {
                onAction(ProfileAction.OnRecentlyViewedClick)
            },
        )

        Spacer(Modifier.height(4.dp))

        OptionCard(
            icon = Icons.Default.Campaign,
            label = stringResource(Res.string.whats_new_title),
            description = stringResource(Res.string.whats_new_profile_description),
            onClick = {
                onAction(ProfileAction.OnWhatsNewClick)
            },
            onLongClick = {
                onAction(ProfileAction.OnWhatsNewLongClick)
            },
        )

        Spacer(Modifier.height(4.dp))

        OptionCard(
            icon = Icons.Default.Notifications,
            label = stringResource(Res.string.announcements_title),
            description = stringResource(Res.string.announcements_profile_description),
            onClick = {
                onAction(ProfileAction.OnAnnouncementsClick)
            },
            onLongClick = {
                onAction(ProfileAction.OnAnnouncementsLongClick)
            },
            hasBadge = hasUnreadAnnouncements,
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
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
    val cardColors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = .7f),
        disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f),
    )
    val cardShape = RoundedCornerShape(32.dp)
    val cardBorder = BorderStroke(
        width = .5.dp,
        color = MaterialTheme.colorScheme.surface,
    )

    if (onLongClick != null) {
        Card(
            modifier = modifier.combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
            colors = cardColors,
            shape = cardShape,
            border = cardBorder,
        ) {
            OptionCardContent(
                icon = icon,
                label = label,
                description = description,
                hasBadge = hasBadge
            )
        }
        return
    }

    Card(
        modifier = modifier,
        colors = cardColors,
        onClick = onClick,
        shape = cardShape,
        border = cardBorder,
    ) {
        OptionCardContent(
            icon = icon,
            label = label,
            description = description
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OptionCardContent(
    icon: ImageVector,
    label: String,
    description: String,
    hasBadge: Boolean = false,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                ),
                            ),
                        ).padding(6.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .align(Alignment.TopEnd),
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = label,
                maxLines = 1,
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = description,
                maxLines = 2,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
