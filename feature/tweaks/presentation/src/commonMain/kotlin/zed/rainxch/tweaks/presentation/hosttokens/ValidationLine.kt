package zed.rainxch.tweaks.presentation.hosttokens

data class ValidationLine(
    val login: String?,
    val scopes: List<String>,
    val rateLimitRemaining: Int?,
    val errorMessage: String?,
) {
    val isSuccess: Boolean get() = errorMessage == null
}
