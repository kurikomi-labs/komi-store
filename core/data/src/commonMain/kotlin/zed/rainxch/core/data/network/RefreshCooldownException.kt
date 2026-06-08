package zed.rainxch.core.data.network

class RefreshCooldownException(
    val retryAfterSeconds: Long,
) : Exception("Refresh cooldown ($retryAfterSeconds s)")
