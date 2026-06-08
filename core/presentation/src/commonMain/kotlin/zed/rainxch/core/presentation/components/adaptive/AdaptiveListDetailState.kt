package zed.rainxch.core.presentation.components.adaptive

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class AdaptiveListDetailState(
    initial: AdaptiveDetailArgs? = null,
) {
    var currentArgs: AdaptiveDetailArgs? by mutableStateOf(initial)
        private set

    fun select(args: AdaptiveDetailArgs) {
        currentArgs = args
    }

    fun clear() {
        currentArgs = null
    }
}
