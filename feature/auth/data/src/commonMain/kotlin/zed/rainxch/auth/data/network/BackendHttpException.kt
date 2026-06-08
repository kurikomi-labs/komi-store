package zed.rainxch.auth.data.network

class BackendHttpException(
    val statusCode: Int,
    message: String,
) : Exception(message)
