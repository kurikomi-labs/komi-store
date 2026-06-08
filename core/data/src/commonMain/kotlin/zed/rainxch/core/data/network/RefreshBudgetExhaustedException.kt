package zed.rainxch.core.data.network

class RefreshBudgetExhaustedException(
    val retryAfterSeconds: Long,
) : Exception("Refresh budget exhausted ($retryAfterSeconds s)")
