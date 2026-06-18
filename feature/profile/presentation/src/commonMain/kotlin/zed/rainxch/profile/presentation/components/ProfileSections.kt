package zed.rainxch.profile.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonSize
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
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

            KomiText(
                text = stringResource(Res.string.profile_section_library),
                role = KomiTextRole.Title,
            )

            Spacer(Modifier.height(8.dp))
        }
        item(key = "row_stars") {
            ProfileEntryRow(
                title = stringResource(Res.string.stars),
                subtitle = stringResource(Res.string.profile_stars_description),
                icon = Icons.Outlined.Star,
                accentColor = MaterialTheme.colorScheme.primary,
                onClick = { onAction(ProfileAction.OnStarredReposClick) },
            )

            Spacer(Modifier.height(8.dp))
        }
    }

    item(key = "row_favourites") {
        ProfileEntryRow(
            title = stringResource(Res.string.favourites),
            subtitle = stringResource(Res.string.profile_favourites_description),
            icon = Icons.Outlined.Favorite,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { onAction(ProfileAction.OnFavouriteReposClick) },
        )

        Spacer(Modifier.height(8.dp))
    }
    item(key = "row_recent") {
        ProfileEntryRow(
            title = stringResource(Res.string.recently_viewed),
            subtitle = stringResource(Res.string.profile_recently_viewed_description),
            icon = Icons.Outlined.Schedule,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { onAction(ProfileAction.OnRecentlyViewedClick) },
        )
    }

    item(key = "updates_header") {
        Spacer(Modifier.height(8.dp))

        KomiText(
            text = stringResource(Res.string.profile_section_updates),
            role = KomiTextRole.Title,
        )

        Spacer(Modifier.height(8.dp))
    }
    item(key = "row_whats_new") {
        ProfileEntryRow(
            title = stringResource(Res.string.whats_new_title),
            subtitle = stringResource(Res.string.whats_new_profile_description),
            icon = Icons.Outlined.Campaign,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { onAction(ProfileAction.OnWhatsNewClick) },
        )

        Spacer(Modifier.height(8.dp))
    }
    item(key = "row_announcements") {
        ProfileEntryRow(
            title = stringResource(Res.string.announcements_title),
            subtitle = stringResource(Res.string.announcements_profile_description),
            icon = Icons.Outlined.Notifications,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { onAction(ProfileAction.OnAnnouncementsClick) },
            badge = if (hasUnreadAnnouncements) {
                { UnreadDot() }
            } else {
                null
            },
        )
    }

    item(key = "app_header") {
        Spacer(Modifier.height(8.dp))

        KomiText(
            text = stringResource(Res.string.section_app_block),
            role = KomiTextRole.Title,
        )

        Spacer(Modifier.height(8.dp))
    }
    item(key = "row_tweaks") {
        ProfileEntryRow(
            title = stringResource(Res.string.tweaks_title),
            subtitle = stringResource(Res.string.profile_tweaks_description),
            icon = Icons.Outlined.Tune,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { onAction(ProfileAction.OnTweaksClick) },
        )

        Spacer(Modifier.height(8.dp))
    }
    item(key = "row_about") {
        ProfileEntryRow(
            title = stringResource(Res.string.profile_entry_about_title),
            subtitle = stringResource(Res.string.profile_entry_about_subtitle),
            icon = Icons.Outlined.Info,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = { onAction(ProfileAction.OnAboutClick) },
        )
    }

    if (state.isUserLoggedIn) {
        item(key = "account_header") {
            Spacer(Modifier.height(8.dp))

            KomiText(
                text = stringResource(Res.string.profile_section_account),
                role = KomiTextRole.Title,
            )

            Spacer(Modifier.height(8.dp))
        }
        item(key = "row_logout") {
            ProfileEntryRow(
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
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
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
        KomiButton(
            onClick = { onAction(ProfileAction.OnLoginClick) },
            label = stringResource(Res.string.profile_login),
            variant = KomiButtonVariant.Primary,
            size = KomiButtonSize.Lg,
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
                .clip(RoundedCornerShape(LocalPersonality.current.shape.cornerSmall))
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProfileEntryRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accentColor: Color = Color.Unspecified,
    onLongClick: (() -> Unit)? = null,
    badge: (@Composable () -> Unit)? = null,
    destructive: Boolean = false,
    trailingChevron: Boolean = true,
) {
    val accent = if (destructive) MaterialTheme.colorScheme.error else accentColor
    val titleColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(LocalPersonality.current.shape.corner))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            KomiText(text = title, role = KomiTextRole.Label, color = titleColor)
            subtitle?.let {
                KomiText(
                    text = it,
                    role = KomiTextRole.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        badge?.invoke()
        if (trailingChevron) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
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
