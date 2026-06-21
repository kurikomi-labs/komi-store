package zed.rainxch.profile.presentation.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.announcement.Announcement
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.components.icon.KomiIcon
import zed.rainxch.core.presentation.components.overlays.KomiDialog
import zed.rainxch.core.presentation.components.text.KomiText
import zed.rainxch.core.presentation.components.text.KomiTextRole
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.announcements_acknowledge
import zed.rainxch.githubstore.core.presentation.res.announcements_view_details

@Composable
fun CriticalAnnouncementModal(
    announcement: Announcement,
    onAcknowledge: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    val colors = LocalPersonality.current.colors
    KomiDialog(
        onDismissRequest = { },
        icon = {
            KomiIcon(
                imageVector = Icons.Filled.Security,
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(40.dp),
            )
        },
        title = {
            KomiText(
                text = announcement.title,
                role = KomiTextRole.Display,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
                uppercase = false,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KomiText(
                    text = announcement.body,
                    role = KomiTextRole.Body,
                    color = colors.onSurface,
                    uppercase = false,
                )
                Spacer(Modifier.height(0.dp))
            }
        },
        confirmButton = {
            KomiButton(
                onClick = onAcknowledge,
                label = stringResource(Res.string.announcements_acknowledge),
                variant = KomiButtonVariant.Destructive,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        dismissButton =
            if (!announcement.ctaUrl.isNullOrBlank()) {
                {
                    KomiButton(
                        onClick = onOpenDetails,
                        label =
                            announcement.ctaLabel
                                ?: stringResource(Res.string.announcements_view_details),
                        variant = KomiButtonVariant.Text,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                    )
                }
            } else {
                null
            },
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
    )
}
