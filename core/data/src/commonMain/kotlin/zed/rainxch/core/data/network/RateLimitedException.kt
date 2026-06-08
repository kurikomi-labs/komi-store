package zed.rainxch.core.data.network

class RateLimitedException(
    val retryAfterSeconds: Long? = null,
    val resetEpochSeconds: Long? = null,
    val limit: Int? = null,
) : Exception("Rate limited by backend (429)")
