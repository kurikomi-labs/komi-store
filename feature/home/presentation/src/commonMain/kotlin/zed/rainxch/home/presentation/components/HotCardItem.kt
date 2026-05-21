package zed.rainxch.home.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.cards.CompactCard
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.vocabulary.FreshnessRing
import zed.rainxch.core.presentation.vocabulary.PlatformGlyph
import zed.rainxch.core.presentation.vocabulary.PlatformKind
import zed.rainxch.core.presentation.vocabulary.StarTier
import zed.rainxch.core.presentation.vocabulary.TopicGlyph

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HotCardItem(
    repo: DiscoveryRepositoryUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = repo.repository
    val days = daysSinceIso(r.updatedAt)

    Box(
        modifier = modifier
            .width(280.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        CompactCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FreshnessRing(daysSinceRelease = days, sizeDp = 48) {
                    GitHubStoreImage(
                        imageModel = { r.owner.avatarUrl },
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = r.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    StarTier(stars = r.stargazersCount)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (!r.description.isNullOrBlank()) {
                Text(
                    text = r.description!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp,
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                r.topics.orEmpty().take(2).forEach { topic ->
                    TopicGlyph(topic = topic, sizeDp = 14)
                }
                Box(Modifier.weight(1f))
                listOf(
                    DiscoveryPlatform.Android to PlatformKind.ANDROID,
                    DiscoveryPlatform.Windows to PlatformKind.WINDOWS,
                    DiscoveryPlatform.Macos to PlatformKind.MACOS,
                    DiscoveryPlatform.Linux to PlatformKind.LINUX,
                ).forEach { (plat, kind) ->
                    if (plat in r.availablePlatforms) {
                        PlatformGlyph(kind = kind, supported = true, sizeDp = 14)
                    }
                }
            }
        }
    }
}
