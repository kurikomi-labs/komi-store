package zed.rainxch.core.presentation.components.overlays

import androidx.compose.runtime.Immutable

@Immutable
data class KomiToastData(
    val id: Long,
    val message: String,
    val tone: KomiToastTone = KomiToastTone.Default,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
    val sfx: String? = null,
    val durationMillis: Long? = null,
    val persistent: Boolean = false,
    val dismissible: Boolean? = null,
)
