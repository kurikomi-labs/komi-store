package zed.rainxch.core.domain.model.account.github

data class GithubDeviceTokenError(
    val error: String,
    val errorDescription: String? = null,
)
