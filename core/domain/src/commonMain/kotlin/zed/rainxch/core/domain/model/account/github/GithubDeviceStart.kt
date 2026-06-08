package zed.rainxch.core.domain.model.account.github

data class GithubDeviceStart(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String? = null,
    val intervalSec: Int = 5,
    val expiresInSec: Int,
)
