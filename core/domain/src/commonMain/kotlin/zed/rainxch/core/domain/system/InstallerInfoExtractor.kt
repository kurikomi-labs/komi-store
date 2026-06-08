package zed.rainxch.core.domain.system

import zed.rainxch.core.domain.model.apk.ApkPackageInfo

interface InstallerInfoExtractor {
    suspend fun extractPackageInfo(filePath: String): ApkPackageInfo?
}
