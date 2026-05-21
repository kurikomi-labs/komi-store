package zed.rainxch.home.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.cards.CompactCard
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.vocabulary.AppAccentResolver
import zed.rainxch.core.presentation.vocabulary.FreshnessRing
import zed.rainxch.core.presentation.vocabulary.PlatformGlyph
import zed.rainxch.core.presentation.vocabulary.PlatformKind
import zed.rainxch.core.presentation.vocabulary.StarTier
import zed.rainxch.core.presentation.vocabulary.TopicGlyph
import zed.rainxch.core.presentation.vocabulary.freshnessOf

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
    val isDark = isSystemInDarkTheme()
    val accent = AppAccentResolver.resolve(
        backendHex = null,
        topics = r.topics.orEmpty(),
        primaryLanguage = r.language,
    )
    val freshness = freshnessOf(days)

    Box(
        modifier = modifier
            .width(260.dp)
            .height(220.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        CompactCard {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FreshnessRing(
                    daysSinceRelease = days,
                    sizeDp = 44,
                    color = accent.c,
                ) {
                    GitHubStoreImage(
                        imageModel = { r.owner.avatarUrl },
                        modifier = Modifier.size(44.dp).clip(CircleShape),
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
                    Spacer(Modifier.height(2.dp))
                    StarTier(stars = r.stargazersCount)
                }
                DaysAgoPill(days = days, color = freshness.color)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = r.description.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.height(36.dp),
            )
            Spacer(Modifier.height(10.dp))
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
    // Suppress unused dark-mode for now; reserved for future accent-bloom tweak.
    @Suppress("UNUSED_EXPRESSION") isDark
}

@Composable
private fun DaysAgoPill(days: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(5.dp).clip(CircleShape).background(color),
        )
        Text(
            text = "${days}d",
            color = color,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
            ),
        )
    }
}
