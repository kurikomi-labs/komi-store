package zed.rainxch.apps.presentation.starred

data class StarredCandidateUi(
    val repoId: Long,
    val owner: String,
    val name: String,
    val ownerAvatarUrl: String,
    val description: String?,
    val stargazersCount: Int,
    val starredAt: Long?,
    val hasApkRelease: Boolean,
    val isAlreadyTracked: Boolean,
    val latestReleaseTag: String?,
)
