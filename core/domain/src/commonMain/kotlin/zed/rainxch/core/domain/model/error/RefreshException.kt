package zed.rainxch.core.domain.model

enum class RefreshError {
    COOLDOWN,
    BUDGET_EXHAUSTED,
    ARCHIVED,
    NOT_FOUND,
    UPSTREAM,
    GENERIC,
}

class RefreshException(
    val kind: RefreshError,
    val retryAfterSeconds: Long? = null,
) : Exception("Refresh failed: $kind")
