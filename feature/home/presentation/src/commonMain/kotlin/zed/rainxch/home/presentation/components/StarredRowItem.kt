package zed.rainxch.home.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.buttons.OutlineButton
import zed.rainxch.core.presentation.components.cards.RowCard
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.vocabulary.StarTier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StarredRowItem(
    repo: DiscoveryRepositoryUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = repo.repository
    val starTint = Color(0xFFC49652)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        RowCard {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(starTint.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                GitHubStoreImage(
                    imageModel = { r.owner.avatarUrl },
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = r.name,
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
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = starTint,
                        modifier = Modifier.size(12.dp),
                    )
                    StarTier(stars = r.stargazersCount, size = 10)
                }
            }
            OutlineButton(onClick = onClick) {
                Text(text = ctaLabel(repo))
            }
        }
    }
}
