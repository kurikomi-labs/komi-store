package zed.rainxch.core.presentation.utils

import androidx.compose.runtime.Composable
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.*
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
private fun parseIsoInstantLenient(isoInstant: String): Instant? {
    val trimmed = isoInstant.trim()
    if (trimmed.isEmpty()) return null
    runCatching { return Instant.parse(trimmed) }

    val normalized =
        runCatching {
            val tzStart = trimmed.indexOfAny(charArrayOf('Z', '+', '-'), startIndex = 11)
            if (tzStart < 0) return@runCatching null
            val head = trimmed.substring(0, tzStart)
            val tail = trimmed.substring(tzStart)
            val colonCount = head.count { it == ':' }
            when (colonCount) {
                1 -> head + ":00" + tail
                0 -> head + ":00:00" + tail
                else -> null
            }
        }.getOrNull() ?: return null

    return runCatching { Instant.parse(normalized) }.getOrNull()
}

@OptIn(ExperimentalTime::class)
fun daysSinceIso(isoInstant: String?): Int? {
    val instant = isoInstant?.let { parseIsoInstantLenient(it) } ?: return null
    return (Clock.System.now() - instant).inWholeDays.toInt()
}

@OptIn(ExperimentalTime::class)
fun formatRelativeShort(isoInstant: String?): String {
    val instant = isoInstant?.let { parseIsoInstantLenient(it) } ?: return ""
    val diffMs = Clock.System.now().toEpochMilliseconds() - instant.toEpochMilliseconds()
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

@OptIn(ExperimentalTime::class)
@Composable
fun formatRelativeLong(isoInstant: String): String {
    val instant = parseIsoInstantLenient(isoInstant) ?: return isoInstant
    val duration = Clock.System.now() - instant
    return when {
        duration.inWholeDays > 365 -> {
            stringResource(Res.string.time_years_ago, (duration.inWholeDays / 365).toInt())
        }

        duration.inWholeDays > 30 -> {
            stringResource(Res.string.time_months_ago, (duration.inWholeDays / 30).toInt())
        }

        duration.inWholeDays > 0 -> {
            stringResource(Res.string.time_days_ago, duration.inWholeDays.toInt())
        }

        duration.inWholeHours > 0 -> {
            stringResource(Res.string.time_hours_ago, duration.inWholeHours.toInt())
        }

        duration.inWholeMinutes > 0 -> {
            stringResource(Res.string.time_minutes_ago, duration.inWholeMinutes.toInt())
        }

        else -> {
            stringResource(Res.string.just_now)
        }
    }
}

@OptIn(ExperimentalTime::class)
fun formatIsoDate(isoTimestamp: String?): String? {
    val instant = isoTimestamp?.let { parseIsoInstantLenient(it) } ?: return null
    return instant.toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
}

@OptIn(ExperimentalTime::class)
fun formatEpochDate(timestamp: Long): String? {
    if (timestamp <= 0L) return null
    return Instant
        .fromEpochMilliseconds(timestamp)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}

@OptIn(ExperimentalTime::class)
@Composable
fun formatLastChecked(timestamp: Long): String {
    val diffMs = Clock.System.now().toEpochMilliseconds() - timestamp
    val minutes = diffMs / 60_000L
    val hours = diffMs / 3_600_000L
    return when {
        minutes < 1 -> stringResource(Res.string.last_checked_just_now)
        minutes < 60 -> stringResource(Res.string.last_checked_minutes_ago, minutes.toInt())
        else -> stringResource(Res.string.last_checked_hours_ago, hours.toInt())
    }
}

@OptIn(ExperimentalTime::class)
fun hasWeekNotPassed(isoInstant: String): Boolean {
    val updated = parseIsoInstantLenient(isoInstant) ?: return false
    val now = Clock.System.now()
    val diff = now - updated

    return diff < 7.days
}

@OptIn(ExperimentalTime::class)
@Composable
fun formatReleasedAt(isoInstant: String): String {
    val updated =
        parseIsoInstantLenient(isoInstant)
            ?: return isoInstant.trim().substringBefore('T').ifBlank { "" }
    val now = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds())
    val diff: Duration = now - updated

    val hoursDiff = diff.inWholeHours
    val daysDiff = diff.inWholeDays

    return when {
        hoursDiff < 1 -> {
            stringResource(Res.string.released_just_now)
        }

        hoursDiff < 24 -> {
            stringResource(Res.string.released_hours_ago, hoursDiff)
        }

        daysDiff == 1L -> {
            stringResource(Res.string.released_yesterday)
        }

        daysDiff < 7 -> {
            stringResource(Res.string.released_days_ago, daysDiff)
        }

        else -> {
            val date = updated.toLocalDateTime(TimeZone.currentSystemDefault()).date
            stringResource(Res.string.released_on_date, date.toString())
        }
    }
}

@OptIn(ExperimentalTime::class)
suspend fun formatAddedAt(epochMillis: Long): String {
    val updated = Instant.fromEpochMilliseconds(epochMillis)
    val now = Clock.System.now()
    val diff: Duration = now - updated

    val hoursDiff = diff.inWholeHours
    val daysDiff = diff.inWholeDays

    return when {
        hoursDiff < 1 -> {
            getString(Res.string.added_just_now)
        }

        hoursDiff < 24 -> {
            getString(Res.string.added_hours_ago, hoursDiff)
        }

        daysDiff == 1L -> {
            getString(Res.string.added_yesterday)
        }

        daysDiff < 7 -> {
            getString(Res.string.added_days_ago, daysDiff)
        }

        else -> {
            val date =
                updated
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
            getString(Res.string.added_on_date, date.toString())
        }
    }
}
