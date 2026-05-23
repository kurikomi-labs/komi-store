@file:OptIn(ExperimentalTime::class)

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
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.chips.StatChip
import zed.rainxch.core.presentation.theme.GithubStoreTheme
import zed.rainxch.core.presentation.theme.tokens.Radii
import zed.rainxch.core.presentation.utils.formatCount
import zed.rainxch.devprofile.domain.model.DeveloperRepository
import zed.rainxch.githubstore.core.presentation.res.*
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperRepoItem(
    repository: DeveloperRepository,
    onItemClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Radii.row,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
        onClick = onItemClick,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repository.name,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(
                            resource = Res.string.updated_on_date,
                            formatRelativeDate(repository.updatedAt),
                        ).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                FavoriteToggle(
                    isFavorite = repository.isFavorite,
                    onClick = onToggleFavorite,
                )
            }

            repository.description?.takeIf { it.isNotBlank() }?.let { description ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = description,
                    maxLines = 2,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (repository.stargazersCount > 0) {
                    StatChip(
                        label = formatCount(repository.stargazersCount),
                        leading = {
                            Icon(
                                imageVector = Icons.Outlined.StarOutline,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                if (repository.forksCount > 0) {
                    StatChip(
                        label = formatCount(repository.forksCount),
                        leading = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.CallSplit,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                if (repository.openIssuesCount > 0) {
                    StatChip(
                        label = formatCount(repository.openIssuesCount),
                        leading = {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                repository.language?.takeIf { it.isNotBlank() }?.let { language ->
                    StatChip(
                        label = language,
                        leading = {
                            Icon(
                                imageVector = Icons.Outlined.Code,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
            }

            val showBadges = repository.hasInstallableAssets || repository.isInstalled
            if (showBadges) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (repository.hasInstallableAssets) {
                        TonalBadge(
                            text = repository.latestVersion
                                ?: stringResource(Res.string.has_release),
                            container = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            content = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (repository.isInstalled) {
                        TonalBadge(
                            text = stringResource(Res.string.installed),
                            container = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f),
                            content = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteToggle(
    isFavorite: Boolean,
    onClick: () -> Unit,
) {
    val container = if (isFavorite) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val tint = if (isFavorite) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(
                if (isFavorite) Res.string.remove_from_favourites
                else Res.string.add_to_favourites,
            ),
            modifier = Modifier.size(18.dp),
            tint = tint,
        )
    }
}

@Composable
private fun TonalBadge(text: String, container: Color, content: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun formatRelativeDate(dateString: String): String {
    val instant = try {
        Instant.parse(dateString)
    } catch (_: IllegalArgumentException) {
        return dateString
    }
    val now = Clock.System.now()
    val duration = now - instant
    return when {
        duration.inWholeDays > 365 ->
            stringResource(Res.string.time_years_ago, (duration.inWholeDays / 365).toInt())
        duration.inWholeDays > 30 ->
            stringResource(Res.string.time_months_ago, (duration.inWholeDays / 30).toInt())
        duration.inWholeDays > 0 ->
            stringResource(Res.string.time_days_ago, duration.inWholeDays.toInt())
        duration.inWholeHours > 0 ->
            stringResource(Res.string.time_hours_ago, duration.inWholeHours.toInt())
        duration.inWholeMinutes > 0 ->
            stringResource(Res.string.time_minutes_ago, duration.inWholeMinutes.toInt())
        else -> stringResource(Res.string.just_now)
    }
}

@Preview
@Composable
private fun PreviewDeveloperRepoItem() {
    GithubStoreTheme {
        DeveloperRepoItem(
            repository = DeveloperRepository(
                id = 1,
                name = "awesome-kotlin-app",
                fullName = "developer/awesome-kotlin-app",
                description = "An amazing Kotlin Multiplatform application that demonstrates modern Android development",
                htmlUrl = "",
                stargazersCount = 2340,
                forksCount = 456,
                openIssuesCount = 23,
                language = "Kotlin",
                hasReleases = true,
                hasInstallableAssets = true,
                isInstalled = true,
                isFavorite = false,
                latestVersion = "v1.5.2",
                updatedAt = Clock.System.now().toString(),
            ),
            onItemClick = {},
            onToggleFavorite = {},
        )
    }
}
