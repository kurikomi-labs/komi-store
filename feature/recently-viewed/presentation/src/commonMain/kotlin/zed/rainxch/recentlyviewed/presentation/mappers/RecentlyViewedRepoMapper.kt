package zed.rainxch.recentlyviewed.presentation.mappers

import zed.rainxch.core.domain.model.repository.SeenRepo
import zed.rainxch.core.presentation.utils.formatAddedAt
import zed.rainxch.recentlyviewed.presentation.model.RecentlyViewedRepo

suspend fun SeenRepo.toRecentlyViewedRepoUi(): RecentlyViewedRepo =
    RecentlyViewedRepo(
        repoId = repoId,
        repoName = repoName,
        repoOwner = repoOwner,
        repoOwnerAvatarUrl = repoOwnerAvatarUrl,
        repoDescription = repoDescription,
        primaryLanguage = primaryLanguage,
        repoUrl = repoUrl,
        viewedAtFormatted = formatAddedAt(seenAt),
    )
