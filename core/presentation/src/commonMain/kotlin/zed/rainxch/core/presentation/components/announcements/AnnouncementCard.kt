package zed.rainxch.core.presentation.components.announcements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import zed.rainxch.core.presentation.components.buttons.GhsButton
import zed.rainxch.core.presentation.components.buttons.GhsButtonSize
import zed.rainxch.core.presentation.components.buttons.GhsButtonVariant
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.announcement.Announcement
import zed.rainxch.core.domain.model.announcement.AnnouncementCategory
import zed.rainxch.core.domain.model.announcement.AnnouncementIconHint
import zed.rainxch.core.domain.model.announcement.AnnouncementSeverity
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.announcements_acknowledge
import zed.rainxch.githubstore.core.presentation.res.announcements_acknowledged
import zed.rainxch.githubstore.core.presentation.res.announcements_read_more
import zed.rainxch.githubstore.core.presentation.res.dismiss

private const val BODY_COLLAPSED_LINES = 4

@Composable
fun AnnouncementCard(
    announcement: Announcement,
    isAcknowledged: Boolean,
    onCtaClick: () -> Unit,
    onDismissClick: () -> Unit,
    onAcknowledgeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val severityColor = severityAccent(announcement.severity)
    val containerColor = when (announcement.severity) {
        AnnouncementSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer
        AnnouncementSeverity.IMPORTANT -> MaterialTheme.colorScheme.surfaceContainerHigh
        AnnouncementSeverity.INFO -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(severityColor),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeaderRow(
                    severity = announcement.severity,
                    category = announcement.category,
                    iconHint = announcement.iconHint,
                    severityColor = severityColor,
                    isAcknowledged = isAcknowledged,
                )

                Text(
                    text = announcement.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )

                ExpandableBody(announcement.body)

                ActionRow(
                    announcement = announcement,
                    isAcknowledged = isAcknowledged,
                    onCtaClick = onCtaClick,
                    onDismissClick = onDismissClick,
                    onAcknowledgeClick = onAcknowledgeClick,
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(
    severity: AnnouncementSeverity,
    category: AnnouncementCategory,
    iconHint: AnnouncementIconHint?,
    severityColor: Color,
    isAcknowledged: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = severityIcon(severity, iconHint),
            contentDescription = stringResource(severityLabel(severity)),
            tint = severityColor,
            modifier = Modifier.size(20.dp),
        )
        CategoryChip(category = category)
        Spacer(Modifier.weight(1f))
        if (isAcknowledged) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(Res.string.announcements_acknowledged),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CategoryChip(category: AnnouncementCategory) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Text(
            text = stringResource(categoryLabel(category)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ExpandableBody(body: String) {
    var expanded by remember(body) { mutableStateOf(false) }
    var isOverflowing by remember(body) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (expanded) Int.MAX_VALUE else BODY_COLLAPSED_LINES,
            onTextLayout = { layout ->
                if (!expanded) {
                    isOverflowing = layout.hasVisualOverflow
                }
            },
        )
        if (!expanded && isOverflowing) {
            GhsButton(
                onClick = { expanded = true },
                label = stringResource(Res.string.announcements_read_more),
                variant = GhsButtonVariant.Text,
                size = GhsButtonSize.Sm,
            )
        }
    }
}

@Composable
private fun ActionRow(
    announcement: Announcement,
    isAcknowledged: Boolean,
    onCtaClick: () -> Unit,
    onDismissClick: () -> Unit,
    onAcknowledgeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!announcement.ctaUrl.isNullOrBlank()) {
            val resolvedLabel = announcement.ctaLabel?.takeIf { it.isNotBlank() }
                ?: stringResource(Res.string.announcements_read_more)
            GhsButton(
                onClick = onCtaClick,
                label = resolvedLabel,
                variant = GhsButtonVariant.Text,
                size = GhsButtonSize.Sm,
                leadingIcon = Icons.AutoMirrored.Filled.OpenInNew,
            )
        }
        Spacer(Modifier.weight(1f))
        if (announcement.requiresAcknowledgment && !isAcknowledged) {
            GhsButton(
                onClick = onAcknowledgeClick,
                label = stringResource(Res.string.announcements_acknowledge),
                variant = GhsButtonVariant.Text,
                size = GhsButtonSize.Sm,
            )
        } else if (announcement.dismissible) {
            GhsButton(
                onClick = onDismissClick,
                label = stringResource(Res.string.dismiss),
                variant = GhsButtonVariant.Text,
                size = GhsButtonSize.Sm,
            )
        }
    }
}

@Composable
private fun severityAccent(severity: AnnouncementSeverity): Color = when (severity) {
    AnnouncementSeverity.CRITICAL -> MaterialTheme.colorScheme.error
    AnnouncementSeverity.IMPORTANT -> MaterialTheme.colorScheme.primary
    AnnouncementSeverity.INFO -> MaterialTheme.colorScheme.tertiary
}

private fun severityIcon(severity: AnnouncementSeverity, hint: AnnouncementIconHint?): ImageVector {
    if (hint != null) {
        return when (hint) {
            AnnouncementIconHint.INFO -> Icons.Filled.Info
            AnnouncementIconHint.WARNING -> Icons.Filled.Warning
            AnnouncementIconHint.SECURITY -> Icons.Filled.Security
            AnnouncementIconHint.CELEBRATION -> Icons.Filled.Campaign
            AnnouncementIconHint.CHANGE -> Icons.Filled.Campaign
        }
    }
    return when (severity) {
        AnnouncementSeverity.CRITICAL -> Icons.Filled.Security
        AnnouncementSeverity.IMPORTANT -> Icons.Filled.Warning
        AnnouncementSeverity.INFO -> Icons.Filled.Info
    }
}

