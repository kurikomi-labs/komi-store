package zed.rainxch.core.presentation.telemetry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.time.TimeSource

@Composable
fun TrackFirstPaint(
    isReady: Boolean,
    onFirstPaint: (elapsedMs: Long) -> Unit,
) {
    val enteredAt = remember { TimeSource.Monotonic.markNow() }
    var fired by remember { mutableStateOf(false) }
    LaunchedEffect(isReady) {
        if (!fired && isReady) {
            fired = true
            onFirstPaint(enteredAt.elapsedNow().inWholeMilliseconds)
        }
    }
}
