package zed.rainxch.core.presentation.components.buttons

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

@Immutable
data class KomiSegmentedItem<T>(
    val value: T,
    val icon: ImageVector? = null,
    val title: String? = null,
)
