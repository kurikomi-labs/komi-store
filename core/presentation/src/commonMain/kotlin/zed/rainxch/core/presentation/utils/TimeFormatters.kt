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

    val normalized = runCatching {
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
fun hasWeekNotPassed(isoInstant: String): Boolean {
    val updated = parseIsoInstantLenient(isoInstant) ?: return false
    val now = Clock.System.now()
    val diff = now - updated

    return diff < 7.days
}

@OptIn(ExperimentalTime::class)
@Composable
fun formatReleasedAt(isoInstant: String): String {
    val updated = parseIsoInstantLenient(isoInstant)
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
