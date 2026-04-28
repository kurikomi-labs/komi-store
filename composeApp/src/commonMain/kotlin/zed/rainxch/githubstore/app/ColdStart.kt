package zed.rainxch.githubstore.app

import kotlin.time.TimeSource

object ColdStart {
    private var mark: TimeSource.Monotonic.ValueTimeMark? = null
    private var consumed: Boolean = false

    fun markStart() {
        if (mark == null) mark = TimeSource.Monotonic.markNow()
    }

    // Returns elapsed ms on the first call after [markStart], null thereafter.
    // Single-process, single-shot — App composable consumes once on first frame.
    fun consumeIfFirst(): Long? {
        if (consumed) return null
        consumed = true
        return mark?.elapsedNow()?.inWholeMilliseconds
    }

    fun elapsedSeconds(): Long? = mark?.elapsedNow()?.inWholeSeconds
}
