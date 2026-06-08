package zed.rainxch.core.presentation.utils

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import zed.rainxch.githubstore.core.presentation.res.*

@Composable
fun formatCount(count: Int): String =
    when {
        count >= 1_000_000 -> stringResource(Res.string.count_millions, count / 1_000_000)
        count >= 1000 -> stringResource(Res.string.count_thousands, count / 1000)
        else -> count.toString()
    }

@Composable
fun formatCount(count: Long): String =
    when {
        count >= 1_000_000L -> stringResource(Res.string.count_millions, (count / 1_000_000L).toInt())
        count >= 1000L -> stringResource(Res.string.count_thousands, (count / 1000L).toInt())
        else -> count.toString()
    }

fun formatCompactCount(count: Long): String =
    when {
        count >= 1_000_000L -> "${count / 100_000L / 10.0}M"
        count >= 1_000L -> "${count / 100L / 10.0}k"
        else -> count.toString()
    }

fun formatCompactCount(count: Int): String = formatCompactCount(count.toLong())
