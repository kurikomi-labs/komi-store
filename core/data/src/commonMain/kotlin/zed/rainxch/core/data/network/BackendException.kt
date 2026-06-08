package zed.rainxch.core.data.network

class BackendException(
    val statusCode: Int,
    message: String = "HTTP $statusCode",
) : Exception(message)
