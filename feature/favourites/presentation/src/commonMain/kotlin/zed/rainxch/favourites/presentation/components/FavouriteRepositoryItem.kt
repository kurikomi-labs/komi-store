package zed.rainxch.favourites.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NewReleases
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
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.components.GitHubStoreImage
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.ExpressiveCard
import zed.rainxch.core.presentation.components.OfficialBadge
import zed.rainxch.core.presentation.components.chips.StatChip
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.favourites.presentation.model.FavouriteRepository
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.remove_from_favourites

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun FavouriteRepositoryItem(
    favouriteRepository: FavouriteRepository,
    onToggleFavouriteClick: () -> Unit,
    onItemClick: () -> Unit,
    onDevProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpressiveCard(
        modifier = modifier,
        onClick = onItemClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Radii.chip)
                    .clickable(onClick = onDevProfileClick)
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GitHubStoreImage(
                    imageModel = { favouriteRepository.repoOwnerAvatarUrl },
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                )
                Text(
                    text = favouriteRepository.repoOwner,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (favouriteRepository.isCurrentUserOwner) {
                    OfficialBadge()
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = favouriteRepository.repoName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    favouriteRepository.repoDescription?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                FavoriteToggle(
                    favorited = true,
                    onClick = onToggleFavouriteClick,
                )
            }

            Spacer(Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                favouriteRepository.primaryLanguage?.let { language ->
                    StatChip(
                        label = language,
                        leading = {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        background = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = MaterialTheme.colorScheme.outline,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
                favouriteRepository.latestRelease?.let { release ->
                    StatChip(
                        label = release,
                        leading = {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                        background = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                        contentColor = MaterialTheme.colorScheme.primary,
                    )
                }
                StatChip(
                    label = favouriteRepository.addedAtFormatter,
                    leading = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    background = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = MaterialTheme.colorScheme.outline,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
                imageVector = Icons.Default.Favorite,
                contentDescription = stringResource(Res.string.remove_from_favourites),
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
