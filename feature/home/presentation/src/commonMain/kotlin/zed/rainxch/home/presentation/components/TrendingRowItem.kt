package zed.rainxch.home.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.buttons.OutlineButton
import zed.rainxch.core.presentation.components.cards.RowCard
import zed.rainxch.core.presentation.vocabulary.PlatformGlyph
import zed.rainxch.core.presentation.vocabulary.StarTier
import zed.rainxch.home.presentation.model.HomeRepoCardUi

@OptIn(ExperimentalFoundationApi::class)
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
        rankColor = MaterialTheme.colorScheme.primary,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
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
        rankColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RankRowItem(
    card: HomeRepoCardUi,
    rank: Int,
    rankColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        RowCard {
            Text(
                text = "#$rank",
                color = rankColor,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                ),
                modifier = Modifier.width(38.dp),
            )
            GitHubStoreImage(
                imageModel = { card.ownerAvatarUrl },
                modifier = Modifier.size(36.dp).clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = card.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StarTier(stars = card.starsCount, size = 10)
                    Text(
                        text = card.ownerLogin,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                card.platforms.forEach { kind ->
                    PlatformGlyph(kind = kind, supported = true, sizeDp = 12)
                }
            }
            OutlineButton(onClick = onClick) {
                Text(
                    text = when {
                        card.isUpdateAvailable -> "Update"
                        card.isInstalled -> "Open"
                        else -> "Get"
                    },
                )
            }
        }
    }
}
