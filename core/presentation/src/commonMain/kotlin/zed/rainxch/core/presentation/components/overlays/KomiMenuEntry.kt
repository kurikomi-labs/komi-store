package zed.rainxch.core.presentation.components.overlays

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

sealed interface KomiMenuEntry

@Immutable
data class KomiMenuItem(
    val id: String,
    val label: String,
    val icon: ImageVector? = null,
    val tone: KomiMenuTone = KomiMenuTone.Default,
    val enabled: Boolean = true,
) : KomiMenuEntry

data object KomiMenuDivider : KomiMenuEntry
