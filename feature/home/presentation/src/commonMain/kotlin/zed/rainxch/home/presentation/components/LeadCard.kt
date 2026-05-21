package zed.rainxch.home.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.buttons.PrimaryButton
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.components.cards.LeadHeroCard
import zed.rainxch.core.presentation.vocabulary.AppAccentResolver
import zed.rainxch.core.presentation.vocabulary.FreshnessRing
import zed.rainxch.core.presentation.vocabulary.Heartbeat
import zed.rainxch.core.presentation.vocabulary.StarTier
import zed.rainxch.core.presentation.vocabulary.TopicGlyph

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LeadCard(
    repo: DiscoveryRepositoryUi,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val r = repo.repository
    val isDark = isSystemInDarkTheme()
    val accent = AppAccentResolver.resolve(
        backendHex = null,
        topics = r.topics.orEmpty(),
        primaryLanguage = r.language,
    )
    val days = daysSinceIso(r.updatedAt)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        LeadHeroCard(accent = accent, isDark = isDark) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                FreshnessRing(daysSinceRelease = days, sizeDp = 80) {
                    GitHubStoreImage(
                        imageModel = { r.owner.avatarUrl },
                        modifier = Modifier.size(80.dp).clip(CircleShape),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = r.name,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 26.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = r.owner.login,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        StarTier(stars = r.stargazersCount)
                        Heartbeat(daysSinceCommit = days)
                        r.topics.orEmpty().take(3).forEach { topic ->
                            TopicGlyph(topic = topic, sizeDp = 14)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            if (!r.description.isNullOrBlank()) {
                Text(
                    text = r.description!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(14.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PrimaryButton(onClick = onClick) {
                    Text(text = ctaLabel(repo))
                }
            }
        }
    }
}

internal fun ctaLabel(repo: DiscoveryRepositoryUi): String = when {
    repo.isUpdateAvailable -> "Update"
    repo.isInstalled -> "Open"
    else -> "Get"
}
