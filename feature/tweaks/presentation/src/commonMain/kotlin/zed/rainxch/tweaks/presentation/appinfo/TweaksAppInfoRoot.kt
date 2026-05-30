package zed.rainxch.tweaks.presentation.appinfo

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.theme.tokens.GhsAccents
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.components.hub.GhsSectionHeader
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_app_name
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_website
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_community_business_cta
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_community_business_subtitle
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_community_business_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_community_section
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_community_subtitle
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_community_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_licenses_subtitle
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_licenses_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_privacy_policy_subtitle
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_privacy_policy_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_source_code_subtitle
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_source_code_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_tagline
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_whats_new_subtitle
import zed.rainxch.githubstore.core.presentation.res.tweaks_app_info_whats_new_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_entry_app_info
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksViewModel
import zed.rainxch.tweaks.presentation.components.TweaksSubScreenScaffold

private const val PRIVACY_POLICY_URL = "https://github-store.org/privacy-policy"
private const val SOURCE_CODE_URL = "https://github.com/OpenHub-Store/GitHub-Store"

private const val TELEGRAM_URL = "https://t.me/githubstore"
private const val DISCORD_URL = "https://discord.gg/githubstore"
private const val MASTODON_URL = "https://mastodon.social/@githubstore"
private const val REDDIT_URL = "https://reddit.com/r/githubstore"
private const val GITHUB_ORG_URL = "https://github.com/OpenHub-Store"
private const val WEBSITE_URL = "https://github-store.org"
private const val BUSINESS_EMAIL = "mailto:hello@github-store.org"

@Composable
fun TweaksAppInfoRoot(
    onNavigateBack: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    viewModel: TweaksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarState = remember { SnackbarHostState() }
    val uriHandler = LocalUriHandler.current

    TweaksSubScreenScaffold(
        title = stringResource(Res.string.tweaks_entry_app_info),
        onNavigateBack = onNavigateBack,
        snackbarState = snackbarState,
        restartReasons = state.needsRestartReasons,
        onRestartNow = { viewModel.onAction(TweaksAction.OnRestartNowClick) },
        onRestartLater = { viewModel.onAction(TweaksAction.OnRestartLaterClick) },
        showRestartBanner = state.restartBannerVisible,
    ) {
        item(key = "app_identity") {
            AppIdentityCard(versionName = state.versionName)
            Spacer(Modifier.height(20.dp))
        }

        item(key = "community_section_header") {
            GhsSectionHeader(text = stringResource(Res.string.tweaks_app_info_community_section))
            Spacer(Modifier.height(8.dp))
        }

        item(key = "community_card") {
            CommunityCard(
                onTelegram = { runCatching { uriHandler.openUri(TELEGRAM_URL) } },
                onDiscord = { runCatching { uriHandler.openUri(DISCORD_URL) } },
                onMastodon = { runCatching { uriHandler.openUri(MASTODON_URL) } },
                onReddit = { runCatching { uriHandler.openUri(REDDIT_URL) } },
                onGithub = { runCatching { uriHandler.openUri(GITHUB_ORG_URL) } },
                onWebsite = { runCatching { uriHandler.openUri(WEBSITE_URL) } },
                onBusiness = { runCatching { uriHandler.openUri(BUSINESS_EMAIL) } },
            )
            Spacer(Modifier.height(20.dp))
        }

        item(key = "action_licenses") {
            ActionRow(
                icon = Icons.Outlined.Code,
                title = stringResource(Res.string.tweaks_app_info_licenses_title),
                subtitle = stringResource(Res.string.tweaks_app_info_licenses_subtitle),
                accent = GhsAccents.Sage,
                onClick = onNavigateToLicenses,
            )
            Spacer(Modifier.height(8.dp))
        }

        item(key = "action_privacy") {
            ActionRow(
                icon = Icons.Outlined.Description,
                title = stringResource(Res.string.tweaks_app_info_privacy_policy_title),
                subtitle = stringResource(Res.string.tweaks_app_info_privacy_policy_subtitle),
                accent = GhsAccents.Rose,
                onClick = {
                    runCatching { uriHandler.openUri(PRIVACY_POLICY_URL) }
                },
            )
            Spacer(Modifier.height(8.dp))
        }

        item(key = "action_source") {
            ActionRow(
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                title = stringResource(Res.string.tweaks_app_info_source_code_title),
                subtitle = stringResource(Res.string.tweaks_app_info_source_code_subtitle),
                accent = GhsAccents.Aqua,
                onClick = {
                    runCatching { uriHandler.openUri(SOURCE_CODE_URL) }
                },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AppIdentityCard(versionName: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(Radii.cardSm)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Store,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.tweaks_app_info_app_name),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = versionName.ifBlank { "—" },
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.tweaks_app_info_tagline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommunityCard(
    onTelegram: () -> Unit,
    onDiscord: () -> Unit,
    onMastodon: () -> Unit,
    onReddit: () -> Unit,
    onGithub: () -> Unit,
    onWebsite: () -> Unit,
    onBusiness: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.tweaks_app_info_community_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(Res.string.tweaks_app_info_community_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SocialTile(
                    label = "Telegram",
                    iconUrl = "https://cdn.simpleicons.org/telegram/000000",
                    accent = GhsAccents.Sky,
                    onClick = onTelegram,
                    modifier = Modifier.weight(1f),
                )
                SocialTile(
                    label = "Discord",
                    iconUrl = "https://cdn.simpleicons.org/discord/000000",
                    accent = GhsAccents.Periwinkle,
                    onClick = onDiscord,
                    modifier = Modifier.weight(1f),
                )
                SocialTile(
                    label = "Mastodon",
                    iconUrl = "https://cdn.simpleicons.org/mastodon/000000",
                    accent = GhsAccents.Lavender,
                    onClick = onMastodon,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SocialTile(
                    label = "Reddit",
                    iconUrl = "https://cdn.simpleicons.org/reddit/000000",
                    accent = GhsAccents.Peach,
                    onClick = onReddit,
                    modifier = Modifier.weight(1f),
                )
                SocialTile(
                    label = "GitHub",
                    iconUrl = "https://cdn.simpleicons.org/github/000000",
                    accent = GhsAccents.Tan,
                    onClick = onGithub,
                    modifier = Modifier.weight(1f),
                )
                SocialTile(
                    label = stringResource(Res.string.tweaks_app_info_website),
                    iconFallback = Icons.Outlined.Language,
                    accent = GhsAccents.Sage,
                    onClick = onWebsite,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.tweaks_app_info_community_business_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(Res.string.tweaks_app_info_community_business_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                GhsButton(
                    onClick = onBusiness,
                    label = stringResource(Res.string.tweaks_app_info_community_business_cta),
                    variant = GhsButtonVariant.Outline,
                    size = GhsButtonSize.Sm,
                    trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                )
            }
        }
    }
}

@Composable
private fun SocialTile(
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconUrl: String? = null,
    iconFallback: ImageVector? = null,
) {
    Surface(
        modifier = modifier
            .clip(Radii.row)
            .clickable(onClick = onClick),
        shape = Radii.row,
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(Radii.chip)
                    .background(accent.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                if (iconUrl != null) {
                    coil3.compose.AsyncImage(
                        model = iconUrl,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(accent),
                    )
                } else if (iconFallback != null) {
                    Icon(
                        imageVector = iconFallback,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActionRow(
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
            .clip(Radii.row)
            .clickable(onClick = onClick),
        shape = Radii.row,
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
                    .clip(Radii.chip)
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
