package zed.rainxch.details.domain.model

import zed.rainxch.core.domain.model.ApkPackageInfo
import zed.rainxch.core.domain.model.GithubRepoSummary

data class SaveInstalledAppParams(
    val repo: GithubRepoSummary,
    val apkInfo: ApkPackageInfo,
    val assetName: String,
    val assetUrl: String,
    val assetSize: Long,
    val releaseTag: String,
    val isPendingInstall: Boolean,
    val isFavourite: Boolean,
    val siblingAssetCount: Int,
    val pickedAssetIndex: Int?,

    val pendingInstallFilePath: String? = null,
)
