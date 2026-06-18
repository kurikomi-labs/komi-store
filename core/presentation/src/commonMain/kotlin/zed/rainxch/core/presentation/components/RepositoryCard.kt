package zed.rainxch.core.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Update
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.repository.DiscoveryPlatform
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.model.GithubRepoSummaryUi
import zed.rainxch.core.presentation.model.GithubUserUi
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.utils.formatCompactCount
import zed.rainxch.core.presentation.utils.formatRelativeShort
import zed.rainxch.core.presentation.utils.toIcons
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.forked_repository
import zed.rainxch.githubstore.core.presentation.res.hide_repository
import zed.rainxch.githubstore.core.presentation.res.installed
import zed.rainxch.githubstore.core.presentation.res.mark_as_unviewed
import zed.rainxch.githubstore.core.presentation.res.mark_as_viewed
import zed.rainxch.githubstore.core.presentation.res.open_on_github
import zed.rainxch.githubstore.core.presentation.res.repo_card_details
import zed.rainxch.githubstore.core.presentation.res.seen_badge
import zed.rainxch.githubstore.core.presentation.res.self_owned_badge
import zed.rainxch.githubstore.core.presentation.res.share_repository
import zed.rainxch.githubstore.core.presentation.res.update_available

@OptIn(
    ExperimentalLayoutApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
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
    onLongClick: (() -> Unit)? = null,
    compact: Boolean = false,
    trailingBadge: (@Composable () -> Unit)? = null,
) {
    val repository = discoveryRepositoryUi.repository
    val uriHandler = LocalUriHandler.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.984f else 1f,
        animationSpec = tween(durationMillis = 140),
        label = "repo_card_press_scale",
    )
    val seenAlpha by animateFloatAsState(
        targetValue = if (discoveryRepositoryUi.isSeen) 0.55f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "repo_card_seen_alpha",
    )
    var showActionsSheet by remember { mutableStateOf(false) }
    val longPress = onLongClick ?: onHideClick?.let { { showActionsSheet = true } }
    val platforms = remember(repository.availablePlatforms) {
        repository.availablePlatforms.filter { it != DiscoveryPlatform.All }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
                alpha = seenAlpha
            },
        shape = RoundedCornerShape(if (compact) 20.dp else 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
        ),
    ) {
        Column(
            modifier = Modifier
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                    onLongClick = longPress,
                )
                .padding(if (compact) 13.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 11.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RepoAvatarTile(
                    avatarUrl = repository.owner.avatarUrl,
                    seed = repository.name.ifBlank { repository.owner.login },
                    sizeDp = if (compact) 46 else 52,
                    cornerDp = if (compact) 12 else 14,
                    monogramSp = 19,
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = repository.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.5f.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )

                        if (discoveryRepositoryUi.isCurrentUserOwner) {
                            OfficialBadge()
                        }

                        if (repository.isFork) {
                            ForkBadge()
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = repository.owner.login,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { onDeveloperClick(repository.owner.login) }
                                .weight(1f, fill = false),
                        )

                        repository.language?.takeIf { it.isNotBlank() }?.let { language ->
                            Box(
                                modifier = Modifier
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                            )

                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(languageDotColor(language)),
                            )

                            Text(
                                text = language,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                if (trailingBadge != null) {
                    trailingBadge()
                }
            }

            repository.description?.takeIf { it.isNotBlank() }?.let { description ->
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (compact) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val releaseTag = repository.latestReleaseTag
            if ((platforms.isNotEmpty() || !releaseTag.isNullOrBlank()) && !compact) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (!releaseTag.isNullOrBlank()) {
                        RepoReleasePill(
                            tag = releaseTag,
                            releaseDate = repository.latestReleaseDate,
                        )
                    }

                    platforms.forEach { platform ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                platform.toIcons().firstOrNull()?.let { icon ->
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                Text(
                                    text = platformLabel(platform),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }

            if (discoveryRepositoryUi.isInstalled || discoveryRepositoryUi.isSeen) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (discoveryRepositoryUi.isInstalled) {
                        InstallStatusBadge(isUpdateAvailable = discoveryRepositoryUi.isUpdateAvailable)
                    }

                    if (discoveryRepositoryUi.isSeen) {
                        SeenBadge()
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (repository.stargazersCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )

                            Text(
                                text = formatCompactCount(repository.stargazersCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (repository.downloadCount > 0) {
                        if (repository.stargazersCount > 0) {
                            FooterDot()
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Text(
                                text = formatCompactCount(repository.downloadCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (repository.stargazersCount > 0 || repository.downloadCount > 0) {
                        FooterDot()
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Text(
                            text = formatRelativeShort(repository.updatedAt),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.repo_card_details),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                    )

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showActionsSheet && onHideClick != null) {
        RepositoryActionsBottomSheet(
            repository = repository,
            isSeen = discoveryRepositoryUi.isSeen,
            onDismiss = { showActionsSheet = false },
            onShare = {
                showActionsSheet = false
                onShareClick()
            },
            onOpenOnGithub = {
                showActionsSheet = false
                uriHandler.openUri(repository.htmlUrl)
            },
            onToggleSeen = onToggleSeen?.let {
                {
                    showActionsSheet = false
                    it()
                }
            },
            onHide = {
                showActionsSheet = false
                onHideClick()
            },
        )
    }
}

@Composable
private fun FooterDot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
    )
}

private fun platformLabel(platform: DiscoveryPlatform): String =
    when (platform) {
        DiscoveryPlatform.Android -> "Android"
        DiscoveryPlatform.Windows -> "Windows"
        DiscoveryPlatform.Macos -> "macOS"
        DiscoveryPlatform.Linux -> "Linux"
        DiscoveryPlatform.Ios -> "iOS"
        DiscoveryPlatform.All -> ""
    }

private fun languageDotColor(language: String): Color =
    when (language.lowercase()) {
        "kotlin" -> Color(0xFFA97BFF)
        "java" -> Color(0xFFB07219)
        "swift" -> Color(0xFFF05138)
        "go" -> Color(0xFF00ADD8)
        "rust" -> Color(0xFFDEA584)
        "python" -> Color(0xFF3572A5)
        "javascript" -> Color(0xFFF1E05A)
        "typescript" -> Color(0xFF3178C6)
        "dart" -> Color(0xFF00B4AB)
        "c++", "cpp" -> Color(0xFFF34B7D)
        "c#", "csharp" -> Color(0xFF178600)
        "c" -> Color(0xFF555555)
        "ruby" -> Color(0xFF701516)
        "php" -> Color(0xFF4F5D95)
        "shell" -> Color(0xFF89E051)
        "html" -> Color(0xFFE34C26)
        "css" -> Color(0xFF563D7C)
        else -> Color(0xFF8E8E93)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GitHubStoreImage(
                    imageModel = { repository.owner.avatarUrl },
                    modifier = Modifier
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
                    label = if (isSeen) {
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
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    ListItem(
        headlineContent = {
            Text(text = label, color = tint)
        },
        leadingContent = {
            Icon(imageVector = icon, contentDescription = null, tint = tint)
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.clickable(onClick = onClick),
    )
}

@OptIn(ExperimentalLayoutApi::class)
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
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            )
        } else {
            null
        },
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (isUpdateAvailable) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (isUpdateAvailable) Icons.Default.Update else Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (isUpdateAvailable) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
            )

            Text(
                text = if (isUpdateAvailable) {
                    stringResource(Res.string.update_available)
                } else {
                    stringResource(Res.string.installed)
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (isUpdateAvailable) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
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
            discoveryRepositoryUi = DiscoveryRepositoryUi(
                repository = GithubRepoSummaryUi(
                    id = 0L,
                    name = "ollama",
                    fullName = "ollama/ollama",
                    owner = GithubUserUi(
                        id = 0L,
                        login = "ollama",
                        avatarUrl = "",
                        htmlUrl = "",
                    ),
                    description = "Get up and running with large language models locally. Run Llama, Mistral, Gemma and more.",
                    htmlUrl = "",
                    stargazersCount = 173_000,
                    forksCount = 4200,
                    language = "Go",
                    topics = null,
                    releasesUrl = "",
                    updatedAt = "2025-12-01T12:00:00Z",
                    defaultBranch = "",
                    downloadCount = 174_000_000,
                ),
                isUpdateAvailable = false,
                isFavourite = false,
                isInstalled = false,
                isStarred = false,
            ),
            onClick = { },
            onShareClick = { },
            onDeveloperClick = { },
        )
    }
}
