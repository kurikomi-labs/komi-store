package zed.rainxch.core.data.mappers

import zed.rainxch.core.data.dto.UserProfileNetwork
import zed.rainxch.core.domain.model.account.UserProfile

fun UserProfileNetwork.toUserProfile(): UserProfile =
    UserProfile(
        id = id.toInt(),
        imageUrl = avatarUrl,
        name = name ?: login,
        username = login,
        bio = bio,
        repositoryCount = publicRepos,
        followers = followers,
        following = following,
    )
