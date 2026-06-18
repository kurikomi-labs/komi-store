package zed.rainxch.starred.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.chips.KomiChip
import zed.rainxch.core.presentation.components.chips.KomiChipKind
import zed.rainxch.core.presentation.components.chips.KomiChipSize
import zed.rainxch.core.presentation.components.surfaces.KomiSurface
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.personality.utils.PersonalityPreview
import zed.rainxch.core.presentation.utils.formatCount
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.starred.presentation.model.StarredRepositoryUi

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StarredRepositoryItem(
    repository: StarredRepositoryUi,
    onToggleFavoriteClick: () -> Unit,
    onItemClick: () -> Unit,
    onDevProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KomiSurface(
        onClick = onItemClick,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GitHubStoreImage(
                    imageModel = { repository.repoOwnerAvatarUrl },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDevProfileClick),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onDevProfileClick),
                ) {
                    Text(
                        text = repository.repoName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = repository.repoOwner,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (repository.isCurrentUserOwner) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = stringResource(Res.string.self_owned_badge),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                FavoriteToggle(
                    favorited = repository.isFavorite,
                    onClick = onToggleFavoriteClick,
                )
            }

            repository.repoDescription?.let { description ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatChipNeutral(
                    icon = Icons.Default.Star,
                    label = formatCount(repository.stargazersCount),
                )
                StatChipNeutral(
                    icon = Icons.AutoMirrored.Filled.CallSplit,
                    label = formatCount(repository.forksCount),
                )
                if (repository.openIssuesCount > 0) {
                    StatChipNeutral(
                        icon = Icons.Outlined.Warning,
                        label = formatCount(repository.openIssuesCount),
                    )
                }
                repository.primaryLanguage?.let { language ->
                    StatChipNeutral(
                        icon = Icons.Default.Code,
                        label = language,
                    )
                }
                if (repository.isInstalled) {
                    TonalBadge(
                        text = stringResource(Res.string.installed),
                        container = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                        content = MaterialTheme.colorScheme.tertiary,
                    )
                }
                repository.latestRelease?.let { version ->
                    TonalBadge(
                        text = version,
                        container = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        content = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChipNeutral(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    KomiChip(
        label = label,
        kind = KomiChipKind.Info,
        size = KomiChipSize.Sm,
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun TonalBadge(
    text: String,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(LocalPersonality.current.shape.cornerSmall))
            .background(container)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = content,
        )
    }
}

@Composable
private fun FavoriteToggle(
    favorited: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (favorited) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val container = if (favorited) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(container),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(38.dp)) {
            Icon(
                imageVector = if (favorited) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Outlined.FavoriteBorder
                },
                contentDescription = if (favorited) {
                    stringResource(Res.string.remove_from_favourites)
                } else {
                    stringResource(Res.string.add_to_favourites)
                },
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview
@Composable
private fun PreviewStarredRepoItem() {
    PersonalityPreview {
        StarredRepositoryItem(
            repository = StarredRepositoryUi(
                repoId = 1,
                repoName = "awesome-app",
                repoOwner = "developer",
                repoOwnerAvatarUrl = "",
                repoDescription = "An awesome application that does amazing things",
                primaryLanguage = "Kotlin",
                repoUrl = "",
                stargazersCount = 1234,
                forksCount = 567,
                openIssuesCount = 12,
                isInstalled = true,
                isFavorite = false,
                latestRelease = "v1.2.3",
                latestReleaseUrl = null,
                starredAt = null,
            ),
            onToggleFavoriteClick = {},
            onItemClick = {},
            onDevProfileClick = {},
        )
    }
}
