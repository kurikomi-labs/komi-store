package zed.rainxch.core.presentation.components.buttons

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class KomiActionRowItem(
    val icon: ImageVector,
    val title: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
    val variant: KomiButtonVariant = KomiButtonVariant.Tonal,
)
