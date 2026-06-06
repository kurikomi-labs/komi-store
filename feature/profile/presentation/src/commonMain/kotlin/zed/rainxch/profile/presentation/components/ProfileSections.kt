package zed.rainxch.profile.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.components.hub.GhsEntryRow
import zed.rainxch.core.presentation.components.hub.GhsSectionHeader
import zed.rainxch.core.presentation.theme.tokens.GhsAccents
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.utils.formatCount
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.profile.presentation.ProfileAction
import zed.rainxch.profile.presentation.ProfileState

fun LazyListScope.profileSections(
    state: ProfileState,
    hasUnreadAnnouncements: Boolean,
    onAction: (ProfileAction) -> Unit,
) {
    item(key = "identity") {
        HeroIdentityCard(state = state, onAction = onAction)
    }

    if (state.isUserLoggedIn) {
        item(key = "library_header") {
            Spacer(Modifier.height(8.dp))

            GhsSectionHeader(text = stringResource(Res.string.profile_section_library))

            Spacer(Modifier.height(8.dp))
        }
        item(key = "row_stars") {
            GhsEntryRow(
                title = stringResource(Res.string.stars),
                subtitle = stringResource(Res.string.profile_stars_description),
                icon = Icons.Outlined.Star,
                accentColor = GhsAccents.Gold,
                onClick = { onAction(ProfileAction.OnStarredReposClick) },
            )

            Spacer(Modifier.height(8.dp))
        }
    }

    item(key = "row_favourites") {
        GhsEntryRow(
            title = stringResource(Res.string.favourites),
            subtitle = stringResource(Res.string.profile_favourites_description),
            icon = Icons.Outlined.Favorite,
            accentColor = GhsAccents.Rose,
            onClick = { onAction(ProfileAction.OnFavouriteReposClick) },
        )

        Spacer(Modifier.height(8.dp))
    }
    item(key = "row_recent") {
        GhsEntryRow(
            title = stringResource(Res.string.recently_viewed),
            subtitle = stringResource(Res.string.profile_recently_viewed_description),
            icon = Icons.Outlined.Schedule,
            accentColor = GhsAccents.Sky,
            onClick = { onAction(ProfileAction.OnRecentlyViewedClick) },
        )
    }

    item(key = "updates_header") {
        Spacer(Modifier.height(8.dp))

        GhsSectionHeader(text = stringResource(Res.string.profile_section_updates))

        Spacer(Modifier.height(8.dp))
    }
    item(key = "row_whats_new") {
        GhsEntryRow(
            title = stringResource(Res.string.whats_new_title),
            subtitle = stringResource(Res.string.whats_new_profile_description),
            icon = Icons.Outlined.Campaign,
            accentColor = GhsAccents.Mint,
            onClick = { onAction(ProfileAction.OnWhatsNewClick) },
            onLongClick = { onAction(ProfileAction.OnWhatsNewLongClick) },
        )

        Spacer(Modifier.height(8.dp))
    }
    item(key = "row_announcements") {
        GhsEntryRow(
            title = stringResource(Res.string.announcements_title),
            subtitle = stringResource(Res.string.announcements_profile_description),
            icon = Icons.Outlined.Notifications,
            accentColor = GhsAccents.Lavender,
            onClick = { onAction(ProfileAction.OnAnnouncementsClick) },
            onLongClick = { onAction(ProfileAction.OnAnnouncementsLongClick) },
            badge = if (hasUnreadAnnouncements) {
                { UnreadDot() }
            } else {
                null
            },
        )
    }

    item(key = "app_header") {
        Spacer(Modifier.height(8.dp))

        GhsSectionHeader(text = stringResource(Res.string.section_app_block))

        Spacer(Modifier.height(8.dp))
    }
    item(key = "row_tweaks") {
        GhsEntryRow(
            title = stringResource(Res.string.tweaks_title),
            subtitle = stringResource(Res.string.profile_tweaks_description),
            icon = Icons.Outlined.Tune,
            accentColor = GhsAccents.Sage,
            onClick = { onAction(ProfileAction.OnTweaksClick) },
        )

        Spacer(Modifier.height(8.dp))
    }
    item(key = "row_about") {
        GhsEntryRow(
            title = stringResource(Res.string.profile_entry_about_title),
            subtitle = stringResource(Res.string.profile_entry_about_subtitle),
            icon = Icons.Outlined.Info,
            accentColor = GhsAccents.Aqua,
            onClick = { onAction(ProfileAction.OnAboutClick) },
        )
    }

    if (state.isUserLoggedIn) {
        item(key = "account_header") {
            Spacer(Modifier.height(8.dp))

            GhsSectionHeader(text = stringResource(Res.string.profile_section_account))

            Spacer(Modifier.height(8.dp))
        }
        item(key = "row_logout") {
            GhsEntryRow(
                title = stringResource(Res.string.logout),
                icon = Icons.AutoMirrored.Filled.Logout,
                destructive = true,
                trailingChevron = false,
                onClick = { onAction(ProfileAction.OnLogoutClick) },
            )
        }
    }
}

@Composable
private fun HeroIdentityCard(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.userProfile == null) {
                SignedOutContent(onAction = onAction)
            } else {
                SignedInContent(state = state, onAction = onAction)
            }
        }
    }
}

@Composable
private fun SignedOutContent(onAction: (ProfileAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.profile_sign_in_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.profile_sign_in_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        GhsButton(
            onClick = { onAction(ProfileAction.OnLoginClick) },
            label = stringResource(Res.string.profile_login),
            variant = GhsButtonVariant.Primary,
            size = GhsButtonSize.Lg,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SignedInContent(
    state: ProfileState,
    onAction: (ProfileAction) -> Unit,
) {
    val profile = state.userProfile ?: return
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GitHubStoreImage(
            imageModel = { profile.imageUrl },
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            extractDominantFor = profile.imageUrl,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val displayName = profile.name.takeIf { it.isNotBlank() } ?: profile.username
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "@${profile.username}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            profile.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
    Spacer(Modifier.height(4.dp))
    MetricsStrip(
        repos = profile.repositoryCount,
        followers = profile.followers,
        following = profile.following,
        onReposClick = { onAction(ProfileAction.OnRepositoriesClick(profile.username)) },
    )
}

@Composable
private fun MetricsStrip(
    repos: Int,
    followers: Int,
    following: Int,
    onReposClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Metric(
            value = formatCount(repos),
            label = stringResource(Res.string.profile_repos),
            modifier = Modifier
                .weight(1f)
                .clip(Radii.chip)
                .clickable(onClick = onReposClick)
                .padding(vertical = 6.dp),
        )
        MetricDivider()
        Metric(
            value = formatCount(followers),
            label = stringResource(Res.string.followers),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp),
        )
        MetricDivider()
        Metric(
            value = formatCount(following),
            label = stringResource(Res.string.following),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp),
        )
    }
}

@Composable
private fun Metric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    )
}

@Composable
private fun UnreadDot() {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error),
    )
}
