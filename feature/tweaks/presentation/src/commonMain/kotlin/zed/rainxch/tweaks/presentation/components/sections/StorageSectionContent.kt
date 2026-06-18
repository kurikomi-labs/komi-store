package zed.rainxch.tweaks.presentation.components.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.presentation.components.buttons.KomiButton
import zed.rainxch.core.presentation.components.buttons.KomiButtonVariant
import zed.rainxch.core.presentation.locals.LocalPersonality
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.tweaks_storage_downloaded_apks_body
import zed.rainxch.githubstore.core.presentation.res.tweaks_storage_downloaded_apks_clear
import zed.rainxch.githubstore.core.presentation.res.tweaks_storage_downloaded_apks_title
import zed.rainxch.githubstore.core.presentation.res.tweaks_storage_empty_size
import zed.rainxch.githubstore.core.presentation.res.tweaks_storage_using_label
import zed.rainxch.tweaks.presentation.TweaksAction
import zed.rainxch.tweaks.presentation.TweaksState

@Composable
fun storageSectionContent(
    state: TweaksState,
    onAction: (TweaksAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DownloadsCard(
            cacheSize = state.cacheSize,
            onClearClick = { onAction(TweaksAction.OnClearCacheClick) },
        )
    }
}

@Composable
private fun DownloadsCard(
    cacheSize: String,
    onClearClick: () -> Unit,
) {
    val emptySize = stringResource(Res.string.tweaks_storage_empty_size)
    val sizeDisplay = cacheSize.ifBlank { emptySize }
    val isEmpty = sizeDisplay == emptySize

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(LocalPersonality.current.shape.corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(LocalPersonality.current.shape.cornerSmall))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Inventory2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = stringResource(Res.string.tweaks_storage_downloaded_apks_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(Res.string.tweaks_storage_downloaded_apks_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.tweaks_storage_using_label, sizeDisplay),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            KomiButton(
                onClick = onClearClick,
                label = stringResource(Res.string.tweaks_storage_downloaded_apks_clear),
                variant = KomiButtonVariant.Destructive,
                enabled = !isEmpty,
                leadingIcon = Icons.Outlined.DeleteOutline,
            )
        }
    }
}
