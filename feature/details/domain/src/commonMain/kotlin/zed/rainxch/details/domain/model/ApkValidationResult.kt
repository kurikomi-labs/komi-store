package zed.rainxch.details.domain.model

import zed.rainxch.core.domain.model.ApkPackageInfo

sealed interface ApkValidationResult {

    data class Valid(
        val apkInfo: ApkPackageInfo,
    ) : ApkValidationResult

    data object ExtractionFailed : ApkValidationResult

    data class PackageMismatch(
        val apkPackageName: String,
        val installedPackageName: String,
    ) : ApkValidationResult
}
