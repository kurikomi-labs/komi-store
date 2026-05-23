package zed.rainxch.devprofile.data.mappers

import zed.rainxch.core.data.dto.GithubRepoNetworkModel
import zed.rainxch.devprofile.data.dto.GitHubRepoResponse

fun GithubRepoNetworkModel.toGitHubRepoResponse(): GitHubRepoResponse =
    GitHubRepoResponse(
        id = id,
        name = name,
        fullName = fullName,
        description = description,
        htmlUrl = htmlUrl,
        stargazersCount = stargazersCount,
        forksCount = forksCount,
        openIssuesCount = openIssuesCount,
        language = language,
        updatedAt = updatedAt,
        hasDownloads = hasDownloads,
        archived = archived,
        fork = fork,
    )
