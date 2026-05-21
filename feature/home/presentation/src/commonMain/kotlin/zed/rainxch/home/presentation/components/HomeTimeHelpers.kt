package zed.rainxch.home.presentation.components

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
internal fun daysSinceIso(isoInstant: String): Int {
    val trimmed = isoInstant.trim()
    if (trimmed.isEmpty()) return Int.MAX_VALUE
    val parsed = runCatching { Instant.parse(trimmed) }.getOrNull() ?: return Int.MAX_VALUE
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val diffMs = nowMs - parsed.toEpochMilliseconds()
    if (diffMs <= 0L) return 0
    return (diffMs / 86_400_000L).toInt()
}
