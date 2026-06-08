package zed.rainxch.core.presentation.components.announcements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.core.domain.model.announcement.AnnouncementCategory
import zed.rainxch.githubstore.core.presentation.res.Res
import zed.rainxch.githubstore.core.presentation.res.announcements_mute_locked
import zed.rainxch.githubstore.core.presentation.res.announcements_mute_settings_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuteSettingsBottomSheet(
    mutedCategories: Set<AnnouncementCategory>,
    onToggle: (AnnouncementCategory, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(PaddingValues(horizontal = 24.dp, vertical = 8.dp)),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.announcements_mute_settings_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            AnnouncementCategory.entries.forEach { category ->
                val locked = !category.isMutable
                CategoryRow(
                    category = category,
                    label = stringResource(categoryLabel(category)),
                    enabled = if (locked) true else category !in mutedCategories,
                    locked = locked,
                    onToggle = { newEnabled -> onToggle(category, !newEnabled) },
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoryRow(
    category: AnnouncementCategory,
    label: String,
    enabled: Boolean,
    locked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val rowModifier = Modifier
        .fillMaxWidth()
        .semantics(mergeDescendants = true) {}
        .let { base ->
            if (locked) {
                base
            } else {
                base.toggleable(
                    value = enabled,
                    role = Role.Switch,
                    onValueChange = onToggle,
                )
            }
        }
    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (locked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = stringResource(Res.string.announcements_mute_locked),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = null,
            enabled = !locked,
        )
    }
}

