package zed.rainxch.home.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import zed.rainxch.core.domain.model.DiscoveryPlatform
import zed.rainxch.core.presentation.components.overlays.GhsDropdownMenu
import zed.rainxch.core.presentation.utils.toLabel

@Composable
fun PlatformFilterMenu(
    expanded: Boolean,
    selectedPlatforms: Set<DiscoveryPlatform>,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onToggle: (DiscoveryPlatform) -> Unit,
) {
    GhsDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        PlatformRow(
            label = DiscoveryPlatform.All.toLabel(),
            isSelected = selectedPlatforms.isEmpty(),
            onClick = onSelectAll,
        )
        DiscoveryPlatform.selectablePlatforms.forEach { platform ->
            PlatformRow(
                label = platform.toLabel(),
                isSelected = platform in selectedPlatforms,
                onClick = { onToggle(platform) },
            )
        }
    }
}

@Composable
private fun PlatformRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
