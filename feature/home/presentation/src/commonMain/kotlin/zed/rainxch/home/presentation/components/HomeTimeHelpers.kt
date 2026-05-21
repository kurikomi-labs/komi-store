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

@OptIn(ExperimentalTime::class)
internal fun relativeAgo(isoInstant: String): String {
    val trimmed = isoInstant.trim()
    if (trimmed.isEmpty()) return ""
    val parsed = runCatching { Instant.parse(trimmed) }.getOrNull() ?: return ""
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val diffMs = nowMs - parsed.toEpochMilliseconds()
    if (diffMs <= 0L) return "now"
    val minutes = diffMs / 60_000L
    if (minutes < 1L) return "now"
    if (minutes < 60L) return "${minutes}m"
    val hours = minutes / 60L
    if (hours < 24L) return "${hours}h"
    val days = hours / 24L
    if (days < 30L) return "${days}d"
    val months = days / 30L
    if (months < 12L) return "${months}mo"
    val years = days / 365L
    return "${years}y"
}
