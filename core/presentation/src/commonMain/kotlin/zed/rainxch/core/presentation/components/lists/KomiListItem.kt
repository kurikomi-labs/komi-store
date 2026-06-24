package zed.rainxch.core.presentation.components.lists

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class KomiListItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val subtitle: String? = null,
    val trailing: KomiListTrailing = KomiListTrailing.Chevron,
    val destructive: Boolean = false,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val onLongClick: (() -> Unit)? = null,
)
