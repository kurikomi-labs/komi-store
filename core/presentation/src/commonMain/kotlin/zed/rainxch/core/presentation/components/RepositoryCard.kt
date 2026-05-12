package zed.rainxch.core.presentation.components

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.model.GithubRepoSummaryUi
import zed.rainxch.core.presentation.model.GithubUserUi
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.formatReleasedAt
import zed.rainxch.core.presentation.utils.hasWeekNotPassed
import zed.rainxch.core.presentation.utils.toIcons
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.forked_repository
import zed.rainxch.githubstore.core.presentation.res.home_view_details
import zed.rainxch.githubstore.core.presentation.res.installed
import zed.rainxch.githubstore.core.presentation.res.open_in_browser
import zed.rainxch.githubstore.core.presentation.res.seen_badge
import zed.rainxch.githubstore.core.presentation.res.self_owned_badge
import zed.rainxch.githubstore.core.presentation.res.share_repository
import zed.rainxch.githubstore.core.presentation.res.update_available

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun RepositoryCard(
    discoveryRepositoryUi: DiscoveryRepositoryUi,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeveloperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    val contentAlpha by animateFloatAsState(
        targetValue = if (discoveryRepositoryUi.isSeen) 0.55f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "seen_content_alpha",
    )

    ExpressiveCard(
        onClick = onClick,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.alpha(contentAlpha)) {
            if (discoveryRepositoryUi.isFavourite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    modifier =
                        Modifier
                            .size(120.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-32).dp, y = 32.dp),
                )
            }

            if (discoveryRepositoryUi.isStarred) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                    modifier =
                        Modifier
                            .size(120.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 32.dp, y = (-32).dp),
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .clip(CircleShape)
                                .clickable(onClick = {
                                    onDeveloperClick(discoveryRepositoryUi.repository.owner.login)
                                })
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        GitHubStoreImage(
                            imageModel = { discoveryRepositoryUi.repository.owner.avatarUrl },
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                        )

                        Text(
                            text = discoveryRepositoryUi.repository.owner.login,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        if (discoveryRepositoryUi.isCurrentUserOwner) {
                            OfficialBadge()
                        }
                    }

                    Text(
                        text = "/",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.outline,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = discoveryRepositoryUi.repository.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )

                    if (discoveryRepositoryUi.repository.isFork) {
                        ForkBadge()
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                discoveryRepositoryUi.repository.description?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyLarge,
                        softWrap = true,
                    )
                }

                Spacer(Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                ) {
                    InfoChip(
                        icon = Icons.Outlined.StarOutline,
                        text = formatCount(discoveryRepositoryUi.repository.stargazersCount.toLong()),
                    )

                    InfoChip(
                        icon = Icons.AutoMirrored.Outlined.CallSplit,
                        text = formatCount(discoveryRepositoryUi.repository.forksCount.toLong()),
                    )

                    if (discoveryRepositoryUi.repository.downloadCount > 0) {
                        InfoChip(
                            icon = Icons.Outlined.Download,
                            text = formatCount(discoveryRepositoryUi.repository.downloadCount),
                        )
                    }

                    discoveryRepositoryUi.repository.language?.let {
                        InfoChip(
                            icon = Icons.Outlined.Code,
                            text = it,
                        )
                    }
                }

                if (discoveryRepositoryUi.isInstalled || discoveryRepositoryUi.isSeen) {
                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (discoveryRepositoryUi.isInstalled) {
                            InstallStatusBadge(
                                isUpdateAvailable = discoveryRepositoryUi.isUpdateAvailable,
                            )
                        }

                        if (discoveryRepositoryUi.isSeen) {
                            SeenBadge()
                        }
                    }
                }

                if (discoveryRepositoryUi.repository.availablePlatforms.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        discoveryRepositoryUi.repository.availablePlatforms.forEach { platform ->
                            PlatformChip(platform = platform)
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                val releasedAtText =
                    buildAnnotatedString {
                        if (hasWeekNotPassed(discoveryRepositoryUi.repository.updatedAt)) {
                            append("🔥 ")
                        }

                        append(formatReleasedAt(discoveryRepositoryUi.repository.updatedAt))
                    }

                Text(
                    text = releasedAtText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GithubStoreButton(
                        text = stringResource(Res.string.home_view_details),
                        onClick = onClick,
                        modifier = Modifier.weight(1f),
                    )

                    IconButton(
                        onClick = onShareClick,
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(Res.string.share_repository),
                        )
                    }

                    IconButton(
                        onClick = {
                            uriHandler.openUri(discoveryRepositoryUi.repository.htmlUrl)
                        },
                        colors =
                            IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            ),
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = stringResource(Res.string.open_in_browser),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlatformChip(
    platform: DiscoveryPlatform,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        FlowRow(
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        ) {
            platform.toIcons().forEach { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = platform.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
fun ForkBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.CallSplit,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(Res.string.forked_repository),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun InstallStatusBadge(
    isUpdateAvailable: Boolean,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        if (isUpdateAvailable) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }

    val textColor =
        if (isUpdateAvailable) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }

    val icon =
        if (isUpdateAvailable) {
            Icons.Default.Update
        } else {
            Icons.Default.CheckCircle
        }

    val text =
        if (isUpdateAvailable) {
            stringResource(Res.string.update_available)
        } else {
            stringResource(Res.string.installed)
        }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = textColor,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun OfficialBadge(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Filled.Verified,
        contentDescription = stringResource(Res.string.self_owned_badge),
        modifier = modifier.size(16.dp),
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun SeenBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Visibility,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.seen_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview
@Composable
fun RepositoryCardPreview() {
    GithubStoreTheme {
        RepositoryCard(
            discoveryRepositoryUi =
                DiscoveryRepositoryUi(
                    repository =
                        GithubRepoSummaryUi(
                            id = 0L,
                            name = "Hello",
                            fullName = "JIFEOJEF",
                            owner =
                                GithubUserUi(
                                    id = 0L,
                                    login = "Skydoves",
                                    avatarUrl = "ewfew",
                                    htmlUrl = "grgrre",
                                ),
                            description = "Hello wolrd Hello wolrd Hello wolrd Hello wolrd Hello wolrd",
                            htmlUrl = "",
                            stargazersCount = 20,
                            forksCount = 4,
                            language = "Kotlin",
                            topics = null,
                            releasesUrl = "",
                            updatedAt = "2025-12-01T12:00:00Z",
                            defaultBranch = "",
                        ),
                    isUpdateAvailable = true,
                    isFavourite = true,
                    isInstalled = true,
                    isStarred = false,
                ),
            onClick = { },
            onShareClick = { },
            onDeveloperClick = { },
        )
    }
}

@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatCount(count: Long): String =
    when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
