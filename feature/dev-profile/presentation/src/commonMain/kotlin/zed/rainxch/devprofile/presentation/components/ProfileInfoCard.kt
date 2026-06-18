package zed.rainxch.devprofile.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.core.presentation.utils.formatCount
import zed.rainxch.devprofile.domain.model.DeveloperProfile
import zed.rainxch.devprofile.presentation.DeveloperProfileAction
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.dev_profile_org_badge
import zed.rainxch.githubstore.core.presentation.res.followers
import zed.rainxch.githubstore.core.presentation.res.following
import zed.rainxch.githubstore.core.presentation.res.profile_repos

private val MentionRegex = Regex("(?<![A-Za-z0-9-])@([A-Za-z0-9](?:[A-Za-z0-9-]{0,38}[A-Za-z0-9])?)")

@Composable
fun ProfileInfoCard(
    profile: DeveloperProfile,
    onAction: (DeveloperProfileAction) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GitHubStoreImage(
                    imageModel = { profile.avatarUrl },
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name ?: profile.login,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (profile.isOrganization) {
                            Spacer(Modifier.width(8.dp))
                            OrgPill()
                        }
                    }
                    Text(
                        text = "@${profile.login}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            profile.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                Spacer(Modifier.height(12.dp))
                BioText(bio = bio, onMention = { user ->
                    onAction(DeveloperProfileAction.OnNavigateToUser(user))
                })
            }

            Spacer(Modifier.height(16.dp))
            MetricsStrip(
                repos = profile.publicRepos,
                followers = profile.followers,
                following = profile.following,
            )
        }
    }
}

@Composable
private fun BioText(bio: String, onMention: (String) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val annotated = remember(bio, cs.primary) {
        buildAnnotatedString {
            var cursor = 0
            for (match in MentionRegex.findAll(bio)) {
                if (match.range.first > cursor) {
                    append(bio.substring(cursor, match.range.first))
                }
                val handle = match.groupValues[1]
                withLink(
                    LinkAnnotation.Clickable(
                        tag = "mention:$handle",
                        styles = TextLinkStyles(
                            style = SpanStyle(
                                color = cs.primary,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        ),
                        linkInteractionListener = { onMention(handle) },
                    ),
                ) {
                    append("@$handle")
                }
                cursor = match.range.last + 1
            }
            if (cursor < bio.length) append(bio.substring(cursor))
        }
    }
    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 5,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun MetricsStrip(repos: Int, followers: Int, following: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Metric(value = formatCount(repos), label = stringResource(Res.string.profile_repos), modifier = Modifier.weight(1f))
        MetricDivider()
        Metric(value = formatCount(followers), label = stringResource(Res.string.followers), modifier = Modifier.weight(1f))
        MetricDivider()
        Metric(value = formatCount(following), label = stringResource(Res.string.following), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    )
}

@Composable
private fun OrgPill() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            text = stringResource(Res.string.dev_profile_org_badge),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IdentityRailCard(
    profile: DeveloperProfile,
    onAction: (DeveloperProfileAction) -> Unit,
) {
    val hasAny = profile.company?.isNotBlank() == true ||
        profile.location?.isNotBlank() == true ||
        profile.blog?.isNotBlank() == true ||
        profile.twitterUsername?.isNotBlank() == true
    if (!hasAny) return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            profile.company?.takeIf { it.isNotBlank() }?.let { company ->
                val trimmed = company.trim()
                if (trimmed.startsWith("@") && trimmed.length > 1) {
                    val handle = trimmed.removePrefix("@")
                    LinkChip(
                        icon = Icons.Default.Business,
                        text = trimmed,
                        onClick = { onAction(DeveloperProfileAction.OnNavigateToUser(handle)) },
                    )
                } else {
                    StaticChip(icon = Icons.Default.Business, text = trimmed)
                }
            }
            profile.location?.takeIf { it.isNotBlank() }?.let { location ->
                StaticChip(icon = Icons.Default.LocationOn, text = location)
            }
            profile.blog?.takeIf { it.isNotBlank() }?.let { blog ->
                val display = blog.removePrefix("https://").removePrefix("http://")
                LinkChip(
                    icon = Icons.Default.Link,
                    text = display,
                    onClick = {
                        val url = if (!blog.startsWith("http")) "https://$blog" else blog
                        onAction(DeveloperProfileAction.OnOpenLink(url))
                    },
                )
            }
            profile.twitterUsername?.takeIf { it.isNotBlank() }?.let { twitter ->
                LinkChip(
                    icon = Icons.Default.Tag,
                    text = "@$twitter",
                    onClick = {
                        onAction(DeveloperProfileAction.OnOpenLink("https://twitter.com/$twitter"))
                    },
                )
            }
        }
    }
}

@Composable
private fun StaticChip(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LinkChip(icon: ImageVector, text: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
