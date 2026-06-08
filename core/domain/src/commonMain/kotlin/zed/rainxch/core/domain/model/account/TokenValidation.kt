package zed.rainxch.core.domain.model

data class TokenValidation(
    val login: String?,
    val scopes: List<String>,
    val rateLimitRemaining: Int?,
)
