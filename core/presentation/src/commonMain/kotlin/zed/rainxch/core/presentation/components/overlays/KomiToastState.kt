package zed.rainxch.core.presentation.components.overlays

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

@Stable
class KomiToastState(
    private val limit: Int = 3,
) {
    private var counter = 0L
    val toasts = mutableStateListOf<KomiToastData>()

    fun show(
        message: String,
        tone: KomiToastTone = KomiToastTone.Default,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        sfx: String? = null,
        durationMillis: Long? = null,
        persistent: Boolean = false,
        dismissible: Boolean? = null,
    ): Long {
        val id = counter + 1
        counter = id
        toasts.add(
            KomiToastData(
                id = id,
                message = message,
                tone = tone,
                actionLabel = actionLabel,
                onAction = onAction,
                sfx = sfx,
                durationMillis = durationMillis,
                persistent = persistent,
                dismissible = dismissible,
            ),
        )
        while (toasts.size > limit) toasts.removeAt(0)
        return id
    }

    fun success(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        sfx: String? = null,
    ): Long = show(message, KomiToastTone.Success, actionLabel, onAction, sfx)

    fun warning(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ): Long = show(message, KomiToastTone.Warning, actionLabel, onAction)

    fun danger(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
        sfx: String? = null,
    ): Long = show(message, KomiToastTone.Danger, actionLabel, onAction, sfx)

    fun info(
        message: String,
        actionLabel: String? = null,
        onAction: (() -> Unit)? = null,
    ): Long = show(message, KomiToastTone.Info, actionLabel, onAction)

    fun dismiss(id: Long) {
        toasts.removeAll { it.id == id }
    }

    fun clear() {
        toasts.clear()
    }
}

@Composable
fun rememberKomiToastState(limit: Int = 3): KomiToastState = remember(limit) { KomiToastState(limit) }
