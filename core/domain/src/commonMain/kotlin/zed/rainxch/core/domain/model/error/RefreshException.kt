package zed.rainxch.core.domain.model.error

class RefreshException(
    val kind: RefreshError,
    val retryAfterSeconds: Long? = null,
) : Exception("Refresh failed: $kind")
