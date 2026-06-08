package zed.rainxch.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.model.GithubRepoSummaryUi
import zed.rainxch.core.presentation.model.GithubUserUi
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.formatReleasedAt
import zed.rainxch.core.presentation.utils.hasWeekNotPassed
import zed.rainxch.core.presentation.utils.toIcons
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.forked_repository
import zed.rainxch.githubstore.core.presentation.res.hide_repository
import zed.rainxch.githubstore.core.presentation.res.mark_as_unviewed
import zed.rainxch.githubstore.core.presentation.res.mark_as_viewed
import zed.rainxch.githubstore.core.presentation.res.open_on_github
import zed.rainxch.githubstore.core.presentation.res.installed
import zed.rainxch.githubstore.core.presentation.res.open_in_browser
import zed.rainxch.githubstore.core.presentation.res.seen_badge
import zed.rainxch.githubstore.core.presentation.res.self_owned_badge
import zed.rainxch.githubstore.core.presentation.res.share_repository
import zed.rainxch.githubstore.core.presentation.res.update_available

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)
@Composable
fun RepositoryCard(
    discoveryRepositoryUi: DiscoveryRepositoryUi,
    onClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeveloperClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onHideClick: (() -> Unit)? = null,
    onToggleSeen: (() -> Unit)? = null,
) {
    val uriHandler = LocalUriHandler.current
    val contentAlpha by animateFloatAsState(
        targetValue = if (discoveryRepositoryUi.isSeen) 0.55f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "seen_content_alpha",
    )

    var showActionsSheet by remember { mutableStateOf(false) }
    val sheetEnabled = onHideClick != null
    val repo = discoveryRepositoryUi.repository

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .alpha(contentAlpha),
        shape = zed.rainxch.core.presentation.theme.tokens.Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Column(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = if (sheetEnabled) {
                        { showActionsSheet = true }
                    } else null,
                )
                .padding(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GitHubStoreImage(
                    imageModel = { repo.owner.avatarUrl },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable { onDeveloperClick(repo.owner.login) },
                    extractDominantFor = repo.owner.avatarUrl,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = repo.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (discoveryRepositoryUi.isCurrentUserOwner) OfficialBadge()
                        if (repo.isFork) ForkBadge()
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = repo.owner.login,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onDeveloperClick(repo.owner.login) }
                                .padding(vertical = 2.dp, horizontal = 2.dp)
                                .weight(1f, fill = false),
                        )
                        Text(
                            text = "  ·  ${formatReleasedAt(repo.updatedAt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            repo.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = desc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (repo.stargazersCount > 0) {
                    zed.rainxch.core.presentation.components.chips.StatChip(
                        label = formatCount(repo.stargazersCount.toLong()),
                        leading = {
                            Icon(
                                imageVector = Icons.Outlined.StarOutline,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                if (repo.forksCount > 0) {
                    zed.rainxch.core.presentation.components.chips.StatChip(
                        label = formatCount(repo.forksCount.toLong()),
                        leading = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.CallSplit,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                if (repo.downloadCount > 0) {
                    zed.rainxch.core.presentation.components.chips.StatChip(
                        label = formatCount(repo.downloadCount),
                        leading = {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                val platformKinds = repo.availablePlatforms
                    .mapNotNull { platform ->
                        when (platform) {
                            DiscoveryPlatform.Android -> zed.rainxch.core.presentation.vocabulary.PlatformKind.ANDROID
                            DiscoveryPlatform.Windows -> zed.rainxch.core.presentation.vocabulary.PlatformKind.WINDOWS
                            DiscoveryPlatform.Macos -> zed.rainxch.core.presentation.vocabulary.PlatformKind.MACOS
                            DiscoveryPlatform.Linux -> zed.rainxch.core.presentation.vocabulary.PlatformKind.LINUX
                            else -> null
                        }
                    }
                if (platformKinds.isNotEmpty()) {
                    zed.rainxch.core.presentation.components.chips.PlatformsChip(
                        platforms = platformKinds.let {
                            kotlinx.collections.immutable.persistentListOf<zed.rainxch.core.presentation.vocabulary.PlatformKind>()
                                .addAll(it)
                        },
                    )
                }
            }

            if (discoveryRepositoryUi.isInstalled || discoveryRepositoryUi.isSeen) {
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
        }
    }

    val hideAction = onHideClick
    if (showActionsSheet && hideAction != null) {
        RepositoryActionsBottomSheet(
            repository = discoveryRepositoryUi.repository,
            isSeen = discoveryRepositoryUi.isSeen,
            onDismiss = { showActionsSheet = false },
            onShare = {
                showActionsSheet = false
                onShareClick()
            },
            onOpenOnGithub = {
                showActionsSheet = false
                uriHandler.openUri(discoveryRepositoryUi.repository.htmlUrl)
            },
            onToggleSeen = onToggleSeen?.let {
                {
                    showActionsSheet = false
                    it()
                }
            },
            onHide = {
                showActionsSheet = false
                hideAction()
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepositoryActionsBottomSheet(
    repository: GithubRepoSummaryUi,
    isSeen: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOpenOnGithub: () -> Unit,
    onToggleSeen: (() -> Unit)?,
    onHide: (() -> Unit)?,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GitHubStoreImage(
                    imageModel = { repository.owner.avatarUrl },
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repository.fullName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    repository.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            SheetActionRow(
                label = stringResource(Res.string.share_repository),
                icon = Icons.Default.Share,
                onClick = onShare,
            )
            SheetActionRow(
                label = stringResource(Res.string.open_on_github),
                icon = Icons.Default.OpenInBrowser,
                onClick = onOpenOnGithub,
            )
            if (onToggleSeen != null) {
                SheetActionRow(
                    label =
                        if (isSeen) {
                            stringResource(Res.string.mark_as_unviewed)
                        } else {
                            stringResource(Res.string.mark_as_viewed)
                        },
                    icon = Icons.Outlined.Visibility,
                    onClick = onToggleSeen,
                )
            }
            if (onHide != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                SheetActionRow(
                    label = stringResource(Res.string.hide_repository),
                    icon = Icons.Default.VisibilityOff,
                    onClick = onHide,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SheetActionRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    ListItem(
        headlineContent = {
            Text(text = label, color = tint)
        },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        },
        colors =
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@Composable
fun PlatformChip(
    platform: DiscoveryPlatform,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        border = if (onClick != null) {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            )
        } else null,
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
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp).padding(end = 4.dp),
                )
            }
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
