package zed.rainxch.core.domain.model

data class HiddenRepo(
    val repoId: Long,
    val repoName: String,
    val repoOwner: String,
    val repoOwnerAvatarUrl: String,
    val hiddenAt: Long,
)
