package zed.rainxch.feed.presentation.model

import zed.rainxch.core.domain.model.account.github.GithubRepoSummary
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.utils.toUi

fun GithubRepoSummary.toDiscoveryRepoUi(
    isInstalled: Boolean,
    isUpdateAvailable: Boolean,
    isFavourite: Boolean,
    isStarred: Boolean,
    isSeen: Boolean,
    isCurrentUserOwner: Boolean,
): DiscoveryRepositoryUi =
    DiscoveryRepositoryUi(
        isInstalled = isInstalled,
        isUpdateAvailable = isUpdateAvailable,
        isFavourite = isFavourite,
        isStarred = isStarred,
        isSeen = isSeen,
        isCurrentUserOwner = isCurrentUserOwner,
        repository = toUi(),
    )
