package zed.rainxch.details.domain.model

import zed.rainxch.core.domain.model.apk.ApkPackageInfo

data class UpdateInstalledAppParams(
    val apkInfo: ApkPackageInfo,
    val assetName: String,
    val assetUrl: String,
    val releaseTag: String,
    val isPendingInstall: Boolean,
)
