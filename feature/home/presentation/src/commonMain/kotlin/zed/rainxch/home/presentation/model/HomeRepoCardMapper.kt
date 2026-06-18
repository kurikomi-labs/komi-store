package zed.rainxch.home.presentation.model

import kotlinx.collections.immutable.toImmutableList
import zed.rainxch.core.domain.model.account.github.GithubRepoSummary
import zed.rainxch.core.presentation.model.DiscoveryRepositoryUi
import zed.rainxch.core.presentation.utils.toUi

fun HomeRepoCardUi.toDiscoveryUi(): DiscoveryRepositoryUi =
    DiscoveryRepositoryUi(
        isInstalled = isInstalled,
        isUpdateAvailable = isUpdateAvailable,
        isFavourite = isFavourite,
        isStarred = isStarred,
        isSeen = isSeen,
        isCurrentUserOwner = isCurrentUserOwner,
        repository = rawRepository,
    )

fun toHomeRepoCardUi(
    repo: GithubRepoSummary,
    isInstalled: Boolean,
    isUpdateAvailable: Boolean,
    isFavourite: Boolean,
    isStarred: Boolean,
    isSeen: Boolean,
    isCurrentUserOwner: Boolean,
): HomeRepoCardUi {
    val ui = repo.toUi()
    return HomeRepoCardUi(
        id = ui.id,
        name = ui.name,
        ownerLogin = ui.owner.login,
        ownerAvatarUrl = ui.owner.avatarUrl,
        description = ui.description.orEmpty(),
        starsCount = ui.stargazersCount,
        downloadsCount = repo.downloadCount,
        language = repo.language,
        topics = ui.topics.orEmpty().toImmutableList(),
        isInstalled = isInstalled,
        isUpdateAvailable = isUpdateAvailable,
        isFavourite = isFavourite,
        isStarred = isStarred,
        isSeen = isSeen,
        isCurrentUserOwner = isCurrentUserOwner,
        rawRepository = ui,
    )
}
