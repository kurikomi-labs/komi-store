package zed.rainxch.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.color.avatarColorFor
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import zed.rainxch.core.presentation.components.cards.RepoStripeCard
import zed.rainxch.core.presentation.components.chips.PlatformsChip
import zed.rainxch.core.presentation.components.chips.StatChip
import zed.rainxch.core.presentation.utils.formatCount
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.home_action_get
import zed.rainxch.githubstore.core.presentation.res.home_rank_format
import zed.rainxch.githubstore.core.presentation.res.open
import zed.rainxch.githubstore.core.presentation.res.update
import zed.rainxch.home.presentation.model.HomeRepoCardUi

@Composable
fun TrendingRowItem(
    card: HomeRepoCardUi,
    rank: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RankRowItem(
        card = card,
        rank = rank,
        rankColor = MaterialTheme.colorScheme.onSurface,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    )
}

@Composable
fun PopularRowItem(
    card: HomeRepoCardUi,
    rank: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RankRowItem(
        card = card,
        rank = rank,
        rankColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankRowItem(
    card: HomeRepoCardUi,
    rank: Int,
    rankColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = avatarColorFor(card.ownerAvatarUrl, MaterialTheme.colorScheme.primary)
    RepoStripeCard(
        accent = accent,
        ownerLogin = card.ownerLogin,
        name = card.name,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        stripeTrailing = {
            Text(
                text = stringResource(Res.string.home_rank_format, rank),
                color = rankColor,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 34.sp,
                    letterSpacing = (-0.5).sp,
                ),
            )
        },
        avatar = {
            GitHubStoreImage(
                imageModel = { card.ownerAvatarUrl },
                modifier = Modifier.size(72.dp).clip(CircleShape),
                extractDominantFor = card.ownerAvatarUrl,
            )
        },
        chips = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (card.starsCount > 0) {
                    StatChip(
                        label = formatCount(card.starsCount),
                        leading = {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                if (card.rawRepository.forksCount > 0) {
                    StatChip(
                        label = formatCount(card.rawRepository.forksCount),
                        leading = {
                            Icon(
                                imageVector = Icons.Outlined.AccountTree,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                if (card.downloadsCount > 0) {
                    StatChip(
                        label = formatCount(card.downloadsCount),
                        leading = {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                if (card.platforms.isNotEmpty()) {
                    PlatformsChip(platforms = card.platforms)
                }
            }
        },
        languagePill = null,
        cta = {
            GhsButton(
                onClick = onClick,
                label = stringResource(
                    when {
                        card.isUpdateAvailable -> Res.string.update
                        card.isInstalled -> Res.string.open
                        else -> Res.string.home_action_get
                    },
                ),
                variant = GhsButtonVariant.Primary,
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            )
        },
    )
}
