package zed.rainxch.core.presentation.model

data class DiscoveryRepositoryUi(
    val isInstalled: Boolean,
    val isUpdateAvailable: Boolean,
    val isFavourite: Boolean,
    val isStarred: Boolean,
    val isSeen: Boolean = false,
    val isCurrentUserOwner: Boolean = false,
    val repository: GithubRepoSummaryUi,
)
