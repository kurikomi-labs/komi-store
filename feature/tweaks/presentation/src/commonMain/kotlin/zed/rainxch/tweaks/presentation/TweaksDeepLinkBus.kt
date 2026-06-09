package zed.rainxch.tweaks.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object TweaksDeepLinkBus {
    private val _openFeedbackRequests =
        MutableSharedFlow<Unit>(
            replay = 1,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    // replay = 1 so a request published while navigating to Tweaks survives until
    // TweaksRoot subscribes; consume() clears it so a later revisit does not reopen.
    val openFeedbackRequests: SharedFlow<Unit> = _openFeedbackRequests.asSharedFlow()

    fun requestOpenFeedback() {
        _openFeedbackRequests.tryEmit(Unit)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun consume() {
        _openFeedbackRequests.resetReplayCache()
    }
}
