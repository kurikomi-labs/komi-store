package zed.rainxch.core.domain.system

import zed.rainxch.core.domain.model.account.github.GithubAsset

data class DownloadSpec(

    val packageName: String,

    val repoOwner: String,

    val repoName: String,

    val asset: GithubAsset,

    val displayAppName: String,

    val installPolicy: InstallPolicy,

    val releaseTag: String,
)
