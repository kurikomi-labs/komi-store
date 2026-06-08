package zed.rainxch.core.data.mappers

import zed.rainxch.core.data.dto.ReleaseNetwork
import zed.rainxch.core.domain.model.account.github.GithubRelease
import zed.rainxch.core.domain.model.account.github.GithubUser

fun ReleaseNetwork.toDomain(): GithubRelease =
    GithubRelease(
        id = id,
        tagName = tagName,
        name = name,
        author =
            author?.let {
                GithubUser(
                    id = it.id,
                    login = it.login,
                    avatarUrl = it.avatarUrl,
                    htmlUrl = it.htmlUrl,
                )
            },
        publishedAt = publishedAt ?: createdAt ?: "",
        description = body,
        assets = assets.map { it.toDomain() },
        tarballUrl = tarballUrl,
        zipballUrl = zipballUrl,
        htmlUrl = htmlUrl,
        isPrerelease = prerelease == true,
    )
