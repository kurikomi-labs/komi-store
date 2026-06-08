package zed.rainxch.core.presentation.utils

import zed.rainxch.core.domain.model.account.github.GithubUser
import zed.rainxch.core.presentation.model.GithubUserUi

fun GithubUser.toUi(): GithubUserUi {
    return GithubUserUi(
        id = id,
        login = login,
        avatarUrl = avatarUrl,
        htmlUrl = htmlUrl
    )
}
