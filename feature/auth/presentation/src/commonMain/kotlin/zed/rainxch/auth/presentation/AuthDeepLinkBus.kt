package zed.rainxch.auth.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AuthDeepLinkBus {
    private val _events =
        MutableSharedFlow<AuthDeepLinkEvent>(
            replay = 1,
            extraBufferCapacity = 4,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

    val events: SharedFlow<AuthDeepLinkEvent> = _events.asSharedFlow()

    fun publish(event: AuthDeepLinkEvent) {
        _events.tryEmit(event)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun resetReplay() {
        _events.resetReplayCache()
    }
}
