package zed.rainxch.apps.presentation.starred.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import zed.rainxch.core.presentation.components.GitHubStoreImage
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.apps.presentation.starred.StarredCandidateUi
import zed.rainxch.core.presentation.components.ExpressiveCard
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.starred_picker_already_tracked
import zed.rainxch.githubstore.core.presentation.res.starred_picker_apk_badge

@Composable
fun StarredCandidateRow(
    candidate: StarredCandidateUi,
    onClick: () -> Unit,
) {
    val a11yLabel = buildString {
        append(candidate.owner)
        append(" / ")
        append(candidate.name)
        if (candidate.hasApkRelease) append(", ships APK")
        if (candidate.isAlreadyTracked) append(", already tracked")
        candidate.latestReleaseTag?.let { append(", latest ").append(it) }
    }

    ExpressiveCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp)
                .semantics { contentDescription = a11yLabel },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            ) {
                GitHubStoreImage(
                    imageModel = { candidate.ownerAvatarUrl },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${candidate.owner}/${candidate.name}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (!candidate.description.isNullOrBlank()) {
                    Text(
                        text = candidate.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp),
                        )

                        Spacer(Modifier.width(2.dp))

                        Text(
                            text = formatStars(candidate.stargazersCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    candidate.latestReleaseTag?.let { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (candidate.hasApkRelease) {
                Badge(
                    icon = Icons.Outlined.GetApp,
                    label = stringResource(Res.string.starred_picker_apk_badge),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (candidate.isAlreadyTracked) {
                Badge(
                    icon = Icons.Outlined.CheckCircle,
                    label = stringResource(Res.string.starred_picker_already_tracked),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun Badge(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: androidx.compose.ui.graphics.Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))

        Spacer(Modifier.width(4.dp))

        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

private fun formatStars(count: Int): String = when {
    count >= 1_000_000 -> "${count / 100_000 / 10.0}M"
    count >= 1_000 -> "${count / 100 / 10.0}k"
    else -> count.toString()
}
