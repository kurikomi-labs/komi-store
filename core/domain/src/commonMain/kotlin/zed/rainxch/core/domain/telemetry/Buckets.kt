package zed.rainxch.core.domain.telemetry

object TelemetryBuckets {
    fun durationMs(ms: Long): String = when {
        ms < 500 -> "<500"
        ms < 1000 -> "500-1000"
        ms < 3000 -> "1000-3000"
        else -> ">3000"
    }

    fun resultCount(n: Int): String = when {
        n < 0 -> "invalid"
        n == 0 -> "0"
        n <= 5 -> "1-5"
        n <= 20 -> "6-20"
        else -> ">20"
    }

    fun confidence(score: Float): String = when {
        score >= 0.85f -> "high"
        score >= 0.5f -> "medium"
        else -> "low"
    }
}
