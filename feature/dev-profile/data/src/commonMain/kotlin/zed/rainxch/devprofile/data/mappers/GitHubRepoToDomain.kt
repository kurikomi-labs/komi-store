package zed.rainxch.devprofile.data.mappers

import zed.rainxch.devprofile.data.dto.GitHubRepoResponse
import zed.rainxch.devprofile.domain.model.DeveloperRepository

fun GitHubRepoResponse.toDomain(
    hasReleases: Boolean = false,
    hasInstallableAssets: Boolean = false,
    isInstalled: Boolean = false,
    isFavorite: Boolean = false,
    latestVersion: String? = null,
    latestReleaseAt: String? = null,
) = DeveloperRepository(
    id = id,
    name = name,
    fullName = fullName,
    description = description,
    htmlUrl = htmlUrl,
    stargazersCount = stargazersCount,
    forksCount = forksCount,
    openIssuesCount = openIssuesCount,
    language = language,
    hasReleases = hasReleases,
    hasInstallableAssets = hasInstallableAssets,
    isInstalled = isInstalled,
    isFavorite = isFavorite,
    latestVersion = latestVersion,
    latestReleaseAt = latestReleaseAt,
    updatedAt = updatedAt,
)
