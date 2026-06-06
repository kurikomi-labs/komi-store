package zed.rainxch.home.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.GitHubStoreImage
import zed.rainxch.core.presentation.components.overlays.GhsBottomSheet
import zed.rainxch.core.presentation.model.GithubRepoSummaryUi
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.hide_repository
import zed.rainxch.githubstore.core.presentation.res.mark_as_unviewed
import zed.rainxch.githubstore.core.presentation.res.mark_as_viewed
import zed.rainxch.githubstore.core.presentation.res.open_on_github
import zed.rainxch.githubstore.core.presentation.res.share_repository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryActionsSheet(
    repository: GithubRepoSummaryUi,
    isSeen: Boolean,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOpenOnGithub: () -> Unit,
    onToggleSeen: () -> Unit,
    onHide: () -> Unit,
) {
    GhsBottomSheet(onDismissRequest = onDismiss) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GitHubStoreImage(
                imageModel = { repository.owner.avatarUrl },
                modifier = Modifier.size(36.dp).clip(CircleShape),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = repository.fullName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                repository.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SheetRow(
            label = stringResource(Res.string.share_repository),
            icon = Icons.Default.Share,
            onClick = onShare,
        )

        SheetRow(
            label = stringResource(Res.string.open_on_github),
            icon = Icons.Default.OpenInBrowser,
            onClick = onOpenOnGithub,
        )

        SheetRow(
            label = if (isSeen) stringResource(Res.string.mark_as_unviewed)
            else stringResource(Res.string.mark_as_viewed),
            icon = Icons.Outlined.Visibility,
            onClick = onToggleSeen,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        SheetRow(
            label = stringResource(Res.string.hide_repository),
            icon = Icons.Default.VisibilityOff,
            onClick = onHide,
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun SheetRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)

        Text(
            text = label,
            color = tint,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
